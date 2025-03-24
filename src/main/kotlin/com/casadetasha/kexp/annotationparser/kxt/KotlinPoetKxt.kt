package com.casadetasha.kexp.annotationparser.kxt

import com.casadetasha.kexp.annotationparser.AnnotationParser.processingEnv
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.metadata.classinspectors.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.specs.ClassData
import com.squareup.kotlinpoet.metadata.specs.containerData
import kotlin.metadata.*
import kotlin.metadata.ClassName as StringClassName

internal fun ClassName.getClassData(): ClassData {
    val classInspector = ElementsClassInspector.create(
        true,
        processingEnv.elementUtils,
        processingEnv.typeUtils)
    val containerData = classInspector.containerData(this, null)
    check(containerData is ClassData) { "Unexpected container data type: ${containerData.javaClass}" }
    return containerData
}

internal fun ClassData.primaryConstructor(): KmConstructor? {
    return constructors.keys.firstOrNull { !it.isSecondary }
}

internal fun KmType.toMemberName(): MemberName {
    val name: StringClassName = (classifier as KmClassifier.Class).name
    return MemberName(name.packageName, name.simpleName)
}

internal fun KmValueParameter.asCanonicalName(): String {
    val clazz = type.classifier as KmClassifier.Class
    return clazz.name.replace("/", ".")
}
