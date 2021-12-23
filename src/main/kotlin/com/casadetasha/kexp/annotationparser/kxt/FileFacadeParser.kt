package com.casadetasha.kexp.annotationparser.kxt

import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import kotlin.reflect.KClass

@OptIn(KotlinPoetMetadataPreview::class)
internal class FileFacadeParser(private val roundEnvironment: RoundEnvironment) {
    private val functionListMap = HashMap<String, MutableList<Element>>()
    private val functionFileElementMap = HashMap<String, Element>()

    fun getFacadesForFilesContainingAnnotations(annotations: List<KClass<out Annotation>>): Set<KotlinContainer.KotlinFileFacade> {
        setupMapsForAnnotations(annotations)

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

    private fun setupMapsForAnnotations(annotations: List<KClass<out Annotation>>) {
        roundEnvironment.getElementsAnnotatedWithAny(annotations.map { it.java }.toSet())
            .filter { it.isTopLevelFunction() }
            .forEach {
                val key = it.enclosingElement.asKey()
                functionListMap.getOrCreateList(key).add(it)
                functionFileElementMap[key] = functionFileElementMap[key] ?: it.enclosingElement
            }
    }

}