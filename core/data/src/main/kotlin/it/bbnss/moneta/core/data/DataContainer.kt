package it.bbnss.moneta.core.data

import android.content.Context
import androidx.room.Room
import it.bbnss.moneta.core.data.db.MonetaDatabase
import it.bbnss.moneta.core.providers.ProviderRegistry
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import okhttp3.Cache
import java.io.File
import java.io.IOException
import java.time.Clock

/**
 * Contenitore delle dipendenze, scritto a mano.
 *
 * Un framework di dependency injection qui costerebbe un processore di
 * annotazioni in più a ogni build, una libreria in più nell'APK e una variabile
 * in più nella riproducibilità della build per F-Droid, in cambio di nulla: le
 * dipendenze di quest'app sono cinque oggetti creati una volta sola.
 */
class DataContainer private constructor(context: Context) {

    private val appContext = context.applicationContext

    val database: MonetaDatabase by lazy {
        Room.databaseBuilder(appContext, MonetaDatabase::class.java, MonetaDatabase.NAME).build()
    }

    val settings: SettingsStore by lazy { SettingsStore(appContext) }

    val localCurrencyDetector: LocalCurrencyDetector by lazy {
        LocalCurrencyDetector(appContext)
    }

    private val seedLoader: SeedLoader by lazy { SeedLoader(appContext) }

    val httpClient: HttpClient by lazy {
        HttpClient(OkHttp) {
            // Gli errori HTTP vengono classificati dai provider, non lanciati.
            expectSuccess = false

            engine {
                config {
                    // La cache HTTP di OkHttp gestisce da sola `ETag` e
                    // `If-None-Match`: quando i tassi non sono cambiati la
                    // risposta è un 304 senza corpo, e in roaming la differenza
                    // fra 10 kB e zero si sente.
                    cache(Cache(File(appContext.cacheDir, "http"), HTTP_CACHE_BYTES))

                    // Un aggiornamento deve essere dimostrato, non supposto.
                    //
                    // La cache serve a non riscaricare byte identici, non a far
                    // credere di aver parlato con la fonte: se la risposta
                    // arriva solo dal disco, la rete non ha confermato niente e
                    // l'orario di aggiornamento non va toccato. Qui la
                    // differenza si vede, perché `networkResponse` è valorizzata
                    // solo quando uno scambio è avvenuto davvero — anche se si è
                    // risolto in un 304.
                    addInterceptor { chain ->
                        val response = chain.proceed(chain.request())
                        if (response.networkResponse == null) {
                            response.close()
                            throw IOException(
                                "Risposta servita dalla cache locale: la fonte non è stata raggiunta",
                            )
                        }
                        response
                    }
                }
            }

            install(HttpTimeout) {
                // Otto secondi per connettersi: oltre, in viaggio con una rete
                // lenta, conviene passare alla fonte successiva piuttosto che
                // far aspettare davanti a uno schermo vuoto.
                connectTimeoutMillis = 8_000
                socketTimeoutMillis = 8_000
                requestTimeoutMillis = 20_000
            }

            install(UserAgent) {
                agent = "Moneta (+https://github.com/bbnss/moneta)"
            }
        }
    }

    /**
     * Elenco delle fonti selezionabili, con copertura e cadenza.
     *
     * Serve alle impostazioni per descrivere ogni fonte prima che l'utente la
     * scelga: quante valute copre e ogni quanto pubblica sono le due cose che
     * determinano se è adatta al suo viaggio.
     */
    val providerCatalog: List<it.bbnss.moneta.core.providers.RateProvider> by lazy {
        ProviderRegistry(httpClient).all
    }

    val rateRepository: RateRepository by lazy {
        RateRepository(
            dao = database.rateDao(),
            settings = settings,
            seedLoader = seedLoader,
            registryFactory = { customEndpoint ->
                ProviderRegistry(httpClient, customEndpoint, Clock.systemUTC())
            },
        )
    }

    companion object {
        private const val HTTP_CACHE_BYTES = 4L * 1024 * 1024

        @Volatile
        private var instance: DataContainer? = null

        fun get(context: Context): DataContainer =
            instance ?: synchronized(this) {
                instance ?: DataContainer(context).also { instance = it }
            }
    }
}
