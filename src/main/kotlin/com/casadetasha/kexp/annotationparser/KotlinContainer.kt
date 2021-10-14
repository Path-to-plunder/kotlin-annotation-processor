package com.casadetasha.kexp.annotationparser

import com.casadetasha.kexp.sproute.processor.MemberNames.convertToMemberNames
import com.casadetasha.kexp.sproute.processor.ktx.primaryConstructor
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.metadata.ImmutableKmPackage
import com.squareup.kotlinpoet.metadata.ImmutableKmValueParameter
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import com.squareup.kotlinpoet.metadata.specs.ClassData
import kotlinx.metadata.KmClassifier
import javax.lang.model.element.Element
import kotlin.reflect.KClass

sealed class KotlinContainer(
    val element: Element,
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
        element: Element,
        val className: ClassName,
        val classData: ClassData,
        val functionMap: Map<String, Element>
    ) : KotlinContainer(
        element = element,
        packageName = classData.className.packageName,
        classSimpleName = classData.className.simpleName
    ) {

        val primaryConstructorParams: List<MemberName>? by lazy {
            classData.primaryConstructor()
                ?.valueParameters
                ?.convertToMemberNames()
        }

        override val kotlinFunctions: Set<KotlinFunction> by lazy {
            classData.methods
                .filter { functionMap.containsKey(it.key.name) }
                .map { entry ->
                    KotlinFunction.KotlinMemberFunction(
                        packageName = packageName,
                        methodElement = functionMap[entry.key.name]!!,
                        function = entry.key
                    )
                }
                .toSortedSet()
        }
    }

    @OptIn(KotlinPoetMetadataPreview::class)
    class KotlinFileFacade(
        element: Element,
        val immutableKmPackage: ImmutableKmPackage,
        packageName: String,
        val fileName: String,
        val functionMap: Map<String, Element>
    ) : KotlinContainer(
        element = element,
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

@OptIn(KotlinPoetMetadataPreview::class)
fun List<ImmutableKmValueParameter>.convertToMemberNames(): List<MemberName> {
    return map { it.asCanonicalName() }
        .map { memberMap[it]!! }
}

@OptIn(KotlinPoetMetadataPreview::class)
internal fun ImmutableKmValueParameter.asCanonicalName(): String {
    val clazz = type!!.classifier as KmClassifier.Class
    return clazz.name.replace("/", ".")
}
