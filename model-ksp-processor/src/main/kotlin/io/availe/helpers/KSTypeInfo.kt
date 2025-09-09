package io.availe.helpers

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import io.availe.models.TypeInfo

internal class ResolutionException : Exception()

internal data class KSTypeInfo(
    val qualifiedName: String,
    val arguments: List<KSTypeInfo>,
    val isNullable: Boolean,
    val isEnum: Boolean,
    val isValueClass: Boolean,
    val isDataClass: Boolean,
    val requiresContextual: Boolean
) {
    companion object {
        private const val JVM_INLINE_ANNOTATION_FQN = "kotlin.jvm.JvmInline"

        fun from(ksType: KSType, environment: SymbolProcessorEnvironment, resolver: Resolver): KSTypeInfo {
            if (ksType.isError) {
                val typeName = ksType.declaration.simpleName.asString()
                environment.logger.info("KReplica KSP: Deferring symbol because type '$typeName' is not yet resolved.")
                throw ResolutionException()
            }

            val decl = ksType.declaration as KSClassDeclaration
            val qualified = decl.qualifiedName?.asString()
                ?: throw IllegalStateException("Failed to get qualified name for declaration '${decl.simpleName.asString()}'")

            val args =
                ksType.arguments.mapNotNull { it.type?.resolve()?.let { type -> from(type, environment, resolver) } }
            val nullable = ksType.isMarkedNullable
            val isEnum = decl.classKind == ClassKind.ENUM_CLASS
            val isData = decl.modifiers.contains(Modifier.DATA)
            val needsContextual = needsContextualSerializer(ksType, resolver)

            val isValueByModifier = decl.modifiers.contains(Modifier.VALUE)
            val isValueByAnnotation = decl.annotations.any {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == JVM_INLINE_ANNOTATION_FQN
            }
            val isValue = isValueByModifier || isValueByAnnotation

            environment.logger.logging("KSTypeInfo.from qualifiedName=$qualified isEnum=$isEnum isValueClass=$isValue isDataClass=$isData")
            return KSTypeInfo(qualified, args, nullable, isEnum, isValue, isData, needsContextual)
        }
    }
}

internal fun KSTypeInfo.toModelTypeInfo(): TypeInfo =
    TypeInfo(
        qualifiedName = qualifiedName,
        arguments = arguments.map { it.toModelTypeInfo() },
        isNullable = isNullable,
        isEnum = isEnum,
        isValueClass = isValueClass,
        isDataClass = isDataClass,
        requiresContextual = requiresContextual
    )