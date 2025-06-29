package io.availe.helpers

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.availe.models.AnnotationArgument
import io.availe.models.AnnotationModel

private val V_INT_REGEX = Regex("^V(\\d+)$")

internal fun fail(environment: SymbolProcessorEnvironment, message: String): Nothing {
    environment.logger.error(message)
    error(message)
}

internal fun KSAnnotation.isAnnotation(qualifiedName: String): Boolean {
    return this.annotationType.resolve().declaration.qualifiedName?.asString() == qualifiedName
}

internal fun extractAllOptInMarkers(declaration: KSClassDeclaration): List<String> {
    val classMarkers = declaration.annotations
        .filter { it.isAnnotation(OPT_IN_ANNOTATION_NAME) }
        .flatMap { optInAnnotation ->
            (optInAnnotation.arguments.first().value as? List<*>)?.mapNotNull {
                (it as? KSType)?.declaration?.qualifiedName?.asString()
            } ?: emptyList()
        }

    val propertyMarkers = declaration.getAllProperties()
        .flatMap { property -> property.annotations }
        .filter { annotation -> annotation.isAnnotation(OPT_IN_ANNOTATION_NAME) }
        .flatMap { optInAnnotation ->
            (optInAnnotation.arguments.first().value as? List<*>)?.mapNotNull {
                (it as? KSType)?.declaration?.qualifiedName?.asString()
            } ?: emptyList()
        }

    return (classMarkers + propertyMarkers).distinct().toList()
}

internal data class VersioningInfo(val baseModelName: String, val schemaVersion: Int)

internal fun determineVersioningInfo(
    declaration: KSClassDeclaration,
    environment: SymbolProcessorEnvironment
): VersioningInfo? {
    val baseInterface = declaration.superTypes
        .mapNotNull { it.resolve().declaration as? KSClassDeclaration }
        .firstOrNull {
            it.classKind == ClassKind.INTERFACE && !it.annotations.any { annotation ->
                annotation.isAnnotation(
                    MODEL_ANNOTATION_NAME
                )
            }
        }
        ?: return null
    val explicitVersion = declaration.annotations
        .firstOrNull { it.isAnnotation(REPLICATE_SCHEMA_VERSION_ANNOTATION_NAME) }
        ?.arguments
        ?.firstOrNull { it.name?.asString() == SCHEMA_VERSION_ARG }
        ?.value as? Int
    val inferredVersion = V_INT_REGEX.find(declaration.simpleName.asString())?.groupValues?.get(1)?.toIntOrNull()
    val version = explicitVersion ?: inferredVersion ?: fail(
        environment,
        "Versioned model '${declaration.simpleName.asString()}' must either be named 'V<N>' (e.g., V1) " +
                "or be annotated with @SchemaVersion(number = N)."
    )
    return VersioningInfo(baseInterface.simpleName.asString(), version)
}

private fun ksAnnotationToModel(
    annotation: KSAnnotation,
    frameworkDeclarations: Set<KSClassDeclaration>
): AnnotationModel? {
    val declaration = annotation.annotationType.resolve().declaration as? KSClassDeclaration ?: return null
    if (declaration in frameworkDeclarations) return null

    val arguments = annotation.arguments.mapNotNull { argument ->
        val key = argument.name?.asString() ?: "value"
        val rawValue = argument.value ?: return@mapNotNull null

        val modelArgument: AnnotationArgument? = when (rawValue) {
            is String -> AnnotationArgument.StringValue(rawValue)
            is KSType -> AnnotationArgument.LiteralValue("${rawValue.declaration.qualifiedName!!.asString()}::class")
            is KSAnnotation -> ksAnnotationToModel(
                rawValue,
                frameworkDeclarations
            )?.let { AnnotationArgument.AnnotationValue(it) }

            is List<*> -> {
                val listContents = rawValue.joinToString(", ") { item ->
                    when (item) {
                        is KSType -> "${item.declaration.qualifiedName!!.asString()}::class"
                        else -> item.toString()
                    }
                }
                AnnotationArgument.LiteralValue("[$listContents]")
            }

            else -> AnnotationArgument.LiteralValue(rawValue.toString())
        }

        modelArgument?.let { key to it }
    }.toMap()

    return AnnotationModel(declaration.qualifiedName!!.asString(), arguments)
}


fun Sequence<KSAnnotation>.toAnnotationModels(
    frameworkDeclarations: Set<KSClassDeclaration>
): List<AnnotationModel>? =
    mapNotNull { ksAnnotationToModel(it, frameworkDeclarations) }
        .toList()
        .takeIf { it.isNotEmpty() }

internal fun isNonHiddenModelAnnotation(declaration: KSClassDeclaration): Boolean {
    val isHidden = declaration.annotations.any {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == REPLICATE_HIDE_ANNOTATION_NAME
    }
    return declaration.classKind == ClassKind.INTERFACE && !isHidden
}

internal fun getFrameworkDeclarations(resolver: Resolver): Set<KSClassDeclaration> {
    return listOf(
        MODEL_ANNOTATION_NAME,
        REPLICATE_PROPERTY_ANNOTATION_NAME,
        REPLICATE_SCHEMA_VERSION_ANNOTATION_NAME,
        REPLICATE_FORCE_CONTEXTUAL_ANNOTATION_NAME,
        REPLICATE_WITH_SERIALIZER_ANNOTATION_NAME,
        REPLICATE_APPLY_ANNOTATION_NAME,
        REPLICATE_HIDE_ANNOTATION_NAME
    )
        .mapNotNull { fullyQualifiedName ->
            resolver.getClassDeclarationByName(resolver.getKSNameFromString(fullyQualifiedName))
        }
        .toSet()
}