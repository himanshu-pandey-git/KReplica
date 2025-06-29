package io.availe.helpers

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.availe.models.INTRINSIC_SERIALIZABLES

private val serializabilityCache = mutableMapOf<String, Boolean>()

fun KSType.requiresContextual(resolver: Resolver): Boolean {
    val anyArgumentRequiresContextual = this.arguments.any {
        it.type?.resolve()?.requiresContextual(resolver) ?: false
    }
    if (anyArgumentRequiresContextual) {
        return true
    }

    val declaration = this.declaration as? KSClassDeclaration ?: return false
    val qualifiedName = declaration.qualifiedName?.asString() ?: return false

    if (INTRINSIC_SERIALIZABLES.contains(qualifiedName)) {
        return false
    }

    serializabilityCache[qualifiedName]?.let { return it }

    val isExplicitlySerializable = declaration.annotations.any {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == SERIALIZABLE_ANNOTATION_FQN
    }

    val needsContextual = !isExplicitlySerializable
    serializabilityCache[qualifiedName] = needsContextual
    return needsContextual
}