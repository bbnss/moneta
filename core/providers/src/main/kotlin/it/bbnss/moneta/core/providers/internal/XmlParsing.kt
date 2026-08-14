package it.bbnss.moneta.core.providers.internal

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Analizza XML già decodificato in [String].
 *
 * Si parte da un [StringReader] e non da un flusso di byte apposta: la
 * dichiarazione `encoding=` in testa al documento viene così ignorata. Serve per
 * Bank Rossii, che pubblica in windows-1251 — decodificare i byte con la
 * codifica giusta e poi lasciare che il parser creda ancora di leggere
 * windows-1251 produrrebbe caratteri corrotti.
 *
 * `DocumentBuilderFactory` è disponibile sia sulla JVM sia su Android, a
 * differenza di `XmlPullParser` (solo Android) e di StAX (solo JVM).
 */
internal fun parseXml(text: String): Document {
    val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = false
        // I feed sono pubblici e non usano entità: disattivarle chiude
        // qualunque possibilità di XXE su un input inatteso o manomesso.
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        isExpandEntityReferences = false
    }
    return factory.newDocumentBuilder().parse(InputSource(StringReader(text)))
}

/** Scorre gli elementi con un dato nome come sequenza Kotlin. */
internal fun Document.elements(tagName: String): Sequence<Element> {
    val nodes = getElementsByTagName(tagName)
    return (0 until nodes.length).asSequence().mapNotNull { nodes.item(it) as? Element }
}

/** Valore di un attributo, oppure `null` se assente o vuoto. */
internal fun Element.attributeOrNull(name: String): String? =
    getAttribute(name).takeIf { it.isNotEmpty() }

/** Contenuto testuale del primo figlio con questo nome. */
internal fun Element.childText(tagName: String): String? {
    val nodes = getElementsByTagName(tagName)
    if (nodes.length == 0) return null
    return (nodes.item(0) as? Element)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
}
