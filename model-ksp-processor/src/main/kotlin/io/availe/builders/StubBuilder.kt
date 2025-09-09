package io.availe.builders

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo
import io.availe.extensions.*
import io.availe.models.DtoVariant

internal fun generateStubs(declarations: List<KSClassDeclaration>, env: SymbolProcessorEnvironment) {
    if (declarations.isEmpty()) return
    val modelsByBaseName = declarations.groupBy {
        it.determineVersioningInfo(env)?.baseModelName ?: it.simpleName.asString()
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
    val schemaClassName = ClassName(packageName, schemaFileName)

    val visibilityModifier =
        if (representativeModel.modifiers.contains(Modifier.INTERNAL)) KModifier.INTERNAL else KModifier.PUBLIC

    val isGloballySerializable = versions.any { isModelSerializable(it) }

    val schemaInterface = TypeSpec.interfaceBuilder(schemaClassName).apply {
        addModifiers(KModifier.SEALED, visibilityModifier)
        if (isGloballySerializable) {
            addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
        }

        if (representativeModel.determineVersioningInfo(env) != null) {
            versions.forEach { versionDecl ->
                val versionInterface = buildVersionedStub(versionDecl, schemaClassName, isGloballySerializable, env)
                addType(versionInterface)
            }
        } else {
            val variants = getVariantsFromAnnotation(versions.first())
            variants.forEach { variant ->
                val variantClass = TypeSpec.classBuilder(variant.suffix).apply {
                    addSuperinterface(schemaClassName)
                    if (isGloballySerializable) {
                        addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
                    }
                }.build()
                addType(variantClass)
            }
        }
    }.build()

    FileSpec.builder(packageName, schemaFileName).apply {
        addType(schemaInterface)
        val optInMarkers = versions.flatMap { it.extractAllOptInMarkers() }.distinct()
        if (optInMarkers.isNotEmpty()) {
            val format = optInMarkers.joinToString(", ") { "%T::class" }
            val args = optInMarkers.map { it.asClassName() }.toTypedArray()
            addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .addMember(format, *args)
                    .build()
            )
        }
    }.build().writeTo(
        codeGenerator = env.codeGenerator,
        dependencies = Dependencies(true, *allSourceFiles)
    )
}

private fun buildVersionedStub(
    versionDecl: KSClassDeclaration,
    schemaClassName: ClassName,
    isGloballySerializable: Boolean,
    env: SymbolProcessorEnvironment
): TypeSpec {
    versionDecl.determineVersioningInfo(env)
        ?: error("Could not determine version info for ${versionDecl.simpleName.asString()}")
    val versionClassName = schemaClassName.nestedClass(versionDecl.simpleName.asString())
    val isVersionSerializable = isModelSerializable(versionDecl)

    return TypeSpec.interfaceBuilder(versionClassName).apply {
        addModifiers(KModifier.SEALED)
        addSuperinterface(schemaClassName)
        if (isGloballySerializable || isVersionSerializable) {
            addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
        }

        val variants = getVariantsFromAnnotation(versionDecl)
        variants.forEach { variant ->
            val variantClass = TypeSpec.classBuilder(variant.suffix).apply {
                addSuperinterface(versionClassName)
                if (isGloballySerializable || isVersionSerializable) {
                    addAnnotation(ClassName("kotlinx.serialization", "Serializable"))
                }
            }.build()
            addType(variantClass)
        }
    }.build()
}

private fun getVariantsFromAnnotation(declaration: KSClassDeclaration): List<DtoVariant> {
    val modelAnnotation = declaration.annotations.first { it.isAnnotation(MODEL_ANNOTATION_NAME) }
    val variantsArgument = modelAnnotation.arguments.find { it.name?.asString() == "variants" }

    return (variantsArgument?.value as? List<*>)
        ?.mapNotNull { (it as? KSDeclaration)?.simpleName?.asString() }
        ?.map { DtoVariant.valueOf(it) }
        ?: emptyList()
}

private fun isModelSerializable(declaration: KSClassDeclaration): Boolean {
    val hasSerializableAnnotation = declaration.annotations.any { annotation ->
        annotation.isAnnotation(SERIALIZABLE_ANNOTATION_FQN)
    }
    if (hasSerializableAnnotation) return true

    return declaration.annotations.any { annotation ->
        if (!annotation.isAnnotation(REPLICATE_APPLY_ANNOTATION_NAME)) return@any false
        val annotationsArg = annotation.arguments.find { it.name?.asString() == "annotations" }?.value as? List<*>
        annotationsArg?.any {
            (it as? KSType)?.declaration?.qualifiedName?.asString() == SERIALIZABLE_ANNOTATION_FQN
        } ?: false
    }
}