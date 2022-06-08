package com.casadetasha.kexp.annotationparser

import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinFunction
import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.annotationparser.kxt.getClassData
import com.casadetasha.kexp.annotationparser.kxt.primaryConstructor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassData
import kotlinx.metadata.KmPackage
import kotlinx.metadata.KmValueParameter
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
        val functionElementMap: Map<String, Element>,
        private val annotatedPropertyElementMap: Map<String, Element>
    ) : KotlinContainer(
        packageName = className.packageName,
        classSimpleName = className.simpleName
    ) {

        val classData: ClassData by lazy { className.getClassData() }

        val primaryConstructorParams: List<KmValueParameter>? by lazy {
            classData
                .primaryConstructor()
                ?.valueParameters
        }

        inline fun <reified T : Annotation> getAnnotation(annotationClass: KClass<out T>): T? {
            return element.getAnnotation(annotationClass.java)
        }

        val kotlinProperties by lazy {
            classData.properties
                .map {
                    val property = it.key
                    val propertyData = it.value
                    KotlinProperty(
                        packageName = packageName,
                        property = property,
                        propertyData = propertyData,
                        annotatedElement = annotatedPropertyElementMap[property.name]
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

    class KotlinFileFacade(
        val element: Element,
        val immutableKmPackage: KmPackage,
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
