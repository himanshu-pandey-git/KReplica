package io.availe.builders

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import io.availe.helpers.*
import io.availe.models.Variant

internal fun generateStubs(declarations: List<KSClassDeclaration>, env: SymbolProcessorEnvironment) {
    if (declarations.isEmpty()) return
    val modelsByBaseName = declarations.groupBy {
        determineVersioningInfo(it, env)?.baseModelName ?: it.simpleName.asString()
    }

    modelsByBaseName.forEach { (baseName, versions) ->
        createStubFileFor(baseName, versions, env)
    }
}

private fun createStubFileFor(baseName: String, versions: List<KSClassDeclaration>, env: SymbolProcessorEnvironment) {
    val representativeModel = versions.first()
    val packageName = representativeModel.packageName.asString()
    val schemaFileName = baseName + "Schema"
    val allSourceFiles = versions.mapNotNull { it.containingFile }.toTypedArray()

    val visibilityModifier = if (representativeModel.modifiers.contains(Modifier.INTERNAL)) "internal" else "public"

    val optInMarkers = versions.flatMap { extractAllOptInMarkers(it) }.distinct()
    val optInAnnotationString = if (optInMarkers.isNotEmpty()) {
        "@kotlin.OptIn(${optInMarkers.joinToString(", ") { "$it::class" }})"
    } else ""

    val isSerializable = versions.any { isModelSerializable(it) }
    val serializableAnnotationString = if (isSerializable) "@kotlinx.serialization.Serializable" else ""

    val nestedContent = buildNestedStubContent(baseName, versions, isSerializable, env).prependIndent("    ")

    val header = if (packageName.isNotEmpty()) "package $packageName\n\n" else ""
    val fileContent = """
        $header$optInAnnotationString
        $serializableAnnotationString
        $visibilityModifier sealed interface $schemaFileName {
        $nestedContent
        }
    """.trimIndent()

    val file = env.codeGenerator.createNewFile(
        dependencies = Dependencies(true, *allSourceFiles),
        packageName = packageName,
        fileName = schemaFileName
    )

    file.writer().use { it.write(fileContent) }
}

private fun buildNestedStubContent(
    baseName: String,
    versions: List<KSClassDeclaration>,
    isParentSerializable: Boolean,
    env: SymbolProcessorEnvironment
): String {
    val representative = versions.first()
    val isVersioned = determineVersioningInfo(representative, env) != null
    val serializableAnnotation = if (isParentSerializable) "@kotlinx.serialization.Serializable" else ""

    if (!isVersioned) {
        val model = versions.first()
        val variants = getVariantsFromAnnotation(model)
        return variants.joinToString("\n\n") { variant ->
            """
            $serializableAnnotation
            class ${variant.suffix}
            """.trimIndent()
        }
    } else {
        return versions
            .filter { determineVersioningInfo(it, env) != null }
            .joinToString("\n\n") { versionDecl ->
                val versionInfo = determineVersioningInfo(versionDecl, env)!!
                val variants = getVariantsFromAnnotation(versionDecl)
                val variantContent = variants.joinToString("\n\n") { variant ->
                    """
                    $serializableAnnotation
                    class ${variant.suffix}
                    """.trimIndent()
                }.prependIndent("    ")

                """
                $serializableAnnotation
                sealed interface ${versionDecl.simpleName.asString()} : ${versionInfo.baseModelName}Schema {
                $variantContent
                }
                """.trimIndent()
            }
    }
}

private fun getVariantsFromAnnotation(declaration: KSClassDeclaration): List<Variant> {
    val modelAnnotation = declaration.annotations.first { it.isAnnotation(MODEL_ANNOTATION_NAME) }
    val variantsArgument = modelAnnotation.arguments.find { it.name?.asString() == "variants" }

    return (variantsArgument?.value as? List<*>)
        ?.mapNotNull { (it as? KSDeclaration)?.simpleName?.asString() }
        ?.map { Variant.valueOf(it) }
        ?: emptyList()
}

private fun isModelSerializable(declaration: KSClassDeclaration): Boolean {
    return declaration.annotations.any { annotation ->
        if (!annotation.isAnnotation(REPLICATE_APPLY_ANNOTATION_NAME)) return@any false
        val annotationsArg = annotation.arguments.find { it.name?.asString() == "annotations" }?.value as? List<*>
        annotationsArg?.any {
            (it as? KSType)?.declaration?.qualifiedName?.asString() == SERIALIZABLE_ANNOTATION_FQN
        } ?: false
    }
}
