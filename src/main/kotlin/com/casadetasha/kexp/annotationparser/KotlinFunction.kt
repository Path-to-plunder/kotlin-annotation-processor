package com.casadetasha.kexp.annotationparser

import com.casadetasha.kexp.annotationparser.kxt.hasAnnotation
import com.casadetasha.kexp.annotationparser.kxt.toMemberName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.metadata.ImmutableKmFunction
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.ImmutableKmValueParameter
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.metadata.KmClassifier
import javax.lang.model.element.Element
import kotlin.reflect.KClass

@OptIn(KotlinPoetMetadataPreview::class)
sealed class KotlinFunction(
    val packageName: String,
    val element: Element,
    val function: ImmutableKmFunction
) : Comparable<KotlinFunction> {

    abstract val memberName: MemberName
    val simpleName: String = function.name
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

    fun hasAnyAnnotationsIn(vararg annotations: KClass<out Annotation>): Boolean {
        annotations.forEach {
            if (element.hasAnnotation(it.java)) return true
        }
        return false
    }

    class KotlinTopLevelFunction(
        packageName: String,
        methodElement: Element,
        function: ImmutableKmFunction,
    ) : KotlinFunction(
        packageName = packageName,
        element = methodElement,
        function = function
    ) {
        override val memberName: MemberName = MemberName(packageName, simpleName)
    }


    class KotlinMemberFunction(
        packageName: String,
        methodElement: Element,
        function: ImmutableKmFunction,
    ) : KotlinFunction(
        packageName = packageName,
        element = methodElement,
        function = function
    ) {
        override val memberName: MemberName = MemberName(packageName, simpleName)
    }
}