package com.casadetasha.kexp.annotationparser.kxt

import kotlinx.metadata.ClassName

internal val ClassName.packageName: String
    get() {
        val segments = this.split('/').toMutableList()
        segments.removeLast()
        return segments.joinToString(".")
    }

internal val ClassName.simpleName: String
    get() = this.split('/').last()

internal fun String.removeWrappingQuotes(): String = removePrefix("\"").removeSuffix("\"")
