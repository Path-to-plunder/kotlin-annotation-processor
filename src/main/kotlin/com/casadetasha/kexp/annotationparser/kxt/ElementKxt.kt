package com.casadetasha.kexp.annotationparser.kxt

import com.casadetasha.kexp.annotationparser.AnnotationParser.processingEnv
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement

@OptIn(KotlinPoetMetadataPreview::class)
internal fun Element.getParentFileKmPackage(): ImmutableKmPackage =
    enclosingElement.getAnnotation(Metadata::class.java)!!
        .toKotlinClassMetadata<KotlinClassMetadata.FileFacade>()
        .toImmutableKmPackage()

@OptIn(KotlinPoetMetadataPreview::class)
internal fun Element.isTopLevelFunction() =
    enclosingElement.getAnnotation(Metadata::class.java)
        ?.readKotlinClassMetadata()
        ?.header
        ?.kind == KotlinClassHeader.FILE_FACADE_KIND

@OptIn(KotlinPoetMetadataPreview::class)
internal fun Element.isClass() =
    getAnnotation(Metadata::class.java)
        ?.readKotlinClassMetadata()
        ?.header
        ?.kind == KotlinClassHeader.CLASS_KIND

@OptIn(KotlinPoetMetadataPreview::class)
internal fun Element.getClassName(): ClassName {
    val typeMetadata = getAnnotation(Metadata::class.java)
    val kmClass = typeMetadata.toImmutableKmClass()
    return ClassInspectorUtil.createClassName(kmClass.name)
}

internal fun Element.asKey(): String {
    val packageName = processingEnv.elementUtils.getPackageOf(this).qualifiedName.toString()
    val containerName = enclosingElement.simpleName.toString()
    return "${packageName}.${containerName}.${simpleName}"
}

internal fun Element.getRequestMethods(): Map<String, Element> = HashMap<String, Element>()
    .apply {
        enclosedElements.forEach {
            if (it.kind == ElementKind.METHOD) {
                this += it.simpleName.toString() to it as ExecutableElement
            }
        }
    }

internal fun Element.hasAnnotation(clazz: Class<out Annotation>): Boolean {
    return getAnnotation(clazz) != null
}

internal val Element.packageName: String
    get() {
        val packageElement = processingEnv.elementUtils.getPackageOf(this)
        return processingEnv.elementUtils.getPackageOf(packageElement).qualifiedName.toString()
    }
