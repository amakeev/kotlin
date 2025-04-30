/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import org.jetbrains.kotlin.js.test.converters.Fir2IrCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirCliWebFacade
import org.jetbrains.kotlin.js.test.converters.FirKlibSerializerCliWebFacade
import org.jetbrains.kotlin.js.test.converters.JsIrInliningFacade
import org.jetbrains.kotlin.js.test.ir.AbstractJsBlackBoxCodegenTestBase.JsBackendFacades
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.IrNoExpectSymbolsHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoFir2IrCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_HMPP
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.SEPARATE_KMP_COMPILATION
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_DOUBLE_CHECKING_COMMON_DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_PARSER
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.frontend.fir.FirCliMetadataFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliMetadataSerializerFacade
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.DelegatingEnvironmentConfiguratorForSeparateKmpCompilation
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.MetadataEnvironmentConfiguratorForSeparateKmpCompilation
import org.jetbrains.kotlin.test.services.isLeafModuleInMppGraph
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.MainFunctionForBlackBoxTestsSourceProvider
import org.jetbrains.kotlin.utils.bind
import java.lang.Boolean.getBoolean

abstract class AbstractJvmBlackBoxCodegenWithSeparateKmpCompilationTestBase(
    val parser: FirParser,
    private val pathToTestDir: String,
    private val testGroupOutputDirPrefix: String,
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JS_IR) {

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        globalDefaults {
            frontend = FrontendKinds.FIR
            targetPlatform = JsPlatforms.defaultJsPlatform
            dependencyKind = DependencyKind.Binary
        }

        val pathToRootOutputDir = System.getProperty("kotlin.js.test.root.out.dir") ?: error("'kotlin.js.test.root.out.dir' is not set")
        defaultDirectives {
            FIR_PARSER with parser
            +SEPARATE_KMP_COMPILATION
            +DISABLE_DOUBLE_CHECKING_COMMON_DIAGNOSTICS
            +WITH_STDLIB
            JsEnvironmentConfigurationDirectives.PATH_TO_ROOT_OUTPUT_DIR with pathToRootOutputDir
            JsEnvironmentConfigurationDirectives.PATH_TO_TEST_DIR with pathToTestDir
            JsEnvironmentConfigurationDirectives.TEST_GROUP_OUTPUT_DIR_PREFIX with testGroupOutputDirPrefix
            +JsEnvironmentConfigurationDirectives.GENERATE_NODE_JS_RUNNER
            if (getBoolean("kotlin.js.ir.skipRegularMode")) +JsEnvironmentConfigurationDirectives.SKIP_REGULAR_MODE
            LANGUAGE with "+JsAllowValueClassesInExternals"
            DIAGNOSTICS with "-warnings"
        }

        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::MetadataEnvironmentConfiguratorForSeparateKmpCompilation,
            ::JsEnvironmentConfiguratorForSeparateKmpCompilation,
        )

        useAdditionalSourceProviders(
            ::MainFunctionForBlackBoxTestsSourceProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        facadeStep(::FirCliMetadataFrontendFacade)
        facadeStep(::FirCliWebFacade)

        firHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler
            )
        }

        facadeStep(::FirCliMetadataSerializerFacade)
        facadeStep(::Fir2IrCliWebFacade)

        irHandlersStep {
            useHandlers(
                ::IrNoExpectSymbolsHandler,
                ::NoFir2IrCompilationErrorsHandler
            )
        }

        facadeStep(::JsIrInliningFacade)

        inlinedIrHandlersStep {
            useHandlers(::NoFir2IrCompilationErrorsHandler)
        }

        facadeStep(::FirKlibSerializerCliWebFacade)

        klibArtifactsHandlersStep {
            useHandlers(::KlibBackendDiagnosticsHandler)
        }

        facadeStep(JsBackendFacades.WithRecompilation.deserializerAndLoweringFacade)
        facadeStep(JsBackendFacades.WithRecompilation.recompileFacade)

        useAfterAnalysisCheckers(
            ::BlackBoxCodegenSuppressor.bind(IGNORE_HMPP),
        )
        enableMetaInfoHandler()
    }
}

class JsEnvironmentConfiguratorForSeparateKmpCompilation(
    testServices: TestServices
) : DelegatingEnvironmentConfiguratorForSeparateKmpCompilation(testServices, ::JsEnvironmentConfigurator) {
    override fun shouldApply(module: TestModule): Boolean {
        return module.isLeafModuleInMppGraph(testServices)
    }
}

open class AbstractJsLightTreeBlackBoxCodegenWithSeparateKmpCompilationTest : AbstractJvmBlackBoxCodegenWithSeparateKmpCompilationTestBase(
    FirParser.LightTree,
    pathToTestDir = "compiler/testData/codegen/box/multiplatform/k2",
    testGroupOutputDirPrefix = "codegen/irBoxHmpp/lightTree/"
)
