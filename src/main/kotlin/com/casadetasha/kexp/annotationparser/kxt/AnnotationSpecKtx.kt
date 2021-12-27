package com.casadetasha.kexp.annotationparser.kxt;

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.TypeName

// TODO: find a way to map the actual annotation to the KotlinProperty to stop using this hack
fun AnnotationSpec.getParameterValueAsString(annotationTypeName: TypeName, key: String): String? {
    return members.filter { typeName == annotationTypeName }
        .map {
            val splitMember = it.toString().split("=")
            Pair(splitMember[0].trim(), splitMember[1].trim())
        }
        .firstOrNull { it.first == key }
        ?.second
        ?.removeWrappingQuotes()
}
