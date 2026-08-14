package it.bbnss.moneta.core.providers

import it.bbnss.moneta.core.model.Currency
import it.bbnss.moneta.core.model.ProviderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/** Confronto a meno di un'inezia, per non dipendere dalle ultime cifre. */
private fun assertNear(expected: String, actual: BigDecimal?, scale: Int = 6) {
    requireNotNull(actual) { "Tasso assente" }
    assertEquals(
        BigDecimal(expected).setScale(scale, RoundingMode.HALF_UP),
        actual.setScale(scale, RoundingMode.HALF_UP),
    )
}

class BankOfCanadaProviderTest {

    private val provider = BankOfCanadaProvider(clientReturning(""), FIXED_CLOCK)

    private fun snapshot() =
        (provider.parse(fixture("boc_recent.json")) as ProviderResult.Success).value

    @Test
    fun `il pivot e il dollaro canadese`() {
        assertEquals(Currency.CAD, snapshot().pivot)
        assertEquals(ProviderId.BANK_OF_CANADA, snapshot().provider)
    }

    /**
     * La banca quota "CAD per 1 unità estera", noi vogliamo l'opposto: 1 CAD
     * deve valere circa 0,727 dollari statunitensi, non 1,375.
     */
    @Test
    fun `inverte la direzione della quotazione`() {
        assertNear("0.727273", snapshot().rates[Currency.USD])
    }

    /**
     * Con `recent=1` ogni serie porta la sua ultima osservazione, quindi
     * arrivano righe con date diverse: per ciascuna valuta va tenuta la più
     * recente, non l'ultima incontrata.
     */
    @Test
    fun `per ogni valuta tiene l osservazione piu recente`() {
        // 2026-08-07 vale 1.3750, il giorno prima 1.3800.
        assertNear("0.727273", snapshot().rates[Currency.USD])
    }

    /** Le serie dismesse restano utilizzabili, ma non spostano la data. */
    @Test
    fun `una serie ferma al 2019 non fa retrodatare lo snapshot`() {
        val snapshot = snapshot()
        assertTrue(snapshot.supports(Currency("VND")))
        assertEquals(LocalDate.of(2026, 8, 7), snapshot.rateDate)
    }

    @Test
    fun `ignora le chiavi che non sono serie di cambio`() {
        assertFalse(snapshot().rates.keys.any { it.code == "D" })
    }
}

class BankRossiiProviderTest {

    private val provider = BankRossiiProvider(clientReturning(""), FIXED_CLOCK)

    private fun snapshot() =
        (provider.parse(fixture("cbr_daily.xml")) as ProviderResult.Success).value

    @Test
    fun `legge la data nel formato russo`() {
        assertEquals(LocalDate.of(2026, 8, 8), snapshot().rateDate)
        assertEquals(Currency.RUB, snapshot().pivot)
    }

    /** I decimali sono separati da virgola: `57,7548`. */
    @Test
    fun `interpreta la virgola decimale`() {
        assertNear("0.017315", snapshot().rates[Currency("AUD")])
    }

    /**
     * Lo yen è quotato per 100 unità. Usando `Value` senza dividere per
     * `Nominal` il tasso risulterebbe sbagliato di due ordini di grandezza:
     * qui si usa `VunitRate`, già normalizzato a una unità.
     */
    @Test
    fun `usa VunitRate per le valute quotate a lotti di cento`() {
        // 1 / 0,541234 = 1,847630 yen per rublo
        assertNear("1.847630", snapshot().rates[Currency.JPY])
    }

    /** Quando `VunitRate` manca si ripiega su `Value / Nominal`. */
    @Test
    fun `ripiega su Value diviso Nominal`() {
        assertNear("0.010811", snapshot().rates[Currency.EUR])
    }

    /**
     * La dichiarazione `encoding="windows-1251"` in testa al documento non deve
     * mandare in errore il parser: i byte sono già stati decodificati a monte.
     */
    @Test
    fun `ignora la dichiarazione di codifica del documento`() {
        assertEquals(3, snapshot().rates.size)
    }
}

class NorgesBankProviderTest {

    private val provider = NorgesBankProvider(clientReturning(""), FIXED_CLOCK)

    private fun snapshot() =
        (provider.parse(fixture("norges_sdmx.json")) as ProviderResult.Success).value

    @Test
    fun `risolve i codici valuta dagli indici posizionali SDMX`() {
        val snapshot = snapshot()
        assertEquals(Currency.NOK, snapshot.pivot)
        assertTrue(snapshot.supports(Currency("DKK")))
        assertTrue(snapshot.supports(Currency("XDR")))
        assertTrue(snapshot.supports(Currency("RUB")))
    }

    /**
     * La corona danese è quotata per 100 unità (UNIT_MULT = 2). Senza applicare
     * il moltiplicatore il tasso sarebbe cento volte più piccolo, e nessun
     * controllo a schermo lo renderebbe evidente.
     */
    @Test
    fun `applica il moltiplicatore alle valute quotate per cento`() {
        // 100 / 146,95 = 0,680504 DKK per 1 NOK
        assertNear("0.680504", snapshot().rates[Currency("DKK")], scale = 6)
    }

    /** Con UNIT_MULT = 0 la quotazione è già per una unità. */
    @Test
    fun `non applica il moltiplicatore dove vale zero`() {
        // 1 / 12,99728 = 0,07694
        assertNear("0.076939", snapshot().rates[Currency("XDR")], scale = 6)
    }

    /**
     * Le serie interrotte hanno un indice temporale diverso: il rublo è fermo
     * al 2022 ma la data dello snapshot deve restare quella più recente.
     */
    @Test
    fun `la data dello snapshot e la piu recente fra le serie`() {
        assertEquals(LocalDate.of(2026, 8, 10), snapshot().rateDate)
    }
}

class InforEuroProviderTest {

    private val provider = InforEuroProvider(clientReturning(""), FIXED_CLOCK)

    private fun snapshot() =
        (provider.parse(fixture("inforeuro.json")) as ProviderResult.Success).value

    @Test
    fun `i valori sono gia unita per euro e non vanno invertiti`() {
        assertNear("1.147600", snapshot().rates[Currency.USD])
        assertNear("186.990000", snapshot().rates[Currency.JPY])
    }

    /**
     * La risposta non porta una data: il tasso vale per il mese in corso,
     * quindi lo snapshot è datato al primo del mese dell'orologio di prova
     * (9 agosto 2026).
     */
    @Test
    fun `data il tasso al primo giorno del mese`() {
        assertEquals(LocalDate.of(2026, 8, 1), snapshot().rateDate)
    }

    /** Pubblica una volta al mese: la UI non deve trattarlo come dato vecchio. */
    @Test
    fun `dichiara cadenza mensile`() {
        assertEquals(UpdateCadence.MONTHLY, provider.capabilities.cadence)
    }
}
