package fredboat.test.extensions

import fredboat.main.Launcher
import fredboat.test.IntegrationTest
import fredboat.test.config.RabbitConfig
import fredboat.test.sentinel.CommandTester
import fredboat.test.sentinel.SentinelState
import kotlinx.coroutines.experimental.launch
import org.junit.jupiter.api.extension.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import java.lang.Thread.sleep
import java.util.concurrent.TimeoutException

class SharedSpringContext : ParameterResolver, BeforeAllCallback, AfterEachCallback {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SharedSpringContext::class.java)
        private var application: ApplicationContext? = null
    }

    override fun beforeAll(context: ExtensionContext) {
        if (application != null) return // Don't start the application again

        log.info("Initializing test context")
        launch { Launcher.main(emptyArray()) }
        var i = 0
        while (Launcher.instance == null) {
            sleep(1000)
            i++
            if (i > 60) throw TimeoutException("Context initialization timed out")
        }
        application = Launcher.instance!!.springContext
        sleep(4500) // Takes care of race conditions
        application!!.getBean(RabbitConfig.HelloSender::class.java).send()
        sleep(500) // Ample time for the hello to be received
        IntegrationTest.commandTester = application!!.getBean(CommandTester::class.java)
        log.info("Successfully initialized test context ${application!!.javaClass.simpleName}")
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return application!!.getBean(parameterContext.parameter.type) != null
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return application!!.getBean(parameterContext.parameter.type)
    }

    override fun afterEach(context: ExtensionContext?) {
        SentinelState.reset()
    }

}