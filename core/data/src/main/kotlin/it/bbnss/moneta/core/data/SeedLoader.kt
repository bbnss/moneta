package it.bbnss.moneta.core.data

import android.content.Context
import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.ProviderId
import it.bbnss.moneta.core.model.RateSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Carica i tassi di partenza inclusi nell'APK.
 *
 * Sono il motivo per cui l'app converte correttamente già al primo avvio senza
 * rete. Il file è generato da `scripts/refresh_seed.sh` e **committato**: la
 * build non chiama mai la rete, altrimenti non sarebbe riproducibile e F-Droid
 * non potrebbe verificarla.
 *
 * Il dato conserva la propria data reale di generazione: caricandolo, l'app
 * dichiara subito che i tassi sono di quel giorno. Spacciarlo per aggiornato
 * sarebbe esattamente la disonestà che questa app esiste per evitare.
 */
class SeedLoader(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): RateSnapshot? = runCatching {
        val body = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val root = json.parseToJsonElement(body) as? JsonObject ?: return null

        val provider = (root["providerId"] as? JsonPrimitive)?.content?.toIntOrNull()
            ?.let { ProviderId.fromStableId(it) }
            ?: ProviderId.FRANKFURTER
        val pivot = Currency.parse((root["pivot"] as? JsonPrimitive)?.content) ?: return null
        val rateDate = (root["rateDate"] as? JsonPrimitive)?.content
            ?.let { LocalDate.parse(it) } ?: return null
        val generatedAt = (root["generatedAt"] as? JsonPrimitive)?.content
            ?.let { Instant.parse(it) }
            ?: rateDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()

        val table = root["rates"] as? JsonObject ?: return null
        val rates = buildMap {
            for ((code, element) in table) {
                val currency = Currency.parse(code) ?: continue
                val raw = (element as? JsonPrimitive)?.content ?: continue
                val value = runCatching { BigDecimal(raw) }.getOrNull() ?: continue
                put(currency, value)
            }
        }

        if (rates.isEmpty()) return null

        RateSnapshot(
            provider = provider,
            pivot = pivot,
            rates = rates,
            rateDate = rateDate,
            rateDates = (root["rateDates"] as? JsonObject)?.mapNotNull { (code, element) ->
                val currency = Currency.parse(code) ?: return@mapNotNull null
                val date = (element as? JsonPrimitive)?.content?.let { LocalDate.parse(it) } ?: return@mapNotNull null
                currency to date
            }?.toMap().orEmpty(),
            // Non "adesso": l'età dichiarata è quella vera del file.
            fetchedAt = generatedAt,
        )
    }.getOrNull()

    private companion object {
        const val ASSET_NAME = "seed_rates.json"
    }
}
