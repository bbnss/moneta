package it.bbnss.moneta.core.model

/**
 * Identificatore stabile di una fonte dei tassi.
 *
 * [stableId] viene persistito nelle preferenze e nel database: non va **mai**
 * cambiato né riciclato, nemmeno quando un provider viene rimosso dall'app.
 * Riordinare gli enum senza id espliciti è il classico modo per ritrovarsi
 * l'utente con un provider diverso da quello che aveva scelto dopo un
 * aggiornamento.
 *
 * [displayName] sta qui e non nell'implementazione perché la UI deve poter
 * scrivere da dove arriva un tasso salvato mesi fa senza costruire un client
 * HTTP per scoprirlo. Non è tradotto: sono nomi propri.
 */
enum class ProviderId(val stableId: Int, val displayName: String) {
    FRANKFURTER(1, "Frankfurter"),
    FAWAZAHMED0(2, "Currency API (fawazahmed0)"),
    EXCHANGERATE_API(3, "ExchangeRate-API (open)"),
    ECB(4, "European Central Bank"),
    BANK_OF_CANADA(5, "Bank of Canada"),
    NORGES_BANK(6, "Norges Bank"),
    INFOR_EURO(7, "InforEuro (European Commission)"),
    BANK_ROSSII(8, "Bank Rossii"),

    /** Istanza self-hosted configurata dall'utente (API compatibile Frankfurter). */
    CUSTOM(99, "Self-hosted endpoint"),
    ;

    companion object {
        private val byStableId = entries.associateBy { it.stableId }

        fun fromStableId(id: Int): ProviderId? = byStableId[id]
    }
}
