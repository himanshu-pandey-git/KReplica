package io.availe.extensions

import com.google.devtools.ksp.symbol.KSClassDeclaration

internal data class KReplicaAnnotationContext(
    val modelAnnotation: KSClassDeclaration
)