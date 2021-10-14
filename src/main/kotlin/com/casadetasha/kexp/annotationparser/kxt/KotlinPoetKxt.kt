package com.casadetasha.kexp.annotationparser.kxt

import com.casadetasha.kexp.annotationparser.AnnotationParser.processingEnv
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.classinspector.elements.ElementsClassInspector
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.ClassData
import com.squareup.kotlinpoet.metadata.specs.containerData
import kotlinx.metadata.ClassName as StringClassName
import kotlinx.metadata.KmClassifier

@OptIn(KotlinPoetMetadataPreview::class)
internal fun ClassName.getClassData(): ClassData {
    val classInspector = ElementsClassInspector.create(
        processingEnv.elementUtils,
        processingEnv.typeUtils)
    val containerData = classInspector.containerData(this, null)
    check(containerData is ClassData) { "Unexpected container data type: ${containerData.javaClass}" }
    return containerData
}

@OptIn(KotlinPoetMetadataPreview::class)
internal fun ClassData.primaryConstructor(): ImmutableKmConstructor? {
    return constructors.keys.firstOrNull { it.isPrimary }
}

@OptIn(KotlinPoetMetadataPreview::class)
internal fun ImmutableKmType.toMemberName(): MemberName {
    val name: StringClassName = (classifier as KmClassifier.Class).name
    return MemberName(name.packageName, name.simpleName)
}

@OptIn(KotlinPoetMetadataPreview::class)
internal fun ImmutableKmValueParameter.asCanonicalName(): String {
    val clazz = type!!.classifier as KmClassifier.Class
    return clazz.name.replace("/", ".")
}
