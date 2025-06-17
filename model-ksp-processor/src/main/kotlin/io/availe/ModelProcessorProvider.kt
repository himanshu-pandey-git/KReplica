package io.availe

import com.google.devtools.ksp.processing.*

class ModelProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ModelProcessor(environment)
}
