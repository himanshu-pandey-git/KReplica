package io.availe.helpers

import com.google.devtools.ksp.symbol.KSClassDeclaration

internal data class KReplicaAnnotationContext(
    val modelAnnotation: KSClassDeclaration
)