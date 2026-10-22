package com.sigpher.xpdf.common.utils

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import androidx.annotation.StyleRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.sigpher.xpdf.R

/**
 * 对话框工厂：统一动画、背景与 BottomSheet 行为。
 *
 * @author sigpher
 */
object DialogUtils {

    fun createDialog(context: Context, layoutId: Int): Dialog {
        val dialog = Dialog(context)
        dialog.window?.apply {
            setWindowAnimations(R.style.AppDialogAnim)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.setContentView(layoutId)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    fun createDialog(context: Context, view: View): Dialog {
        val dialog = Dialog(context)
        dialog.window?.apply {
            setWindowAnimations(R.style.AppDialogAnim)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.setContentView(view)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    fun createBottomSheetDialog(context: Context, view: View): BottomSheetDialog {
        return createBottomSheetDialog(context, view, R.style.AppBottomDialogAnim, transparentBg = true)
    }

    fun createBottomSheetDialog(
            context: Context,
            view: View,
            @StyleRes animStyle: Int,
            transparentBg: Boolean
    ): BottomSheetDialog {
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)
        dialog.delegate?.findViewById<View>(R.id.design_bottom_sheet)?.let {
            BottomSheetBehavior.from(it).isHideable = false
        }
        dialog.window?.let { window: Window ->
            window.setWindowAnimations(animStyle)
            if (transparentBg) {
                window.findViewById<View>(R.id.design_bottom_sheet)
                        ?.setBackgroundResource(android.R.color.transparent)
            }
        }
        return dialog
    }
}
