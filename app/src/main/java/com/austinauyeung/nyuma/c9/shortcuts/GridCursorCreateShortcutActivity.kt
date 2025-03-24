package com.austinauyeung.nyuma.c9.shortcuts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.austinauyeung.nyuma.c9.R

class GridCursorCreateShortcutActivity : Activity() {
    @Suppress("Deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val shortcutIntent = Intent(Intent.ACTION_VIEW)
        shortcutIntent.setClassName(packageName, "com.austinauyeung.nyuma.c9.shortcuts.GridCursorActivity")

        val intent = Intent()
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Activate Grid Cursor")
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
            Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher))

        setResult(RESULT_OK, intent)
        finish()
    }
}