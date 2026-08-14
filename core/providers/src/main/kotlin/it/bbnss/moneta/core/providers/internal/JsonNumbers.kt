package it.bbnss.moneta.core.providers.internal

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal

/**
 * Legge un numero JSON come [BigDecimal] **senza passare da `Double`**.
 *
 * `kotlinx.serialization` conserva il testo originale del numero in
 * [JsonPrimitive.content]: costruendo il [BigDecimal] da quel testo si ottiene
 * esattamente la cifra pubblicata dalla fonte. Deserializzare in `Double` e poi
 * convertire introdurrebbe già in fase di parsing l'errore binario che tutta
 * l'aritmetica dell'app è costruita per evitare.
 */
internal fun JsonElement.asBigDecimalOrNull(): BigDecimal? {
    val primitive = this as? JsonPrimitive ?: return null
    if (primitive.isString) {
        // Alcune fonti (Bank Rossii, Norges Bank) pubblicano i tassi come stringa.
        return primitive.content.trim().replace(',', '.').toBigDecimalOrNull()
    }
    return primitive.content.toBigDecimalOrNull()
}

private fun String.toBigDecimalOrNull(): BigDecimal? =
    runCatching { BigDecimal(this) }.getOrNull()
