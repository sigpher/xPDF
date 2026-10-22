package com.sigpher.xpdf.common.utils

import android.text.Editable
import android.text.TextWatcher

/**
 * TextWatcher 空实现，按需覆写其中的方法。
 *
 * 内联自 sigpher/Base 的 `TextWatcherImpl`（Apache License 2.0）。
 *
 * @author Aaron aaronzheng9603@gmail.com
 */
abstract class TextWatcherImpl : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(s: Editable) = Unit
}
