package com.casadetasha.kexp.annotationparser

import com.casadetasha.kexp.annotationparser.kxt.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
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
        annotationClass: KClass<out Annotation>
    ): Set<KotlinContainer.KotlinClass> = roundEnv.getElementsAnnotatedWith(annotationClass.java)
        .filter { it.isClass() }
        .map {
            val className = it.getClassName()
            KotlinContainer.KotlinClass(
                element = it,
                className = className,
                classData = className.getClassData(),
                functionElementMap = it.getChildFunctionElementMap()
            )
        }.toSet()

    @OptIn(KotlinPoetMetadataPreview::class)
    fun getFileFacadesForTopLevelFunctionsAnnotatedWith(
        annotations: List<KClass<out Annotation>>
    ): Set<KotlinContainer.KotlinFileFacade> {
        return FileFacadeParser(roundEnv).getFacadesForFilesContainingAnnotations(annotations)
    }

    private fun throwNotSetupException(): Nothing = printThenThrowError(
        "setup must be called before using AnnotationParser.")
}
