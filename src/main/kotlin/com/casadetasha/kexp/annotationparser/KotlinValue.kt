package com.casadetasha.kexp.annotationparser

import com.casadetasha.kexp.annotationparser.kxt.hasAnnotation
import com.casadetasha.kexp.annotationparser.kxt.toMemberName
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.specs.PropertyData
import javax.lang.model.element.Element
import kotlin.metadata.*
import kotlin.reflect.KClass

sealed class KotlinValue(
    val packageName: String,
    val simpleName: String
) : Comparable<KotlinValue> {

    val memberName: MemberName = MemberName(packageName, simpleName)

    override fun compareTo(other: KotlinValue): Int {
        val simpleNameEquality = simpleName.compareTo(other.simpleName)
        if (simpleNameEquality != 0) {
            return simpleNameEquality
        }

        return memberName.toString().compareTo(other.memberName.toString())
    }

    sealed class KotlinFunction(
        val element: Element,
        val function: KmFunction,
        packageName: String
    ) : KotlinValue(
        packageName = packageName,
        simpleName = function.name
    ) {

        val parameterMap: Map<String, KotlinParameter> by lazy { createParameterMap(element, function.valueParameters) }
        val parameters: Collection<KotlinParameter> by lazy { parameterMap.values }

        val receiver: MemberName? by lazy {
            val receiverType = function.receiverParameterType
            if (receiverType == null) null
            else when (receiverType.classifier) {
                is KmClassifier.Class -> receiverType.toMemberName()
                else -> throw IllegalStateException(
                    "Unable to generate $memberName method, extension parameter must be a class."
                )
            }
        }

        val returnType: KmType = function.returnType
        val hasReturnValue: Boolean = returnType.toMemberName() != Unit::class.toMemberName()

        fun hasAnyAnnotationsIn(vararg annotations: KClass<out Annotation>): Boolean {
            annotations.forEach {
                if (element.hasAnnotation(it.java)) return true
            }
            return false
        }

        fun getAnnotation(annotationClass: KClass<out Annotation>): Annotation? {
            return element.getAnnotation(annotationClass.java)
        }

        class KotlinTopLevelFunction(
            packageName: String,
            methodElement: Element,
            function: KmFunction,
        ) : KotlinFunction(
            function = function,
            element = methodElement,
            packageName = packageName
        )

        class KotlinMemberFunction(
            packageName: String,
            methodElement: Element,
            function: KmFunction,
        ) : KotlinFunction(
            function = function,
            element = methodElement,
            packageName = packageName
        )
    }

    class KotlinProperty(
        packageName: String,
        val property: KmProperty,
        val propertyData: PropertyData,
        val annotatedElement: Element?
    ) : KotlinValue(
        packageName = packageName,
        simpleName = property.name
    ) {

        val annotations: Collection<AnnotationSpec> = propertyData.allAnnotations
        val returnType: KmType = property.returnType
        val typeName: TypeName by lazy { returnType.toTypeName() }

        val isNullable: Boolean = property.returnType.isNullable
        val isMutable: Boolean = property.isVar
        val isPublic: Boolean = property.isPublic()
        val isTransient: Boolean by lazy {
            annotations
                .map { annotationSpec -> annotationSpec.typeName }
                .any { it == Transient::class.asTypeName() }
        }

        fun getAnnotationSpec(kClass: KClass<*>): AnnotationSpec? {
            return annotations.firstOrNull { it.typeName == kClass.asTypeName()  }
        }

        private fun KmType.toTypeName(): TypeName {
            val className = when (val valClassifier = classifier) {
                is KmClassifier.Class -> {
                    createClassName(valClassifier.name)
                }
                else -> throw IllegalArgumentException("Only class classifiers are currently supported.")
            }

            val typeArguments = arguments.map { typeProjection ->
                val argType = typeProjection.type
                    ?: throw IllegalArgumentException("Wildcard and star projections not yet supported.")

                argType.toTypeName()
            }

            return if (typeArguments.isNotEmpty()) {
                className.parameterizedBy(*typeArguments.toTypedArray()).copy(nullable = isNullable)
            } else {
                className.copy(nullable = isNullable)
            }
        }
    }
}

private fun KmProperty.isPublic(): Boolean {
    return this.visibility == Visibility.PUBLIC
}

internal fun createClassName(kotlinMetadataName: String): ClassName {

    // Top-level: package/of/class/MyClass
    // Nested A:  package/of/class/MyClass.NestedClass
    val simpleName = kotlinMetadataName.substringAfterLast(
        '/', // Drop the package name, e.g. "package/of/class/"
        '.' // Drop any enclosing classes, e.g. "MyClass."
    )
    val packageName = kotlinMetadataName.substringBeforeLast(
        delimiter = "/",
        missingDelimiterValue = ""
    )
    val simpleNames = kotlinMetadataName.removeSuffix(simpleName)
        .removeSuffix(".") // Trailing "." if any
        .removePrefix(packageName)
        .removePrefix("/")
        .let {
            if (it.isNotEmpty()) {
                it.split(".")
            } else {
                // Don't split, otherwise we end up with an empty string as the first element!
                emptyList()
            }
        }
        .plus(simpleName)

    return ClassName(
        packageName = packageName.replace("/", "."),
        simpleNames = simpleNames
    )
}

private fun String.substringAfterLast(vararg delimiters: Char): String {
    val index = lastIndexOfAny(delimiters)
    return if (index == -1) this else substring(index + 1, length)
}
private fun String.substringBeforeLast(delimiter: String, missingDelimiterValue: String = this): String {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}
