/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Context
import io.javalin.staticfiles.Location
import java.net.URL

/**
 * This is just a glorified 404 handler.
 * Ex: app.enableSinglePageMode("/my-path", "index.html")
 * If no routes or static files are found on "/my-path/" (or any subpath), index.html will be returned
 */
class SinglePageHandler {

    private val pathUrlMap = mutableMapOf<String, URL>()
    private val pathPageMap = mutableMapOf<String, String>()

    fun add(path: String, filePath: String, location: Location) {
        pathUrlMap[path] = when (location) {
            Location.CLASSPATH -> Util.getResource(filePath.removePrefix("/")) ?: throw IllegalArgumentException("File at '$filePath' not found. Path should be relative to resource folder.")
            Location.EXTERNAL -> Util.getFileUrl(filePath) ?: throw IllegalArgumentException("External file at '$filePath' not found.")
        }
        pathPageMap[path] = pathUrlMap[path]!!.readText()
    }

    fun handle(ctx: Context): Boolean { // this could be more idiomatic
        if (!ContextUtil.acceptsHtml(ctx)) return false
        for (entry in pathPageMap) {
            if (ctx.path().startsWith(entry.key)) {
                ctx.html(entry.value)
                return true
            }
        }
        return false
    }

}
