package it.bbnss.moneta.core.model

import java.math.BigDecimal
import java.time.LocalDate

/** Documented banknote denominations; never substitute estimated amounts in a counter. */
object Denominations {
    data class Catalog(val values: List<BigDecimal>, val issuer: String, val source: String,
                       val checkedOn: LocalDate = LocalDate.of(2026, 10, 8))

    private fun catalog(amounts: String, issuer: String, source: String) =
        Catalog(amounts.split(" ").map(::BigDecimal), issuer, source)

    val catalogs: Map<String, Catalog> = mapOf(
        "EUR" to catalog("5 10 20 50 100 200 500", "ECB", "https://www.ecb.europa.eu/euro/banknotes/current/html/index.en.html"),
        "USD" to catalog("1 2 5 10 20 50 100", "U.S. Currency Education Program", "https://www.uscurrency.gov/denominations"),
        "GBP" to catalog("5 10 20 50", "Bank of England", "https://www.bankofengland.co.uk/banknotes/current-banknotes"),
        "CHF" to catalog("10 20 50 100 200 1000", "Swiss National Bank", "https://www.snb.ch/en/the-snb/mandates-goals/cash/series-9"),
        "DKK" to catalog("50 100 200 500", "Danmarks Nationalbank", "https://www.nationalbanken.dk/en/what-we-do/notes-and-coins/danish-banknotes-today"),
        "EGP" to catalog("0.25 0.5 1 5 10 20 50 100 200", "Central Bank of Egypt", "https://www.cbe.org.eg/en/banknote/banknote-issuance/denominations"),
        "JPY" to catalog("1000 2000 5000 10000", "Bank of Japan", "https://www.boj.or.jp/en/note_tfjgs/note/valid/"),
        "INR" to catalog("1 2 5 10 20 50 100 200 500 2000", "Reserve Bank of India", "https://www.rbi.org.in/scripts/FS_FAQs.aspx?Id=136&fn=2753"),
        "IDR" to catalog("1000 2000 5000 10000 20000 50000 100000", "Bank Indonesia", "https://www.bi.go.id/en/publikasi/ruang-media/news-release/Pages/sp_2421922.aspx"),
        "THB" to catalog("20 50 100 500 1000", "Bank of Thailand", "https://www.bot.or.th/en/our-roles/banknotes/History-and-Series-of-Banknote-And-Commemorative/banknotes-series/banknote-series17.html"),
        "MXN" to catalog("20 50 100 200 500 1000", "Banco de Mexico", "https://www.banxico.org.mx/banknotes-and-coins/currently-banknotes-and-coins.html"),
        "BRL" to catalog("2 5 10 20 50 100 200", "Banco Central do Brasil", "https://www.bcb.gov.br/cedulasemoedas/cedulasemitidas"),
        "AUD" to catalog("5 10 20 50 100", "Reserve Bank of Australia", "https://banknotes.rba.gov.au/australias-banknotes/history/"),
        "CAD" to catalog("5 10 20 50 100", "Bank of Canada", "https://www.bankofcanada.ca/banknotes/audience-specific-resources/blind-and-partially-sighted/"),
        "NZD" to catalog("5 10 20 50 100", "Reserve Bank of New Zealand", "https://www.rbnz.govt.nz/money-and-cash/banknotes-and-coins/banknotes-in-circulation-"),
        "SEK" to catalog("20 50 100 200 500 1000", "Sveriges Riksbank", "https://www.riksbank.se/en-gb/payments--cash/notes--coins/notes/valid-banknotes/"),
        "NOK" to catalog("50 100 200 500 1000", "Norges Bank", "https://www.norges-bank.no/en/topics/notes-and-coins/legal-tender-notes-coins/About-the-notes/"),
        "CZK" to catalog("100 200 500 1000 2000 5000", "Czech National Bank", "https://www.cnb.cz/cs/bankovky-a-mince/bankovky/kdo-je-kdo-na-nasich-bankovkach/"),
        "PLN" to catalog("10 20 50 100 200 500", "Narodowy Bank Polski", "https://nbp.pl/wp-content/uploads/2025/12/ROG_2024_EN.pdf"),
        "AED" to catalog("5 10 20 50 100 200 500 1000", "Central Bank of the UAE", "https://centralbank.ae/en/our-operations/currency-and-coins/circulated-currency/"),
        "ZAR" to catalog("10 20 50 100 200", "South African Reserve Bank", "https://resbank.co.za/en/home/what-we-do/banknotes-and-coin/Banknotes"),
        "CNY" to catalog("1 5 10 20 50 100", "People's Bank of China", "https://lanzhou.pbc.gov.cn/lanzhou/117124/2417438/index.html"),
        "SGD" to catalog("2 5 10 50 100 1000 10000", "Monetary Authority of Singapore", "https://www.mas.gov.sg/currency/circulation-currency/circulation-currency-notes"),
        "KRW" to catalog("1000 5000 10000 50000", "Bank of Korea", "https://www.bok.or.kr/eng/main/contents.do?menuNo=400172"),
        "TWD" to catalog("100 200 500 1000 2000", "Central Bank of the Republic of China (Taiwan)", "https://www.cbc.gov.tw/en/dl-3280-78bd2dad93224dcf9458ad4e5fcb6fa1.html"),
        "MYR" to catalog("1 5 10 20 50 100", "Bank Negara Malaysia", "https://www.bnm.gov.my/currency/banknotes"),
        "MAD" to catalog("20 50 100 200", "Bank Al-Maghrib", "https://www.sgg.gov.ma/BO/FR/2873/2023/BO_7254_Fr.pdf"),
        "KWD" to catalog("0.25 0.5 1 5 10 20", "Central Bank of Kuwait", "https://www.cbk.gov.kw/en/banknotes-and-coins/banknotes/sixth-issue"),
        "QAR" to catalog("1 5 10 50 100 200 500", "Qatar Central Bank", "https://www.qcb.gov.qa/en/Pages/HistoryOfBanknotes.aspx"),
        "TRY" to catalog("5 10 20 50 100 200", "Central Bank of the Republic of Türkiye", "https://www.tcmb.gov.tr/wps/wcm/connect/EN/TCMB%2BEN/Main%2BMenu/Banknotes/Banknotes%2Bin%2BCirculation%2Band%2BTheir%2BSecurity%2BFeatures/"),
        "HKD" to catalog("10 20 50 100 500 1000", "Hong Kong Monetary Authority", "https://www.hkma.gov.hk/gb_chi/smart-consumers/frequently-asked-questions/banking/p/2/"),
        "VND" to catalog("200 500 1000 2000 5000 10000 20000 50000 100000 200000 500000", "State Bank of Vietnam · Thời báo Ngân hàng", "https://thoibaonganhang.vn/tien-polymer-va-tien-kim-loai-2003-nay-ky-i-184106.html"),
    )

    fun catalog(currency: Currency): Catalog? = catalogs[currency.code]
    fun areKnown(currency: Currency): Boolean = catalog(currency) != null
    fun of(currency: Currency): List<BigDecimal> = catalog(currency)?.values.orEmpty()

    /** Fixed examples for the indicative table, independent of the comparison currency/rate. */
    fun illustrativeAmounts(currency: Currency): List<BigDecimal> =
        LEGACY_AMOUNTS[currency.code]?.map { BigDecimal(it) }
            ?: listOf(1, 2, 5, 10, 20, 50, 100, 200).map { BigDecimal(it) }

    // Old lists are preserved only as indicative amounts, not verified banknotes.
    private val LEGACY_AMOUNTS: Map<String, List<Int>> = mapOf(
        // Europa
        "EUR" to listOf(5, 10, 20, 50, 100, 200),
        "GBP" to listOf(5, 10, 20, 50),
        "CHF" to listOf(10, 20, 50, 100, 200),
        "NOK" to listOf(50, 100, 200, 500, 1000),
        "SEK" to listOf(20, 50, 100, 200, 500, 1000),
        "DKK" to listOf(50, 100, 200, 500, 1000),
        "ISK" to listOf(500, 1000, 2000, 5000, 10000),
        "PLN" to listOf(10, 20, 50, 100, 200, 500),
        "CZK" to listOf(100, 200, 500, 1000, 2000, 5000),
        "HUF" to listOf(500, 1000, 2000, 5000, 10000, 20000),
        "RON" to listOf(1, 5, 10, 50, 100, 200, 500),
        "BGN" to listOf(5, 10, 20, 50, 100),
        "RSD" to listOf(50, 100, 200, 500, 1000, 2000, 5000),
        "UAH" to listOf(20, 50, 100, 200, 500, 1000),
        "RUB" to listOf(10, 50, 100, 200, 500, 1000, 2000, 5000),
        "TRY" to listOf(5, 10, 20, 50, 100, 200),
        "GEL" to listOf(5, 10, 20, 50, 100),
        "AMD" to listOf(1000, 2000, 5000, 10000, 20000, 50000),
        "MDL" to listOf(10, 20, 50, 100, 200, 500, 1000),

        // Americhe
        "USD" to listOf(1, 5, 10, 20, 50, 100),
        "CAD" to listOf(5, 10, 20, 50, 100),
        "MXN" to listOf(20, 50, 100, 200, 500, 1000),
        "BRL" to listOf(2, 5, 10, 20, 50, 100, 200),
        "ARS" to listOf(100, 200, 500, 1000, 2000, 10000, 20000),
        "CLP" to listOf(1000, 2000, 5000, 10000, 20000),
        "COP" to listOf(2000, 5000, 10000, 20000, 50000, 100000),
        "PEN" to listOf(10, 20, 50, 100, 200),
        "UYU" to listOf(20, 50, 100, 200, 500, 1000, 2000),
        "BOB" to listOf(10, 20, 50, 100, 200),
        "CRC" to listOf(1000, 2000, 5000, 10000, 20000, 50000),
        "DOP" to listOf(50, 100, 200, 500, 1000, 2000),
        "CUP" to listOf(5, 10, 20, 50, 100, 200, 500, 1000),
        "GTQ" to listOf(1, 5, 10, 20, 50, 100, 200),

        // Asia
        "JPY" to listOf(1000, 2000, 5000, 10000),
        "CNY" to listOf(1, 5, 10, 20, 50, 100),
        "KRW" to listOf(1000, 5000, 10000, 50000),
        "TWD" to listOf(100, 200, 500, 1000, 2000),
        "HKD" to listOf(20, 50, 100, 500, 1000),
        "SGD" to listOf(2, 5, 10, 50, 100),
        "MYR" to listOf(1, 5, 10, 20, 50, 100),
        "THB" to listOf(20, 50, 100, 500, 1000),
        "VND" to listOf(1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000),
        "KHR" to listOf(500, 1000, 2000, 5000, 10000, 20000, 50000, 100000),
        "LAK" to listOf(1000, 2000, 5000, 10000, 20000, 50000, 100000),
        "MMK" to listOf(50, 100, 200, 500, 1000, 5000, 10000),
        "IDR" to listOf(1000, 2000, 5000, 10000, 20000, 50000, 100000),
        "PHP" to listOf(20, 50, 100, 200, 500, 1000),
        "INR" to listOf(10, 20, 50, 100, 200, 500),
        "PKR" to listOf(10, 20, 50, 100, 500, 1000, 5000),
        "BDT" to listOf(5, 10, 20, 50, 100, 200, 500, 1000),
        "LKR" to listOf(20, 50, 100, 500, 1000, 5000),
        "NPR" to listOf(5, 10, 20, 50, 100, 500, 1000),
        "MNT" to listOf(100, 500, 1000, 5000, 10000, 20000),
        "KZT" to listOf(200, 500, 1000, 2000, 5000, 10000, 20000),
        "UZS" to listOf(1000, 2000, 5000, 10000, 20000, 50000, 100000),

        // Medio Oriente e Africa
        "AED" to listOf(5, 10, 20, 50, 100, 200, 500, 1000),
        "SAR" to listOf(5, 10, 50, 100, 500),
        "QAR" to listOf(1, 5, 10, 50, 100, 500),
        "ILS" to listOf(20, 50, 100, 200),
        "JOD" to listOf(1, 5, 10, 20, 50),
        "LBP" to listOf(1000, 5000, 10000, 20000, 50000, 100000),
        "IRR" to listOf(10000, 20000, 50000, 100000, 500000, 1000000),
        "EGP" to listOf(5, 10, 20, 50, 100, 200),
        "MAD" to listOf(20, 50, 100, 200),
        "TND" to listOf(5, 10, 20, 50),
        "ZAR" to listOf(10, 20, 50, 100, 200),
        "KES" to listOf(50, 100, 200, 500, 1000),
        "TZS" to listOf(500, 1000, 2000, 5000, 10000),
        "UGX" to listOf(1000, 2000, 5000, 10000, 20000, 50000),
        "NGN" to listOf(5, 10, 20, 50, 100, 200, 500, 1000),
        "GHS" to listOf(1, 2, 5, 10, 20, 50, 100, 200),
        "ETB" to listOf(10, 50, 100, 200),

        // Oceania
        "AUD" to listOf(5, 10, 20, 50, 100),
        "NZD" to listOf(5, 10, 20, 50, 100),
        "FJD" to listOf(5, 10, 20, 50, 100),
    )

}
