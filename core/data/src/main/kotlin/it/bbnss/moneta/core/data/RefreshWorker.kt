package it.bbnss.moneta.core.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Aggiornamento periodico dei tassi in background.
 *
 * L'aggiornamento non avviene a ogni apertura: aprire il convertitore per
 * cinque secondi al mercato non deve costare una richiesta di rete, e in
 * roaming nemmeno del traffico. La UI legge sempre dal database, questo lavoro
 * lo tiene aggiornato quando le condizioni sono buone.
 */
class RefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = DataContainer.get(applicationContext).rateRepository

        // Al primo avvio dopo l'installazione il database può essere ancora
        // vuoto: i tassi inclusi nell'APK vanno caricati comunque, anche se poi
        // la rete non risponde.
        repository.ensureSeeded()

        return when (repository.refresh(kind = it.bbnss.moneta.core.model.RequestKind.AUTOMATIC)) {
            is RefreshResult.Updated -> Result.success()
            // Non è un guasto: l'utente ha chiesto di non usare la rete.
            RefreshResult.SkippedOffline -> Result.success()
            is RefreshResult.Blocked -> Result.success()
            is RefreshResult.Failed -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "moneta-refresh"

        /**
         * Intervallo che significa "non aggiornare da solo". WorkManager non
         * accetta periodi infiniti, quindi il lavoro viene semplicemente tolto.
         */
        const val MANUAL = -1

        fun schedule(context: Context, intervalHours: Int, wifiOnly: Boolean) {
            if (intervalHours == MANUAL) {
                cancel(context)
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
                )
                .build()

            val request = PeriodicWorkRequestBuilder<RefreshWorker>(
                intervalHours.toLong().coerceAtLeast(1),
                TimeUnit.HOURS,
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                // UPDATE e non REPLACE: cambiare l'intervallo nelle impostazioni
                // non deve azzerare il conteggio e rimandare l'aggiornamento.
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
