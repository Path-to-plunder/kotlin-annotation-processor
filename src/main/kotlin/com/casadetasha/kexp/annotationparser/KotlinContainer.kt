package com.casadetasha.kexp.annotationparser

import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinFunction
import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.annotationparser.kxt.primaryConstructor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.metadata.ImmutableKmPackage
import com.squareup.kotlinpoet.metadata.ImmutableKmValueParameter
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassData
import javax.lang.model.element.Element
import kotlin.reflect.KClass

sealed class KotlinContainer(
    val packageName: String,
    val classSimpleName: String
) : Comparable<KotlinContainer> {

    val memberName = MemberName(packageName, classSimpleName)
    abstract val kotlinFunctions: Set<KotlinFunction>

    fun getFunctionsAnnotatedWith(vararg annotations: KClass<out Annotation>)
            : Set<KotlinFunction> = HashSet<KotlinFunction>().apply {
        kotlinFunctions.forEach { function ->
            if (function.hasAnyAnnotationsIn(*annotations)) {
                this += function
            }
        }
    }

    override fun compareTo(other: KotlinContainer): Int {
        return memberName.toString().compareTo(other.memberName.toString())
    }


    @OptIn(KotlinPoetMetadataPreview::class)
    class KotlinClass(
        val element: Element,
        val className: ClassName,
        val classData: ClassData,
        val functionElementMap: Map<String, Element>
    ) : KotlinContainer(
        packageName = classData.className.packageName,
        classSimpleName = classData.className.simpleName
    ) {

        val primaryConstructorParams: List<ImmutableKmValueParameter>? by lazy {
            classData
                .primaryConstructor()
                ?.valueParameters
        }

        fun getAnnotation(annotationClass: KClass<out Annotation>): Annotation? {
            return element.getAnnotation(annotationClass.java)
        }

        val kotlinProperties by lazy {
            classData.properties
                .map {
                    KotlinProperty(
                        packageName = packageName,
                        property = it.key,
                        propertyData = it.value
                    )
                }
        }

        override val kotlinFunctions: Set<KotlinFunction> by lazy {
            classData.methods
                .filter { functionElementMap.containsKey(it.key.name) }
                .map { entry ->
                    KotlinFunction.KotlinMemberFunction(
                        packageName = packageName,
                        methodElement = functionElementMap[entry.key.name]!!,
                        function = entry.key
                    )
                }
                .toSortedSet()
        }
    }

    @OptIn(KotlinPoetMetadataPreview::class)
    class KotlinFileFacade(
        val element: Element,
        val immutableKmPackage: ImmutableKmPackage,
        packageName: String,
        val fileName: String,
        val functionMap: Map<String, Element>
    ) : KotlinContainer(
        packageName = packageName,
        classSimpleName = fileName
    ) {

        override val kotlinFunctions: Set<KotlinFunction> by lazy {
            immutableKmPackage.functions
                .filter { functionMap.containsKey(it.name) }
                .map {
                    val methodElement = functionMap[it.name]!!
                    KotlinFunction.KotlinTopLevelFunction(
                        packageName = packageName,
                        methodElement = methodElement,
                        function = it
                    )
                }.toSortedSet()
        }
    }
}
