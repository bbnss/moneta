package it.bbnss.moneta.ui.board

import it.bbnss.moneta.core.model.Currency
import org.junit.Assert.assertEquals
import org.junit.Test

class BoardClipboardTest {
    @Test fun copiesBaseAndAllConversionsInDisplayedOrder() {
        val state = BoardUiState(base = Currency.EUR, inputText = "1.000,50", rows = listOf(
            row("GBP", "855,12"), row("USD", "1.124,06"), row("VND", "29.500.000"),
        ))
        assertEquals("1.000,50 EUR\n855,12 GBP\n1.124,06 USD\n29.500.000 VND", state.clipboardText())
    }

    @Test fun preservesUnsupportedCurrenciesWithoutInventingAValue() {
        val state = BoardUiState(base = Currency.USD, inputText = "100", rows = listOf(row("EUR", "")))
        assertEquals("100 USD\n— EUR", state.clipboardText())
    }

    @Test fun clearedAmountAndEmptyFavouritesCopyZeroWithBaseCurrency() {
        assertEquals("0 EUR", BoardUiState().clipboardText())
    }

    private fun row(code: String, formatted: String) = BoardRow(
        currency = Currency(code), amount = null, formatted = formatted, name = code, flag = null,
    )
}
