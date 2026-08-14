package it.bbnss.moneta

import android.app.Application
import it.bbnss.moneta.core.data.DataContainer
import it.bbnss.moneta.core.data.RefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MonetaApplication : Application() {

    lateinit var container: DataContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DataContainer.get(this)

        CoroutineScope(Dispatchers.IO).launch {
            // Il seed va caricato prima di qualunque altra cosa: se l'utente ha
            // appena installato l'app e non ha rete, deve comunque poter
            // convertire.
            container.rateRepository.ensureSeeded()

            RefreshWorker.schedule(
                context = this@MonetaApplication,
                intervalHours = container.settings.syncIntervalHours.first(),
                wifiOnly = container.settings.wifiOnly.first(),
            )
        }
    }
}
