package com.casadetasha.kexp.annotationparser

import com.casadetasha.kexp.annotationparser.kxt.hasAnnotation
import com.casadetasha.kexp.annotationparser.kxt.toMemberName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.PropertyData
import kotlinx.metadata.KmClassifier
import javax.lang.model.element.Element
import kotlin.reflect.KClass

@OptIn(KotlinPoetMetadataPreview::class)
sealed class KotlinValue(
    val element: Element,
    val packageName: String,
    val simpleName: String
) {

    val memberName: MemberName = MemberName(packageName, simpleName)

    fun hasAnyAnnotationsIn(vararg annotations: KClass<out Annotation>): Boolean {
        annotations.forEach {
            if (element.hasAnnotation(it.java)) return true
        }
        return false
    }

    fun getAnnotation(annotationClass: KClass<out Annotation>): Annotation? {
        return element.getAnnotation(annotationClass.java)
    }

    sealed class KotlinFunction(
        element: Element,
        packageName: String,
        val function: ImmutableKmFunction
    ) : KotlinValue(
        element = element,
        packageName = packageName,
        simpleName = function.name
    ), Comparable<KotlinFunction> {

        val parameters: List<ImmutableKmValueParameter> = function.valueParameters
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

        val returnType: ImmutableKmType = function.returnType
        val hasReturnValue: Boolean = returnType.toMemberName() != Unit::class.toMemberName()

        override fun compareTo(other: KotlinFunction): Int {
            return this.memberName.toString().compareTo(other.memberName.toString())
        }

        class KotlinTopLevelFunction(
            packageName: String,
            methodElement: Element,
            function: ImmutableKmFunction,
        ) : KotlinFunction(
            function = function,
            element = methodElement,
            packageName = packageName
        )

        class KotlinMemberFunction(
            packageName: String,
            methodElement: Element,
            function: ImmutableKmFunction,
        ) : KotlinFunction(
            function = function,
            element = methodElement,
            packageName = packageName
        )
    }

    class KotlinProperty(
        propertyElement: Element,
        packageName: String,
        val property: ImmutableKmProperty,
        val propertyData: PropertyData
    ) : KotlinValue(
        element = propertyElement,
        packageName = packageName,
        simpleName = property.name
    )
}

