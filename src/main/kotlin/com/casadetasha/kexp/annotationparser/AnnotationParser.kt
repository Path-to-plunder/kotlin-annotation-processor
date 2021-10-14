package com.casadetasha.kexp.annotationparser

import javax.annotation.processing.ProcessingEnvironment

object AnnotationParser {
    private var _processingEnv: ProcessingEnvironment? = null
    internal var processingEnvironment: ProcessingEnvironment
        get() {
            if (_processingEnv == null) throw IllegalStateException("AnnotationParser.setup(ProcessingEnvironment) must be called before using annotation parser extensions.")
            return _processingEnv!!
        }
        set(processingEnv) {
            _processingEnv = processingEnv
        }

    fun setup(processingEnv: ProcessingEnvironment) {
        this.processingEnvironment = processingEnv
    }
}