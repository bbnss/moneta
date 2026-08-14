package it.bbnss.moneta.core.providers

/**
 * Motivo per cui una fonte non ha risposto.
 *
 * La distinzione non è cosmetica: guida sia il messaggio mostrato all'utente
 * sia la decisione se passare alla fonte successiva. Un [UNSUPPORTED] non è un
 * guasto e non deve far scattare il failover, mentre un [MALFORMED_RESPONSE] è
 * il segnale che l'API ha cambiato forma e che serve un aggiornamento del
 * parser.
 */
enum class FailureReason {
    /** Nessuna rete, DNS fallito, host irraggiungibile. */
    NETWORK,

    /** La fonte non ha risposto in tempo utile. */
    TIMEOUT,

    /** Risposta HTTP di errore. */
    HTTP_ERROR,

    /** Risposta ricevuta ma non interpretabile: l'API è probabilmente cambiata. */
    MALFORMED_RESPONSE,

    /** La fonte non offre questa funzione (es. serie storiche) o questa valuta. */
    UNSUPPORTED,
}

sealed interface ProviderResult<out T> {

    data class Success<T>(val value: T) : ProviderResult<T>

    data class Failure(
        val reason: FailureReason,
        val message: String? = null,
        val cause: Throwable? = null,
    ) : ProviderResult<Nothing> {

        /**
         * Se abbia senso provare la fonte successiva.
         *
         * [FailureReason.UNSUPPORTED] non è un guasto: la fonte ha risposto ed
         * è semplicemente il posto sbagliato dove chiedere. Ripiegare su
         * un'altra darebbe all'utente un dato che non ha chiesto.
         */
        val canFailover: Boolean get() = reason != FailureReason.UNSUPPORTED
    }

    val isSuccess: Boolean get() = this is Success

    fun valueOrNull(): T? = (this as? Success)?.value
}

inline fun <T, R> ProviderResult<T>.map(transform: (T) -> R): ProviderResult<R> = when (this) {
    is ProviderResult.Success -> ProviderResult.Success(transform(value))
    is ProviderResult.Failure -> this
}
