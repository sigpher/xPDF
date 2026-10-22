package com.sigpher.xpdf.preview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.view.View
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.sigpher.xpdf.R
import com.sigpher.xpdf.common.Settings
import com.sigpher.xpdf.common.utils.ClipboardHelper
import com.sigpher.xpdf.databinding.AppViewTranslateCardBinding
import java.util.Locale

/**
 * 选中文本的端侧翻译。
 *
 * - 语种识别：ML Kit language-id；
 * - 翻译：ML Kit translate，首次使用需要下载对应语言对模型（需要网络），之后完全离线；
 * - 目标语言记忆在 [Settings.translateTarget]，卡片上的语言按钮可循环切换；
 * - 模型不可用（无 GMS / 下载失败 / 不支持的语言）时降级为系统"用其他应用翻译"。
 */
class TranslateController(
    private val context: Context,
    private val binding: AppViewTranslateCardBinding
) {

    /**
     * ML Kit 客户端按需创建：不要在 Activity 打开（也即打开 PDF）时初始化，
     * 否则会白白付出一次 native 库加载耗时。
     */
    private val languageIdentifierLazy = lazy { LanguageIdentification.getClient() }
    private val languageIdentifier: LanguageIdentifier by languageIdentifierLazy

    private var translator: Translator? = null
    private var translatorKey: String? = null

    private var currentText = ""
    private var requestToken = 0

    init {
        binding.appTvTranslateClose.setOnClickListener { hide() }
        binding.appTvTranslateRetry.setOnClickListener {
            if (currentText.isNotEmpty()) {
                translate(currentText)
            }
        }
        binding.appTvTranslateLangToggle.setOnClickListener { cycleTargetLanguage() }
        binding.appTvTranslateCopy.setOnClickListener {
            val text = binding.appTvTranslateResult.text?.toString().orEmpty()
            if (text.isEmpty()) {
                ToastUtils.showShort(R.string.app_copy_empty)
                return@setOnClickListener
            }
            ClipboardHelper.copy(context, text)
            ToastUtils.showShort(R.string.app_copy_success)
        }
        binding.appTvTranslateExternal.setOnClickListener { openExternalTranslate() }
    }

    fun isVisible(): Boolean = binding.root.visibility == View.VISIBLE

    /** 翻译一段文本并显示卡片 */
    fun show(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            ToastUtils.showShort(R.string.app_copy_empty)
            return
        }
        currentText = trimmed
        binding.root.visibility = View.VISIBLE
        binding.appTvTranslateSrc.text = trimmed
        binding.appTvTranslateResult.text = ""
        binding.appTvTranslateExternal.visibility = View.GONE
        updateTargetLabel()
        translate(trimmed)
    }

    fun hide() {
        requestToken++
        binding.root.visibility = View.GONE
        binding.appPbTranslate.visibility = View.GONE
    }

    fun release() {
        requestToken++
        translator?.close()
        translator = null
        translatorKey = null
        if (languageIdentifierLazy.isInitialized()) {
            languageIdentifierLazy.value.close()
        }
    }

    //region 内部实现

    private fun translate(text: String) {
        val token = ++requestToken
        showLoading(context.getString(R.string.app_translate_identifying))
        // 该版本 ML Kit 只提供默认置信度的单参重载
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { tag ->
                    if (token == requestToken) {
                        onSourceIdentified(token, text, tag)
                    }
                }
                .addOnFailureListener { error ->
                    LogUtils.w("identify language failed: ${error.message}")
                    if (token == requestToken) {
                        showError(R.string.app_translate_no_language)
                    }
                }
    }

    private fun onSourceIdentified(token: Int, text: String, tag: String?) {
        val sourceLanguage = toTranslateLanguage(tag)
        if (sourceLanguage == null) {
            showError(R.string.app_translate_no_language)
            return
        }
        val targetLanguage = resolveTargetLanguage()
        if (sourceLanguage == targetLanguage) {
            showError(R.string.app_translate_same_language)
            return
        }
        binding.appTvTranslateLang.text = context.getString(R.string.app_translate_direction,
                labelOf(sourceLanguage), labelOf(targetLanguage))

        val client = obtainTranslator(sourceLanguage, targetLanguage)
        showLoading(context.getString(R.string.app_translate_downloading))
        client.downloadModelIfNeeded()
                .addOnSuccessListener {
                    if (token != requestToken) {
                        return@addOnSuccessListener
                    }
                    showLoading(context.getString(R.string.app_translate_translating))
                    client.translate(text)
                            .addOnSuccessListener { result ->
                                if (token == requestToken) {
                                    showResult(result)
                                }
                            }
                            .addOnFailureListener { error ->
                                LogUtils.w("translate failed: ${error.message}")
                                if (token == requestToken) {
                                    showError(R.string.app_translate_failed)
                                }
                            }
                }
                .addOnFailureListener { error ->
                    // 无 GMS、模型下载失败（常见于无网络）都走这里
                    LogUtils.w("download translate model failed: ${error.message}")
                    if (token == requestToken) {
                        showError(R.string.app_translate_failed)
                    }
                }
    }

    private fun obtainTranslator(sourceLanguage: String, targetLanguage: String): Translator {
        val key = "$sourceLanguage-$targetLanguage"
        val cached = translator
        if (cached != null && key == translatorKey) {
            return cached
        }
        cached?.close()
        val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()
        return Translation.getClient(options).also {
            translator = it
            translatorKey = key
        }
    }

    private fun showLoading(message: String) {
        binding.appPbTranslate.visibility = View.VISIBLE
        binding.appTvTranslateResult.text = message
        binding.appTvTranslateResult.alpha = 0.6f
    }

    private fun showResult(result: String) {
        binding.appPbTranslate.visibility = View.GONE
        binding.appTvTranslateResult.alpha = 1f
        binding.appTvTranslateResult.text = result
        binding.appTvTranslateExternal.visibility = View.VISIBLE
    }

    private fun showError(messageRes: Int) {
        binding.appPbTranslate.visibility = View.GONE
        binding.appTvTranslateResult.alpha = 1f
        binding.appTvTranslateResult.text = context.getString(messageRes)
        binding.appTvTranslateExternal.visibility = View.VISIBLE
    }

    private fun cycleTargetLanguage() {
        val current = resolveTargetLanguage()
        val index = TARGET_LANGUAGES.indexOf(current)
        val next = TARGET_LANGUAGES[(index + 1).coerceAtLeast(0) % TARGET_LANGUAGES.size]
        Settings.translateTarget = next
        updateTargetLabel()
        if (currentText.isNotEmpty()) {
            translate(currentText)
        }
    }

    private fun updateTargetLabel() {
        binding.appTvTranslateLangToggle.text = labelOf(resolveTargetLanguage())
    }

    private fun resolveTargetLanguage(): String {
        val configured = Settings.translateTarget
        if (configured.isNotEmpty()) {
            return configured
        }
        // 默认译成"另一种语言"：界面为中文则译英文，否则译中文
        val language = Locale.getDefault().language
        return if (language.startsWith("zh")) TranslateLanguage.ENGLISH else TranslateLanguage.CHINESE
    }

    private fun labelOf(language: String): String = when (language) {
        TranslateLanguage.CHINESE -> context.getString(R.string.app_language_chinese)
        TranslateLanguage.ENGLISH -> context.getString(R.string.app_language_english)
        TranslateLanguage.JAPANESE -> context.getString(R.string.app_language_japanese)
        TranslateLanguage.KOREAN -> context.getString(R.string.app_language_korean)
        else -> language
    }

    private fun toTranslateLanguage(tag: String?): String? {
        if (tag.isNullOrEmpty() || tag == "und") {
            return null
        }
        return TranslateLanguage.fromLanguageTag(tag)
    }

    private fun openExternalTranslate() {
        if (currentText.isEmpty()) {
            return
        }
        try {
            val processText = Intent(Intent.ACTION_PROCESS_TEXT)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_PROCESS_TEXT, currentText)
                    .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(Intent.createChooser(processText, context.getString(R.string.app_translate)))
        } catch (e: ActivityNotFoundException) {
            try {
                val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, currentText)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(Intent.createChooser(send, context.getString(R.string.app_translate)))
            } catch (e2: ActivityNotFoundException) {
                LogUtils.w("no activity to handle translate intent")
                ToastUtils.showShort(R.string.app_translate_failed)
            }
        }
    }

    //endregion

    companion object {
        private val TARGET_LANGUAGES = listOf(
                TranslateLanguage.CHINESE,
                TranslateLanguage.ENGLISH,
                TranslateLanguage.JAPANESE,
                TranslateLanguage.KOREAN)
    }
}
