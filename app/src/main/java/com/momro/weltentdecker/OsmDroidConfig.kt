package com.momro.weltentdecker

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

object OsmDroidConfig {
    fun init(context: Context) {
        val prefs = context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, prefs)
        Configuration.getInstance().userAgentValue = context.packageName
        val base = File(context.cacheDir, "osmdroid")
        base.mkdirs()
        Configuration.getInstance().osmdroidBasePath = base
        Configuration.getInstance().osmdroidTileCache = File(base, "tiles")
    }
}
