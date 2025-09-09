package io.availe.helpers

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

internal sealed interface TypeValidationResult

internal data object Valid : TypeValidationResult

internal data class Invalid(
    val offendingDeclaration: KSClassDeclaration,
    val fullTypeName: String
) : TypeValidationResult

internal fun validateKReplicaTypeUsage(
    type: KSType,
    context: KReplicaAnnotationContext
): TypeValidationResult {
    if (type.isError) {
        return Valid
    }

    val declaration = type.declaration
    if (declaration is KSClassDeclaration) {
        val hasModelAnnotation = declaration.annotations.any {
            it.shortName.asString() == context.modelAnnotation.simpleName.asString() &&
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == context.modelAnnotation.qualifiedName?.asString()
        }
        if (hasModelAnnotation) {
            return Invalid(declaration, type.toString())
        }
    }

    for (argument in type.arguments) {
        val argumentType = argument.type?.resolve() ?: continue
        val result = validateKReplicaTypeUsage(argumentType, context)
        if (result is Invalid) {
            return Invalid(result.offendingDeclaration, type.toString())
        }
    }

    return Valid
}