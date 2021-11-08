package com.casadetasha.kexp.annotationparser.kxt

import kotlinx.metadata.ClassName
import javax.lang.model.element.Element

internal fun MutableList<Element>.mapOnSimpleName() = HashMap<String, Element>().apply {
    this@mapOnSimpleName.forEach { this[it.simpleName.toString()] = it }
}

internal fun <K, V> HashMap<K, MutableList<V>>.getOrCreateList(key: K): MutableList<V> {
    this[key] = this[key] ?: ArrayList()
    return this[key]!!
}

internal val ClassName.packageName: String
    get() {
        val segments = this.split('/').toMutableList()
        segments.removeLast()
        return segments.joinToString(".")
    }

internal val ClassName.simpleName: String
    get() = this.split('/').last()

internal fun String.removeWrappingQuotes(): String = removePrefix("\"").removeSuffix("\"")
