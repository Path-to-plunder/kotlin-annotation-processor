package com.casadetasha.kexp.annotationparser.kxt

import com.casadetasha.kexp.annotationparser.AnnotationParser.processingEnv
import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.squareup.kotlinpoet.metadata.*
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import kotlin.reflect.KClass

@OptIn(KotlinPoetMetadataPreview::class)
fun RoundEnvironment.getFileFacadesForTopLevelFunctionsAnnotatedWith(
    annotations: List<KClass<out Annotation>>
): Set<KotlinContainer.KotlinFileFacade> {
    val functionListMap = HashMap<String, MutableList<Element>>()
    val functionFileElementMap = HashMap<String, Element>()

    getElementsAnnotatedWithAny(annotations.map { it.java }.toSet())
        .filter { it.isTopLevelFunction() }
        .forEach {
            val key = it.enclosingElement.asKey()
            functionListMap.getOrCreateList(key).add(it)
            functionFileElementMap[key] = functionFileElementMap[key] ?: it.enclosingElement
        }

    return functionListMap.map {
        val fileElement = functionFileElementMap[it.key]!!
        KotlinContainer.KotlinFileFacade(
            element = fileElement,
            immutableKmPackage = it.value.first().getParentFileKmPackage(),
            packageName = fileElement.packageName,
            fileName = fileElement.simpleName?.toString() ?: "",
            functionMap = it.value.mapOnSimpleName()
        )
    }.toSet()
}

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
            functionMap = it.getRequestMethods()
        )
    }.toSet()

@OptIn(KotlinPoetMetadataPreview::class)
fun RoundEnvironment.getContainersAnnotatedWith(
    annotationClass: KClass<out Annotation>
): Set<KotlinContainer.KotlinClass> = getElementsAnnotatedWith(annotationClass.java)
    .filter { it.isClass() }
    .map {
        val className = it.getClassName()
        KotlinContainer.KotlinClass(
            element = it,
            className = className,
            classData = className.getClassData(),
            functionMap = it.getRequestMethods()
        )
    }.toSet()