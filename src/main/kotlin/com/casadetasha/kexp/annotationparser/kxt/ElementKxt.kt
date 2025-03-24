package com.casadetasha.kexp.annotationparser.kxt

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.AnnotationParser.processingEnv
import com.casadetasha.kexp.annotationparser.createClassName
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.metadata.*
import kotlin.metadata.KmPackage
import kotlin.metadata.jvm.KotlinClassMetadata
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.PackageElement

internal fun Element.getParentFileKmPackage(): KmPackage {
    val metadata = enclosingElement.getAnnotation(Metadata::class.java)!!
    return when (val kotlinMetadata = KotlinClassMetadata.readLenient(metadata)) {
        is KotlinClassMetadata.FileFacade -> kotlinMetadata.kmPackage
        is KotlinClassMetadata.MultiFileClassPart -> kotlinMetadata.kmPackage
        else -> enclosingElement.getParentFileKmPackage()
    }
}

internal fun Element.isTopLevelFunction(): Boolean {
    if (this.kind != ElementKind.METHOD) {
        return false
    }

    return enclosingElement is PackageElement
}

internal fun Element.isClass() = kind == ElementKind.CLASS

internal fun Element.isInterface() = kind == ElementKind.INTERFACE

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

internal fun Element.getClassName(): ClassName {
    val typeMetadata = getAnnotation(Metadata::class.java)
    val kotlinMetadata = KotlinClassMetadata.readLenient(typeMetadata)
    if (kotlinMetadata is KotlinClassMetadata.Class) {
        val kmClass = kotlinMetadata.kmClass
        return createClassName(kmClass.name)
    } else {
        printThenThrowError("Cannot get class name for $kind")
    }
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
