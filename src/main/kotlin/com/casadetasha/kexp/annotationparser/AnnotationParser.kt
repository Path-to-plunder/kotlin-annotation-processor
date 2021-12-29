package com.casadetasha.kexp.annotationparser

import com.casadetasha.kexp.annotationparser.kxt.*
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Name
import javax.tools.Diagnostic
import kotlin.reflect.KClass

object AnnotationParser {

    const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

    private var processingEnv_: ProcessingEnvironment? = null
    var processingEnv: ProcessingEnvironment
        get() {
            if (processingEnv_ == null) throwNotSetupException()
            return processingEnv_!!
        }
        set(processingEnv) {
            processingEnv_ = processingEnv
        }

    private var kaptKotlinGeneratedDir_: String? = null
    val kaptKotlinGeneratedDir: String
        get() {
            if (kaptKotlinGeneratedDir_ == null) throwNotSetupException()
            return kaptKotlinGeneratedDir_!!
        }

    private val messager: Messager by lazy { processingEnv.messager }
    private lateinit var roundEnv: RoundEnvironment

    fun setup(processingEnv: ProcessingEnvironment, roundEnv: RoundEnvironment?) {
        val incomingKotlinGeneratedDir = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
        if (incomingKotlinGeneratedDir == null || roundEnv == null) {
            printThenThrowError(
                "roundEnvironment and kapt generated dir for ProcessingEnvironment must both be non null")
        }

        this.processingEnv = processingEnv
        this.roundEnv = roundEnv
        kaptKotlinGeneratedDir_ = incomingKotlinGeneratedDir
    }

    fun printNote(text: String) {
        messager.printMessage(Diagnostic.Kind.NOTE, text)
    }

    fun printThenThrowError(errorMessage: String): Nothing {
        messager.printMessage(Diagnostic.Kind.ERROR, errorMessage)
        throw IllegalArgumentException(errorMessage)
    }

    @OptIn(KotlinPoetMetadataPreview::class)
    fun getClassesAnnotatedWith(
        annotationClass: KClass<out Annotation>,
        propertyAnnotations: List<KClass<out Annotation>> = listOf()
    ): Set<KotlinContainer.KotlinClass> {
        val elementSet: MutableSet<Element> = HashSet<Element>().apply {
            propertyAnnotations.forEach {
                addAll(roundEnv.getElementsAnnotatedWith(it.java))
            }
        }
        val propertyElementSet: List<Pair<String, Element>> = elementSet.map { it.simpleName.asColumnName() to it }
        val propertyElementClassMap: MutableMap<ClassName, MutableMap<String, Element>> = HashMap()

        propertyElementSet.forEach {
            val parentName: ClassName = it.second.enclosingElement.getClassName()
            if (propertyElementClassMap[parentName] == null) propertyElementClassMap[parentName] = HashMap()
            propertyElementClassMap[parentName]!![it.first] = it.second
        }

        return roundEnv.getElementsAnnotatedWith(annotationClass.java)
            .filter { it.isClass() }
            .map {
                val className = it.getClassName()
                KotlinContainer.KotlinClass(
                    element = it,
                    className = className,
                    classData = className.getClassData(),
                    functionElementMap = it.getChildFunctionElementMap(),
                    annotatedPropertyElementMap = propertyElementClassMap[className] ?: HashMap()
                )
            }.toSet()
    }

    @OptIn(KotlinPoetMetadataPreview::class)
    fun getFileFacadesForTopLevelFunctionsAnnotatedWith(
        annotations: List<KClass<out Annotation>>
    ): Set<KotlinContainer.KotlinFileFacade> {
        return FileFacadeParser(roundEnv).getFacadesForFilesContainingAnnotations(annotations)
    }

    @OptIn(KotlinPoetMetadataPreview::class)
    fun getPropertiesAnnotatedWith(
        annotationClass: KClass<out Annotation>
    ) = roundEnv.getElementsAnnotatedWith(annotationClass.java)
        .toSet()

    private fun throwNotSetupException(): Nothing = throw IllegalStateException(
        "'setup' must be called before using AnnotationParser.")
}

private fun Name.asColumnName(): String {
    return toString()
        .removeSuffix("\$annotations")
        .removePrefix("get")
        .replaceFirstChar { it.lowercase() }
}
