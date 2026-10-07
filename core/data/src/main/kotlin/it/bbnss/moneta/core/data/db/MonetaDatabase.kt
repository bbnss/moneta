package it.bbnss.moneta.core.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Uno snapshot per fonte.
 *
 * Tenerne uno per ciascuna, invece del solo più recente, permette di cambiare
 * provider anche da offline continuando a vedere i suoi ultimi tassi noti.
 */
@Entity(tableName = "snapshot")
data class SnapshotEntity(
    @PrimaryKey val providerId: Int,
    val pivot: String,
    /** Data dichiarata dalla fonte, ISO-8601. */
    val rateDate: String,
    /** Quando l'abbiamo scaricato noi, epoch in millisecondi. */
    val fetchedAt: Long,
    val endpoint: String? = null,
)

/**
 * Un tasso.
 *
 * [value] è una **stringa**, non un `REAL`. SQLite memorizza i REAL come
 * virgola mobile a 64 bit e reintrodurrebbe proprio l'errore di
 * rappresentazione che l'aritmetica dell'app evita ovunque: è il difetto
 * tuttora aperto nel progetto concorrente più diffuso.
 */
@Entity(
    tableName = "rate",
    primaryKeys = ["providerId", "currency"],
    foreignKeys = [
        ForeignKey(
            entity = SnapshotEntity::class,
            parentColumns = ["providerId"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("providerId")],
)
data class RateEntity(
    val providerId: Int,
    val currency: String,
    val value: String,
    val rateDate: String? = null,
)

/** Punto di una serie storica, in cache per consultare il grafico da offline. */
@Entity(
    tableName = "series_point",
    primaryKeys = ["providerId", "base", "quote", "date"],
)
data class SeriesPointEntity(
    val providerId: Int,
    val base: String,
    val quote: String,
    val date: String,
    val value: String,
)

data class SnapshotWithRates(
    @Embedded val snapshot: SnapshotEntity,
    @Relation(parentColumn = "providerId", entityColumn = "providerId")
    val rates: List<RateEntity>,
)

@Dao
interface RateDao {

    /**
     * Lo snapshot scaricato più di recente, da qualunque fonte.
     *
     * Se la fonte preferita è irraggiungibile da giorni e una di riserva ha
     * risposto stamattina, i tassi buoni sono quelli della riserva: mostrare i
     * dati più freschi che abbiamo, dichiarando da chi vengono, è più utile che
     * restare fedeli a una fonte muta.
     */
    @Transaction
    @Query("SELECT * FROM snapshot ORDER BY fetchedAt DESC LIMIT 1")
    fun observeMostRecent(): Flow<SnapshotWithRates?>

    @Transaction
    @Query("SELECT * FROM snapshot ORDER BY fetchedAt DESC")
    fun observeAll(): Flow<List<SnapshotWithRates>>

    @Query("DELETE FROM snapshot WHERE providerId = :providerId")
    suspend fun deleteSnapshot(providerId: Int)

    @Query("DELETE FROM series_point WHERE providerId = :providerId")
    suspend fun deleteSeries(providerId: Int)

    @Transaction
    @Query("SELECT * FROM snapshot WHERE providerId = :providerId")
    suspend fun get(providerId: Int): SnapshotWithRates?

    @Query("SELECT COUNT(*) FROM snapshot")
    suspend fun snapshotCount(): Int

    @Query("SELECT MAX(fetchedAt) FROM snapshot")
    suspend fun lastFetchedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: SnapshotEntity)

    @Query("DELETE FROM rate WHERE providerId = :providerId")
    suspend fun deleteRates(providerId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<RateEntity>)

    /** Sostituisce in blocco: mai uno stato misto fra vecchi e nuovi tassi. */
    @Transaction
    suspend fun replaceSnapshot(snapshot: SnapshotEntity, rates: List<RateEntity>) {
        upsertSnapshot(snapshot)
        deleteRates(snapshot.providerId)
        insertRates(rates)
    }

    @Query(
        "SELECT * FROM series_point WHERE providerId = :providerId " +
            "AND base = :base AND quote = :quote AND date BETWEEN :from AND :to ORDER BY date",
    )
    suspend fun seriesPoints(
        providerId: Int,
        base: String,
        quote: String,
        from: String,
        to: String,
    ): List<SeriesPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeriesPoints(points: List<SeriesPointEntity>)
}

@Database(
    entities = [SnapshotEntity::class, RateEntity::class, SeriesPointEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class MonetaDatabase : RoomDatabase() {
    abstract fun rateDao(): RateDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rate ADD COLUMN rateDate TEXT")
                db.execSQL("ALTER TABLE snapshot ADD COLUMN endpoint TEXT")
            }
        }
        const val NAME = "moneta.db"
    }
}
