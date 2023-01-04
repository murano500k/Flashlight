package com.stc.flashlight

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

val Any.TAG: String
    get() {
        val tag = javaClass.simpleName
        return if (tag.length <= 23) tag else tag.substring(0, 23)
    }

internal fun Context.showDialog(
    title: String,
    message: CharSequence,
    icon: Int?,
    action: () -> Unit = {},
    actionMessage: String = "",
    cancelable: Boolean = true
) {
    val builder = MaterialAlertDialogBuilder(
        this,
        R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
    )
        .setTitle(title)
        .setMessage(message)
        .setCancelable(cancelable)

    if (icon != null) {
        builder.setIcon(
            ResourcesCompat.getDrawable(resources, icon, theme)
        )
    }

    if (actionMessage.isNotBlank()) {
        builder.setPositiveButton(actionMessage) { _, _ ->
            action()
        }
    }
    builder.show()
}