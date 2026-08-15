package it.bbnss.moneta.core.providers.internal

import it.bbnss.moneta.core.providers.FailureReason
import it.bbnss.moneta.core.providers.ProviderResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import java.io.IOException
import java.nio.channels.UnresolvedAddressException
import java.nio.charset.Charset
import kotlin.coroutines.cancellation.CancellationException

/**
 * Esegue una GET e traduce ogni modo di fallire in un [FailureReason].
 *
 * Tutti i provider passano di qui perché la classificazione dell'errore decide
 * se ha senso passare alla fonte successiva e cosa scrivere all'utente: "sei
 * offline" e "questa fonte ha cambiato formato" richiedono reazioni diverse.
 */
internal suspend fun HttpClient.fetchText(url: String): ProviderResult<String> =
    fetchDecoded(url) { it.bodyAsText() }

/**
 * Come [fetchText], ma decodifica il corpo con una codifica imposta da noi.
 *
 * Bank Rossii pubblica in windows-1251 e non sempre lo dichiara in modo che il
 * client lo rispetti: lasciare indovinare significa ritrovarsi il documento
 * corrotto.
 */
internal suspend fun HttpClient.fetchText(
    url: String,
    charset: Charset,
): ProviderResult<String> =
    fetchDecoded(url) { String(it.body<ByteArray>(), charset) }

private suspend inline fun HttpClient.fetchDecoded(
    url: String,
    decode: (HttpResponse) -> String,
): ProviderResult<String> =
    try {
        val response = get(url) {
            // `max-age=0` obbliga a rivalidare sempre con la fonte.
            //
            // Senza, la cache HTTP considera "fresca" la copia locale finché
            // dura il `max-age` dichiarato dal server — che su una CDN sono
            // ore — e la restituisce senza toccare la rete. L'app riceverebbe
            // un 200 identico a quello di una richiesta riuscita e scriverebbe
            // un orario di aggiornamento nuovo su dati vecchi: esattamente la
            // bugia che questa app esiste per non dire.
            //
            // Rivalidare non costa quasi nulla: se i tassi non sono cambiati la
            // fonte risponde 304 senza corpo e la cache fornisce comunque i
            // byte, che era il motivo per cui la cache c'è.
            header(HttpHeaders.CacheControl, "max-age=0")
        }
        if (response.status.isSuccess()) {
            ProviderResult.Success(decode(response))
        } else {
            ProviderResult.Failure(
                reason = FailureReason.HTTP_ERROR,
                message = "HTTP ${response.status.value} da $url",
            )
        }
    } catch (e: CancellationException) {
        // Mai inghiottire una cancellazione: appartiene al chiamante.
        throw e
    } catch (e: HttpRequestTimeoutException) {
        ProviderResult.Failure(FailureReason.TIMEOUT, "Tempo scaduto su $url", e)
    } catch (e: ConnectTimeoutException) {
        ProviderResult.Failure(FailureReason.TIMEOUT, "Connessione scaduta su $url", e)
    } catch (e: SocketTimeoutException) {
        ProviderResult.Failure(FailureReason.TIMEOUT, "Lettura scaduta su $url", e)
    } catch (e: UnresolvedAddressException) {
        ProviderResult.Failure(FailureReason.NETWORK, "Host non risolvibile: $url", e)
    } catch (e: IOException) {
        ProviderResult.Failure(FailureReason.NETWORK, e.message, e)
    }

/** Racchiude un parsing: qualunque eccezione diventa [FailureReason.MALFORMED_RESPONSE]. */
internal inline fun <T> parseResponse(
    provider: String,
    block: () -> T?,
): ProviderResult<T> = try {
    val parsed = block()
    if (parsed == null) {
        ProviderResult.Failure(
            FailureReason.MALFORMED_RESPONSE,
            "$provider: risposta priva dei dati attesi",
        )
    } else {
        ProviderResult.Success(parsed)
    }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    ProviderResult.Failure(
        FailureReason.MALFORMED_RESPONSE,
        "$provider: formato inatteso (${e.message})",
        e,
    )
}
