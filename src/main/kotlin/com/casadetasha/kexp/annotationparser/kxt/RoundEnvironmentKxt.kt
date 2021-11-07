package com.casadetasha.kexp.annotationparser.kxt

import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.annotation.processing.RoundEnvironment
import kotlin.reflect.KClass

@OptIn(KotlinPoetMetadataPreview::class)
fun RoundEnvironment.getClassesAnnotatedWith(
    annotationClass: KClass<out Annotation>
): Set<KotlinContainer.KotlinClass> = getElementsAnnotatedWith(annotationClass.java)
    .filter { it.isClass() }
    .map {
        val className = it.getClassName()
        KotlinContainer.KotlinClass(
            element = it,
            className = className,
            classData = className.getClassData(),
            functionElementMap = it.getChildFunctionElementMap()
        )
    }.toSet()
