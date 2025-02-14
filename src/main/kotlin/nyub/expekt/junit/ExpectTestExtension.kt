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

class ExpectTestExtension : ParameterResolver, BeforeEachCallback, AfterEachCallback, BeforeAllCallback {
    private var promote: Boolean = System.getProperty("nyub.expekt.promote", "false") == "true"
    private var root: Path = System.getProperty("nyub.expekt.root", "src/test/kotlin").let(::Path)
    private lateinit var expectTests: ExpectTests
    private lateinit var expectTest: ExpectTests.ExpectTest

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
                expectTest = ExpectTests.ExpectTest(expectTests)
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
        expectTest.clear()
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

    /**
     * Do not fail if the [ExpectTests.ExpectTest] output buffer is not empty at the end of the test
     *
     * @throws AssertionError if the [ExpectTests.ExpectTest] output buffer is empty at the end of the test
     */
    @Target(AnnotationTarget.FUNCTION) annotation class ExpectUnhandledOutput

    /** Override system property for promotion behavior */
    @Target(AnnotationTarget.CLASS) annotation class Promote(val value: Boolean)

    /** Override system property for sources root path */
    @Target(AnnotationTarget.CLASS) annotation class RootPath(val value: String)
}
