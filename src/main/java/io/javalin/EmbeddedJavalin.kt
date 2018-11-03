package io.javalin

import io.javalin.core.JavalinServlet
import io.javalin.core.util.Util
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import java.util.function.Supplier

/**
 * Use this class instead of [Javalin] to embed Javalin into servlet containers such as Tomcat. Instantiating this class
 * allows you to exclude all Jetty dependencies and tie Javalin to a servlet as follows:
 *
 * ```
 * @WebServlet(urlPatterns = ["/rest/*"], name = "MyServlet", asyncSupported = false)  // */
 * class MyServlet : HttpServlet() {
 *     val javalin = EmbeddedJavalin()
 *         .get("/rest") { ctx -> ctx.result("Hello!") }
 *         .createServlet()
 *
 *     override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
 *         javalin.service(req, resp)
 *     }
 * }
 * ```
 *
 * Note that static files and uploads will not work without Jetty.
 */
class EmbeddedJavalin : Javalin(null, null) {

    init {
        Util.noJettyStarted = false // embeddable doesn't use Jetty
    }

    override fun createServlet() = JavalinServlet(
            javalin = this,
            matcher = pathMatcher,
            exceptionMapper = exceptionMapper,
            errorMapper = errorMapper,
            debugLogging = debugLogging,
            requestLogger = requestLogger,
            dynamicGzipEnabled = dynamicGzipEnabled,
            autogeneratedEtagsEnabled = autogeneratedEtagsEnabled,
            defaultContentType = defaultContentType,
            maxRequestCacheBodySize = maxRequestCacheBodySize,
            prefer405over404 = prefer405over404,
            singlePageHandler = singlePageHandler,
            jettyResourceHandler = null // no jetty here
    )

    override fun contextPath(contextPath: String) = notAvailable("contextPath()")
    override fun enableStaticFiles(classpathPath: String) = notAvailable("enableStaticFiles()")
    override fun enableWebJars() = notAvailable("enableWebJars()")
    override fun port() = notAvailable("port()")
    override fun port(port: Int) = notAvailable("port(port)")
    override fun server(server: Supplier<Server>) = notAvailable("server()")
    override fun sessionHandler(sessionHandler: Supplier<SessionHandler>) = notAvailable("sessionHandler()")
    override fun start() = notAvailable("start()")
    override fun stop() = notAvailable("stop()")
    private fun notAvailable(action: String): Nothing = throw RuntimeException("$action is not available in standalone mode")
}
