package com.casadetasha.kexp.annotationparser.kxt

import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.AnnotationParser.processingEnv
import com.casadetasha.kexp.annotationparser.createClassName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.metadata.*
import kotlinx.metadata.KmPackage
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement

@OptIn(KotlinPoetMetadataPreview::class)
internal fun Element.getParentFileKmPackage(): KmPackage =
    enclosingElement.getAnnotation(Metadata::class.java)!!
        .toKotlinClassMetadata<KotlinClassMetadata.FileFacade>()
        .toKmPackage()

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
internal fun Element.getNonDefaultParentMemberName(): MemberName {
    var parent: Element = enclosingElement
    try {
        if (parent.simpleName.toString() == "DefaultImpls" && parent.kind == ElementKind.CLASS) {
            parent = parent.enclosingElement
        }
            return parent.memberName
    }  catch(e: Exception) {
        printThenThrowError(
            "Failed to generate member info for parent element ${parent.simpleName}" +
                    " of property $simpleName", e)
    }
}

@OptIn(KotlinPoetMetadataPreview::class)
internal fun Element.getClassName(): ClassName {
    val typeMetadata = getAnnotation(Metadata::class.java)
    val kmClass = typeMetadata.toKmClass()
    return createClassName(kmClass.name)
}

internal fun Element.asKey(): String {
    val packageName = processingEnv.elementUtils.getPackageOf(this).qualifiedName.toString()
    val containerName = enclosingElement.simpleName.toString()
    return "${packageName}.${containerName}.${simpleName}"
}

internal fun Element.getChildFunctionElementMap(): Map<String, Element> = HashMap<String, Element>()
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

internal val Element.memberName: MemberName
    get() {
        return MemberName(packageName, simpleName.toString())
    }
