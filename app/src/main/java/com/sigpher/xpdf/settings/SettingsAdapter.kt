package com.sigpher.xpdf.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView
import com.sigpher.xpdf.R
import com.sigpher.xpdf.databinding.AppRecyclerItemSettingsSwitchBinding
import com.sigpher.xpdf.databinding.AppRecyclerItemSettingsRecentCountBinding
import com.sigpher.xpdf.common.App
import com.sigpher.xpdf.common.Settings
import com.sigpher.xpdf.common.event.MaxRecentEvent
import com.blankj.utilcode.util.ConvertUtils
import org.greenrobot.eventbus.EventBus

/**
 * @author sigpher
 */
internal class SettingsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val maxRecentEvent: MaxRecentEvent = MaxRecentEvent()
    private val maxRecentCounts: List<String>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        if (viewType == TYPE_NUM_PICKER) {
            val binding = AppRecyclerItemSettingsRecentCountBinding.inflate(inflater, parent, false)
            val holder = MaxRecentHolder(binding)
            holder.itemView.setOnClickListener { holder.binding.appSpinner.performClick() }
            holder.binding.appSpinner.dropDownHorizontalOffset = -ConvertUtils.dp2px(46f)
            holder.binding.appSpinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                    val maxRecentCount = maxRecentCounts[position]
                    holder.binding.appTvCount.text = maxRecentCount
                    Settings.maxRecentCount = maxRecentCount
                    EventBus.getDefault().post(maxRecentEvent)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            return holder
        }
        val binding = AppRecyclerItemSettingsSwitchBinding.inflate(inflater, parent, false)
        val holder = SwitchHolder(binding)
        holder.itemView.setOnClickListener {
            val pos = holder.adapterPosition
            holder.binding.appSwitch.isChecked = !holder.binding.appSwitch.isChecked
            tapOptions(holder.binding.appSwitch, pos)
        }
        return holder
    }

    private fun tapOptions(switcher: Switch, pos: Int) {
        when (pos) {
            POS_VOLUME_CONTROL -> Settings.volumeControl = switcher.isChecked
            POS_CLICK_FLIP_PAGE -> Settings.clickFlipPage = switcher.isChecked
            POS_SHOW_STATUS_BAR -> Settings.showStatusBar = switcher.isChecked
            POS_KEEP_SCREEN_ON -> Settings.keepScreenOn = switcher.isChecked
            POS_LINEAR_LAYOUT -> Settings.linearLayout = switcher.isChecked
            POS_SCROLL_SHORTCUT -> Settings.scrollShortCut = switcher.isChecked
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (viewHolder) {
            is SwitchHolder -> {
                when (position) {
                    POS_VOLUME_CONTROL -> {
                        viewHolder.binding.appTvTitle.setText(R.string.app_volume_control)
                        viewHolder.binding.appSwitch.isChecked = Settings.volumeControl
                    }
                    POS_CLICK_FLIP_PAGE -> {
                        viewHolder.binding.appTvTitle.setText(R.string.app_click_flip_page)
                        viewHolder.binding.appSwitch.isChecked = Settings.clickFlipPage
                    }
                    POS_SHOW_STATUS_BAR -> {
                        viewHolder.binding.appTvTitle.setText(R.string.app_show_status_bar)
                        viewHolder.binding.appSwitch.isChecked = Settings.showStatusBar
                    }
                    POS_KEEP_SCREEN_ON -> {
                        viewHolder.binding.appTvTitle.setText(R.string.app_keep_screen_on)
                        viewHolder.binding.appSwitch.isChecked = Settings.keepScreenOn
                    }
                    POS_LINEAR_LAYOUT -> {
                        viewHolder.binding.appTvTitle.setText(R.string.app_linear_layout)
                        viewHolder.binding.appSwitch.isChecked = Settings.linearLayout
                    }
                    POS_SCROLL_SHORTCUT -> {
                        viewHolder.binding.appTvTitle.setText(R.string.app_scroll_shortcut)
                        viewHolder.binding.appSwitch.isChecked = Settings.scrollShortCut
                    }
                }
            }
            is MaxRecentHolder -> {
                viewHolder.binding.appTvTitle.setText(R.string.app_max_recent_count)
                viewHolder.binding.appTvCount.text = Settings.maxRecentCount
                viewHolder.binding.appSpinner.setSelection(maxRecentCounts.indexOf(Settings.maxRecentCount))
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == POS_NUM_PICKER) TYPE_NUM_PICKER else TYPE_SWITCH
    }

    override fun getItemCount(): Int {
        return ITEM_COUNT
    }

    internal class SwitchHolder(val binding: AppRecyclerItemSettingsSwitchBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            val mlp = binding.root.layoutParams as MarginLayoutParams
            mlp.bottomMargin = ConvertUtils.dp2px(0.6f)
            binding.root.layoutParams = mlp
        }
    }

    internal class MaxRecentHolder(val binding: AppRecyclerItemSettingsRecentCountBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            val mlp = binding.root.layoutParams as MarginLayoutParams
            mlp.topMargin = ConvertUtils.dp2px(12f)
            binding.root.layoutParams = mlp
        }
    }

    companion object {
        private const val ITEM_COUNT = 7

        private const val TYPE_SWITCH = 0
        private const val TYPE_NUM_PICKER = 1

        private const val POS_VOLUME_CONTROL = 0
        private const val POS_CLICK_FLIP_PAGE = 1
        private const val POS_SHOW_STATUS_BAR = 2
        private const val POS_KEEP_SCREEN_ON = 3
        private const val POS_LINEAR_LAYOUT = 4
        private const val POS_SCROLL_SHORTCUT = 5
        private const val POS_NUM_PICKER = 6
    }

    init {
        val array = App.getContext().resources.getStringArray(R.array.max_recent_count)
        maxRecentCounts = listOf(*array)
    }
}
