/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.core.util.Header
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.server.handler.gzip.GzipHandler
import org.eclipse.jetty.util.resource.Resource
import java.io.File
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JettyResourceHandler : io.javalin.staticfiles.ResourceHandler {

    val handlers = mutableListOf<GzipHandler>()

    override fun addStaticFileConfig(config: StaticFileConfig) {
        handlers.add(GzipHandler().apply {
            handler = if (config.path == "/webjars") WebjarHandler() else ResourceHandler().apply {
                resourceBase = getResourcePath(config)
                isDirAllowed = false
                isEtags = true
                Javalin.log.info("Static file handler added with path=${config.path} and location=${config.location}. Absolute path: '${getResourcePath(config)}'.")
            }
            start()
        })
    }

    inner class WebjarHandler : ResourceHandler() {
        override fun getResource(path: String) = Resource.newClassPathResource("META-INF/resources$path") ?: super.getResource(path)
    }

    fun getResourcePath(staticFileConfig: StaticFileConfig): String {
        val nosuchdir = "Static resource directory with path: '${staticFileConfig.path}' does not exist."
        if (staticFileConfig.location == Location.CLASSPATH) {
            val classPathResource = Resource.newClassPathResource(staticFileConfig.path)
            if (classPathResource == null) {
                throw RuntimeException(nosuchdir + " Depending on your setup, empty folders might not get copied to classpath.")
            }
            return classPathResource.toString()
        }
        if (!File(staticFileConfig.path).exists()) {
            throw RuntimeException(nosuchdir)
        }
        return staticFileConfig.path
    }

    override fun handle(httpRequest: HttpServletRequest, httpResponse: HttpServletResponse, ignoreTrailingSlashes: Boolean): Boolean {
        val target = httpRequest.getAttribute("jetty-target") as String
        val baseRequest = httpRequest.getAttribute("jetty-request") as Request
        for (gzipHandler in handlers) {
            try {
                val resourceHandler = (gzipHandler.handler as ResourceHandler)
                val resource = resourceHandler.getResource(target)
                if (resource.isFile() || resource.isDirectoryWithWelcomeFile(resourceHandler, target, ignoreTrailingSlashes)) {
                    val maxAge = if (target.startsWith("/immutable/") || resourceHandler is WebjarHandler) 31622400 else 0
                    httpResponse.setHeader(Header.CACHE_CONTROL, "max-age=$maxAge")
                    gzipHandler.handle(target, baseRequest, httpRequest, httpResponse)
                    httpRequest.setAttribute("handled-as-static-file", true)
                    return true
                }
            } catch (e: Exception) { // it's fine
                Javalin.log.error("Exception occurred while handling static resource", e)
            }
        }
        return false
    }

    private fun Resource?.isFile() = this != null && this.exists() && !this.isDirectory

    private fun Resource?.isDirectoryWithWelcomeFile(handler: ResourceHandler, target: String, ignoreTrailingSlashes: Boolean) =
            this != null && this.isDirectory && handler.getResource(welcomeFilePath(target, ignoreTrailingSlashes))?.exists() == true

    private fun welcomeFilePath(target: String, ignoreTrailingSlashes: Boolean) = if (!target.endsWith("/") && ignoreTrailingSlashes) "$target/index.html" else "${target}index.html"

}
