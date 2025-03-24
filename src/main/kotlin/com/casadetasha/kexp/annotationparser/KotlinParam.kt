package com.casadetasha.kexp.annotationparser

import kotlin.metadata.KmValueParameter
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement

class KotlinParameter(val kmParameter: KmValueParameter, val element: VariableElement)

internal fun createParameterMap(parentElement: Element, valueParameters: List<KmValueParameter>): Map<String, KotlinParameter> {
    val elementMap = (parentElement as ExecutableElement).parameters
        .groupBy { it.simpleName.toString() }
        .mapValues { it.value.first() }

    return valueParameters.groupBy { it.name }
        .mapValues { it.value.first() }
        .mapValues { (key, value) -> KotlinParameter(value, elementMap[key]!!)  }
}
