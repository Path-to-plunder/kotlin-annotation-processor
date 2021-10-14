package com.casadetasha.kexp.annotationparser

import javax.annotation.processing.ProcessingEnvironment

object AnnotationParser {
    private var processingEnv_: ProcessingEnvironment? = null
    internal var processingEnv: ProcessingEnvironment
        get() {
            if (processingEnv_ == null) throw IllegalStateException("AnnotationParser.setup(ProcessingEnvironment) must be called before using annotation parser extensions.")
            return processingEnv_!!
        }
        set(processingEnv) {
            processingEnv_ = processingEnv
        }

    fun setup(processingEnv: ProcessingEnvironment) {
        this.processingEnv = processingEnv
    }
}