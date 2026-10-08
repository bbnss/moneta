package it.bbnss.moneta.ui.board

import it.bbnss.moneta.core.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class BoardClipboardTest {
    private val labels = BoardClipboardLabels("Tassi di cambio · Moneta", "Tassi del", "Verificati", "Sconosciuto")
    private val zone = ZoneId.of("Europe/Rome")
    private val footer = "\n\nMoneta\nhttps://github.com/bbnss/moneta"
    private fun BoardUiState.export() = clipboardText(labels, Locale.ITALY, zone)
    private val unknownHeader = "Tassi di cambio · Moneta\nTassi del: Sconosciuto\nVerificati: Sconosciuto\n\n"
    @Test fun copiesBaseAndAllConversionsInDisplayedOrder() {
        val state = BoardUiState(base = Currency.EUR, inputText = "1.000,50", rows = listOf(
            row("GBP", "855,12"), row("USD", "1.124,06"), row("VND", "29.500.000"),
        ))
        assertEquals(unknownHeader + "1.000,50 EUR\n855,12 GBP\n1.124,06 USD\n29.500.000 VND" + footer, state.export())
    }

    @Test fun preservesUnsupportedCurrenciesWithoutInventingAValue() {
        val state = BoardUiState(base = Currency.USD, inputText = "100", rows = listOf(row("EUR", "")))
        assertEquals(unknownHeader + "100 USD\n— EUR" + footer, state.export())
    }

    @Test fun clearedAmountAndEmptyFavouritesCopyZeroWithBaseCurrency() {
        assertEquals(unknownHeader + "0 EUR" + footer, BoardUiState().export())
    }

    @Test fun distinguishesQuoteDateFromLaterVerificationWithLocalTime() {
        val state = BoardUiState(inputText = "100", rateDate = LocalDate.of(2026, 10, 5),
            updatedAt = Instant.parse("2026-10-08T13:04:00Z"))
        assertEquals("Tassi di cambio · Moneta\nTassi del: 5 ott 2026\nVerificati: 08/10/26, 15:04\n\n100 EUR" + footer, state.export())
    }

    private fun row(code: String, formatted: String) = BoardRow(
        currency = Currency(code), amount = null, formatted = formatted, name = code, flag = null,
    )
}
