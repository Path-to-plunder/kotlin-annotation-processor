package com.casadetasha.kexp.annotationparser.kxt

import javax.lang.model.element.Element

internal fun MutableList<Element>.mapOnSimpleName() = HashMap<String, Element>().apply {
    this@mapOnSimpleName.forEach { this[it.simpleName.toString()] = it }
}

internal fun <K, V> HashMap<K, MutableList<V>>.getOrCreateList(key: K): MutableList<V> {
    this[key] = this[key] ?: ArrayList()
    return this[key]!!
}
