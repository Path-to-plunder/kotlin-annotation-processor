package com.casadetasha.kexp.annotationparser

import com.casadetasha.kexp.annotationparser.kxt.hasAnnotation
import com.casadetasha.kexp.annotationparser.kxt.toMemberName
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.metadata.*
import com.squareup.kotlinpoet.metadata.specs.PropertyData
import com.squareup.kotlinpoet.metadata.specs.TypeNameAliasTag
import com.squareup.kotlinpoet.metadata.specs.internal.ClassInspectorUtil
import kotlinx.metadata.KmClassifier
import javax.lang.model.element.Element
import kotlin.reflect.KClass

@OptIn(KotlinPoetMetadataPreview::class)
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
        val function: ImmutableKmFunction,
        packageName: String
    ) : KotlinValue(
        packageName = packageName,
        simpleName = function.name
    ) {

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
        packageName: String,
        val property: ImmutableKmProperty,
        val propertyData: PropertyData,
        val annotatedElement: Element?
    ) : KotlinValue(
        packageName = packageName,
        simpleName = property.name
    ) {

        val annotations: Collection<AnnotationSpec> = propertyData.allAnnotations
        val returnType: ImmutableKmType = property.returnType
        val typeName: TypeName by lazy { returnType.toTypeName() }

        val isNullable = property.returnType.isNullable
        val isPublic = property.isPublic
        val isDeclaration = property.isDeclaration
        val isSynthesized =  property.isSynthesized
        val isTransient: Boolean by lazy {
            annotations
                .map { annotationSpec -> annotationSpec.typeName }
                .any { it == Transient::class.asTypeName() }
        }

        fun getAnnotationSpec(kClass: KClass<*>): AnnotationSpec? {
            return annotations.firstOrNull { it.typeName == kClass.asTypeName()  }
        }

        @KotlinPoetMetadataPreview
        private fun ImmutableKmType.toTypeName(): TypeName {
            val type: TypeName = when (val valClassifier = classifier) {
                is KmClassifier.Class -> {
                    ClassInspectorUtil.createClassName(valClassifier.name)
                }
                else -> throw IllegalArgumentException("Only class classifiers are currently supported.")
            }
            val finalType = type.copy(nullable = isNullable)

            return returnType.abbreviatedType?.let {
                // This is actually an alias! The "abbreviated type" is the alias and how it's actually
                // represented in source. So instead - we'll return the abbreviated type but store the "real"
                // type in tags for reference.
                val abbreviatedTypeName = it.toTypeName()
                abbreviatedTypeName.copy(
                    tags = mapOf(TypeNameAliasTag::class to TypeNameAliasTag(finalType))
                )
            } ?: finalType
        }
    }
}

