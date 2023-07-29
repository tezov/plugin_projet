package com.tezov.plugin_project

import java.io.File

object Utils {

    inline val String.normalizePath get() = run {
        var path = this
        if (File.separator != "/") {
            path = path.replace("/", File.separator)
        }
        if (File.separator != "\\") {
            path = path.replace("\\", File.separator)
        }
        path
    }

}