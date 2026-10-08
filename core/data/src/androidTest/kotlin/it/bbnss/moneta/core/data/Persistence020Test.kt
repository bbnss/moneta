package it.bbnss.moneta.core.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import it.bbnss.moneta.core.data.db.MonetaDatabase
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.FeeMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class Persistence020Test {
    @Test fun migrationPreservesCacheHistoryAndUnknownQuoteDates() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val name = "migration-020-${System.nanoTime()}.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        val schema = JSONObject(instrumentation.context.assets.open("it.bbnss.moneta.core.data.db.MonetaDatabase/1.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            old.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            old.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42,?)", arrayOf(schema.getString("identityHash")))
            old.execSQL("INSERT INTO snapshot(providerId,pivot,rateDate,fetchedAt) VALUES(1,'EUR','2026-08-09',123456789)")
            old.execSQL("INSERT INTO rate(providerId,currency,value) VALUES(1,'USD','1.234567890123456789')")
            old.execSQL("INSERT INTO series_point(providerId,base,quote,date,value) VALUES(1,'EUR','USD','2026-08-01','1.1234567890123456789')")
            old.version = 1
        }
        val database = Room.databaseBuilder(context, MonetaDatabase::class.java, name)
            .addMigrations(MonetaDatabase.MIGRATION_1_2).build()
        try {
            val cached = database.rateDao().get(1)!!
            assertEquals(123456789L, cached.snapshot.fetchedAt)
            assertEquals("1.234567890123456789", cached.rates.single().value)
            assertNull(cached.rates.single().rateDate)
            assertEquals("1.1234567890123456789", database.rateDao().seriesPoints(1, "EUR", "USD", "2026-08-01", "2026-08-31").single().value)
            assertEquals(2, database.openHelper.readableDatabase.version)
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun settingsSurviveClosingAndReopeningRealDataStore() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.filesDir, "restore-${System.nanoTime()}.preferences_pb")
        suspend fun write() {
            val job = SupervisorJob()
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job), produceFile = { file })
            val settings = SettingsStore(store)
            assertEquals(SettingsStore.Calculation(), settings.calculation.first())
            assertEquals(FeeMode.CASH, settings.feeMode.first())
            settings.setCalculation("(123+4)/5", "TO", false)
            settings.setBoardCalculation("", false)
            settings.setFavourites(listOf(Currency.USD, Currency.EUR))
            settings.setFeeMode(FeeMode.CARD)
            settings.setPair(Currency("VND"), Currency.EUR); settings.ensureCashPair()
            settings.adjustCashCount(Currency("VND"), BigDecimal("100000"), 3)
            job.cancelAndJoin()
        }
        write()
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job), produceFile = { file })
        val restored = SettingsStore(store)
        try {
            assertEquals(SettingsStore.Calculation("(123+4)/5", "TO", false), restored.calculation.first())
            assertEquals("", restored.boardCalculation.first().input)
            assertFalse(restored.boardCalculation.first().initial)
            assertEquals(listOf(Currency.USD, Currency.EUR), restored.favourites.first())
            assertEquals(FeeMode.CARD, restored.feeMode.first())
            assertEquals(3, restored.cashCounts(Currency("VND")).first()[BigDecimal("100000")])
            restored.setPair(Currency.USD, Currency.GBP)
            assertEquals(Currency("VND") to Currency.EUR, restored.cashPair.first())
        } finally { job.cancelAndJoin(); file.delete() }
    }

    @Test fun offlineSeedContainsIndividualQuotationDates() {
        val seed = SeedLoader(InstrumentationRegistry.getInstrumentation().targetContext).load()!!
        assertTrue(seed.rates.size >= 50)
        assertEquals(seed.rates.keys, seed.rateDates.keys)
        assertNotNull(seed.dateFor(Currency.EUR, Currency.USD))
    }

    @Test fun cashCountsKeepCurrencyAndMergeLegacyFractionalKeys() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.filesDir, "cash-022-${System.nanoTime()}.preferences_pb")
        val job = SupervisorJob()
        val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job), produceFile = { file })
        val settings = SettingsStore(store)
        try {
            val egp = Currency("EGP")
            settings.setCashPair(egp, Currency.EUR)
            settings.adjustCashCount(egp, BigDecimal("100"), 3)
            settings.setCashPair(egp, Currency.USD)
            assertEquals(3, settings.cashCounts(egp).first()[BigDecimal("100")])
            settings.ensureCashPair()
            assertEquals(egp to Currency.USD, settings.cashPair.first())
            settings.setCashPair(Currency.EUR, Currency.USD)
            assertTrue(settings.cashCounts(Currency.EUR).first().isEmpty())
            assertEquals(3, settings.cashCounts(egp).first()[BigDecimal("100")])

            store.edit { it[stringPreferencesKey("cash_counts_KWD")] = "0.50:2;0.5:1" }
            settings.adjustCashCount(Currency("KWD"), BigDecimal("0.5"), -1)
            val counts = settings.cashCounts(Currency("KWD")).first()
            assertEquals(mapOf(BigDecimal("0.5") to 2), counts)
            assertEquals(0, BigDecimal.ONE.compareTo(it.bbnss.moneta.core.model.CashCounter.total(counts)))
        } finally { job.cancelAndJoin(); file.delete() }
    }
}
