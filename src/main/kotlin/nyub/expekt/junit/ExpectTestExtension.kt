package nyub.expekt.junit

import java.nio.file.Path
import kotlin.io.path.Path
import nyub.expekt.ExpectTests
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * JUnit5 [extension](https://junit.org/junit5/docs/current/user-guide/#extensions) automating expect tests setup.
 * Provides [ExpectTests.ExpectTest] parameters, and asserts that the provided [ExpectTests.ExpectTest]'s printed
 * content is consumed during the test (see @[ExpectUnhandledOutput] to change this behaviour).
 *
 * Example:
 * ```java
 *  @ExtendWith(ExpectTestExtension.class)
 *  @ExpectTestExtension.RootPath("src/test/kotlin")
 *  class Test {
 *   @Test
 *   void test(ExpectTests.ExpectTest t) {
 *     t.print("Ok");
 *     t.expect("""
 *     Ok
 *     """);
 *   }
 *  }
 * ```
 *
 * ### Configuration
 *
 * Each parameter is configurable by a system property or by annotation. Annotations take precedence over system
 * properties.
 *
 * #### Promotion (in-place update) of expected string blocks
 * - default: `false`
 * - system property: [PROMOTE_PROPERTY_KEY]
 * - annotation: [Promote]
 *
 * #### Classes root path
 * - default: `"src/test/kotlin"`
 * - system property: [CLASSES_ROOT_PROPERTY_KEY]
 * - annotation: [RootPath]
 */
class ExpectTestExtension : ParameterResolver, BeforeEachCallback, AfterEachCallback, BeforeAllCallback {
    /**
     * Do not fail if the [ExpectTests.ExpectTest] output buffer is not empty at the end of the test
     *
     * @throws AssertionError if the [ExpectTests.ExpectTest] output buffer is empty at the end of the test
     */
    @Target(AnnotationTarget.FUNCTION) annotation class ExpectUnhandledOutput

    /** Override system property for promotion behavior */
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION) annotation class Promote(val value: Boolean)

    /** Override system property for sources root path */
    @Target(AnnotationTarget.CLASS) annotation class RootPath(val value: String)

    private var promote: Boolean = System.getProperty(PROMOTE_PROPERTY_KEY, "false") == "true"
    private var root: Path = System.getProperty(CLASSES_ROOT_PROPERTY_KEY, "src/test/kotlin").let(::Path)
    private lateinit var expectTests: ExpectTests
    private lateinit var expectTest: ExpectTests.ExpectTest

    companion object {
        /** Set to `"true"` to trigger promotion */
        const val PROMOTE_PROPERTY_KEY = "nyub.expekt.promote"

        /**
         * Path of the test source code from test run folder, should lead to the last folder before package folders,
         * e.g. `"src/test/java"` for the usual maven setup.
         */
        const val CLASSES_ROOT_PROPERTY_KEY = "nyub.expekt.root"
    }

    override fun beforeAll(ctx: ExtensionContext) {
        ctx.testClass.ifPresent {
            it.annotations.forEach { annotation ->
                if (annotation is Promote) {
                    promote = annotation.value
                }
                if (annotation is RootPath) {
                    root = annotation.value.let(::Path)
                }

                expectTests = ExpectTests(root, promote)
            }
        }
    }

    override fun supportsParameter(parameterContext: ParameterContext, ctx: ExtensionContext): Boolean {
        return parameterContext.parameter.type == ExpectTests.ExpectTest::class.java
    }

    override fun resolveParameter(parameterContext: ParameterContext, ctx: ExtensionContext): Any {
        return expectTest
    }

    override fun beforeEach(ctx: ExtensionContext) {
        var overrideBaseConfig = expectTests
        ctx.testMethod.ifPresent {
            it.annotations.forEach { annotation ->
                if (annotation is Promote) overrideBaseConfig = overrideBaseConfig.copy(promote = annotation.value)
            }
        }
        expectTest = overrideBaseConfig.expectTest()
    }

    override fun afterEach(ctx: ExtensionContext) {
        if (ctx.testMethod.isPresent && ctx.testMethod.get().annotations.any { it is ExpectUnhandledOutput }) {
            if (expectTest.output.isEmpty())
                throw AssertionError(
                    "Expected unhandled output but got nothing, remove the ${ExpectUnhandledOutput::class.simpleName} annotation"
                )
            expectTest.clear()
        }
        expectTest.end()
    }
}
