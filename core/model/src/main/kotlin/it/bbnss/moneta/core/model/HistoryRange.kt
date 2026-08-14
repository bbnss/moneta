package it.bbnss.moneta.core.model

import java.time.LocalDate

/**
 * Intervalli offerti dal grafico.
 *
 * [MAX] non parte dal 1948 — Frankfurter ha dati fin lì, ma un grafico di
 * ottant'anni su uno schermo da telefono è illeggibile e scaricarlo costa
 * traffico che in roaming si paga. Venti anni bastano a vedere qualunque
 * andamento di lungo periodo.
 */
enum class HistoryRange(val years: Int = 0, val months: Int = 0) {
    ONE_MONTH(months = 1),
    THREE_MONTHS(months = 3),
    SIX_MONTHS(months = 6),
    ONE_YEAR(years = 1),
    FIVE_YEARS(years = 5),
    MAX(years = 20),
    ;

    fun startDate(today: LocalDate): LocalDate =
        today.minusYears(years.toLong()).minusMonths(months.toLong())
}
