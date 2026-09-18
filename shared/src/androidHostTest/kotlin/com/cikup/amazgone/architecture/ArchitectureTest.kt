package com.cikup.amazgone.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test

/**
 * Enforces the MVI + Clean Architecture rules from CLAUDE.md.
 * Scope = production sources of the shared module only.
 */
class ArchitectureTest {

    private val scope = Konsist.scopeFromProduction(moduleName = "shared")

    private val forbiddenInDomain = listOf(
        ".data.", ".presentation.", "androidx.", "io.ktor.", "org.koin.",
        "dev.gitlive.", "coil3.", "org.jetbrains.compose.", "platform.",
    )

    @Test
    fun domainLayerIsPureKotlin() {
        scope.files
            .filter { it.packagee?.name?.contains(".domain") == true }
            .assertFalse(additionalMessage = "domain/ must not depend on data, presentation or frameworks") { file ->
                file.imports.any { import -> forbiddenInDomain.any { import.name.contains(it) } }
            }
    }

    @Test
    fun dataLayerDoesNotDependOnPresentation() {
        scope.files
            .filter { it.packagee?.name?.contains(".data") == true }
            .assertFalse { file -> file.imports.any { it.name.contains(".presentation.") } }
    }

    @Test
    fun viewModelsExtendMviViewModel() {
        scope.classes()
            .withNameEndingWith("ViewModel")
            .filterNot { it.name == "MviViewModel" }
            .assertTrue { it.hasParent { parent -> parent.name.substringBefore("<") == "MviViewModel" } }
    }

    @Test
    fun viewModelsLiveInPresentationPackages() {
        scope.classes()
            .withNameEndingWith("ViewModel")
            .filterNot { it.name == "MviViewModel" }
            .assertTrue { it.resideInPackage("..presentation..") }
    }

    @Test
    fun contractsDeclareStateIntentAndEffect() {
        scope.files
            .filter { it.name.endsWith("Contract") }
            .assertTrue(additionalMessage = "XxxContract must define State, Intent and Effect") { file ->
                val names = file.classesAndInterfacesAndObjects().map { it.name }
                listOf("State", "Intent", "Effect").all { suffix -> names.any { it.endsWith(suffix) } }
            }
    }

    @Test
    fun useCasesExposeSingleOperatorInvoke() {
        scope.classes()
            .withNameEndingWith("UseCase")
            .assertTrue { useCase ->
                val publicFunctions = useCase.functions(includeNested = false).filter { !it.hasPrivateModifier && !it.hasInternalModifier }
                publicFunctions.size == 1 && publicFunctions.single().let { it.name == "invoke" && it.hasOperatorModifier }
            }
    }

    @Test
    fun useCasesLiveInDomain() {
        scope.classes()
            .withNameEndingWith("UseCase")
            .assertTrue { it.resideInPackage("..domain.usecase..") }
    }
}
