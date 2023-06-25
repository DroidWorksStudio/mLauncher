package com.github.hecodes2much.mlauncher.ui

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.hecodes2much.mlauncher.R
import com.github.hecodes2much.mlauncher.data.AppModel
import com.github.hecodes2much.mlauncher.data.Constants.AppDrawerFlag
import com.github.hecodes2much.mlauncher.data.Prefs
import com.github.hecodes2much.mlauncher.databinding.AdapterAppDrawerBinding
import com.github.hecodes2much.mlauncher.helper.dp2px
import com.github.hecodes2much.mlauncher.helper.getHexFontColor
import com.github.hecodes2much.mlauncher.helper.uninstallApp
import org.apache.commons.text.similarity.JaroWinklerSimilarity

class AppDrawerAdapter(
    private var flag: AppDrawerFlag,
    private val gravity: Int,
    private val clickListener: (AppModel) -> Unit,
    private val appInfoListener: (AppModel) -> Unit,
    private val appHideListener: (AppDrawerFlag, AppModel) -> Unit,
    private val appRenameListener: (String, String) -> Unit
) : RecyclerView.Adapter<AppDrawerAdapter.ViewHolder>(), Filterable {

    private lateinit var prefs: Prefs
    private var appFilter = createAppFilter()
    var appsList: MutableList<AppModel> = mutableListOf()
    var appFilteredList: MutableList<AppModel> = mutableListOf()
    private lateinit var binding: AdapterAppDrawerBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = AdapterAppDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        prefs = Prefs(parent.context)
        if (prefs.useCustomIconFont) {
            val typeface = ResourcesCompat.getFont(parent.context, R.font.roboto)
            binding.appTitle.typeface = typeface
        }
        if (prefs.followAccentColors) {
            val fontColor = getHexFontColor(parent.context)
            binding.appTitle.setTextColor(fontColor)
        }
        binding.appTitle.textSize = prefs.textSizeLauncher.toFloat()
        val padding: Int = prefs.textMarginSize
        binding.appTitle.setPadding(0, padding, 0, padding)
        return ViewHolder(binding)
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (appFilteredList.size == 0) return
        val appModel = appFilteredList[holder.absoluteAdapterPosition]
        holder.bind(flag, gravity, appModel, clickListener, appInfoListener)

        holder.appHideButton.setOnClickListener {
            appFilteredList.removeAt(holder.absoluteAdapterPosition)
            appsList.remove(appModel)
            notifyItemRemoved(holder.absoluteAdapterPosition)
            appHideListener(flag, appModel)
        }

        holder.appRenameButton.setOnClickListener {
            val name = holder.appRenameEdit.text.toString().trim()
            appModel.appAlias = name
            notifyItemChanged(holder.absoluteAdapterPosition)
            appRenameListener(appModel.appPackage, appModel.appAlias)
        }

        autoLaunch(position)
    }

    override fun getItemCount(): Int = appFilteredList.size

    override fun getFilter(): Filter = this.appFilter

    private val search = JaroWinklerSimilarity()
    private fun scoreApp(app: AppModel, searchChars: String): Float {
        val appChars = if (app.appAlias.isEmpty()) {
            app.appLabel
        } else {
            app.appAlias
        }

        val score = search.apply(
            appChars.uppercase()
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                .replace(Regex("[-_+,.]"), ""),
            searchChars.uppercase()
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                .replace(Regex("[-_+,.]"), ""))
        println("$appChars, $searchChars, $score, ${prefs.filterStrength}")

        return score.toFloat()
    }


    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchChars = constraint.toString()

                val scoredApps = mutableMapOf<AppModel, Float>()
                for (app in appsList){ scoredApps[app] = scoreApp(app, searchChars) }

                val appFilteredList = (if (searchChars.isEmpty()) appsList
                else scoredApps.filter { it.value > prefs.filterStrength.toFloat() / 100.0 }
                    .toSortedMap()
                    .map { it.key } as MutableList<AppModel>)

                val filterResults = FilterResults()
                filterResults.values = appFilteredList
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                appFilteredList = results?.values as MutableList<AppModel>
                notifyDataSetChanged()
            }
        }
    }

    private fun autoLaunch(position: Int) {
        val lastMatch = itemCount == 1
        val openApp = flag == AppDrawerFlag.LaunchApp
        val autoOpenApp = prefs.autoOpenApp
        if (lastMatch && openApp && autoOpenApp) {
            try { // Automatically open the app when there's only one search result
                clickListener(appFilteredList[position])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAppList(appsList: MutableList<AppModel>) {
        this.appsList = appsList
        this.appFilteredList = appsList
        notifyDataSetChanged()
    }

    fun launchFirstInList() {
        if (appFilteredList.size > 0)
            clickListener(appFilteredList[0])
    }

    class ViewHolder(itemView: AdapterAppDrawerBinding) : RecyclerView.ViewHolder(itemView.root) {
        val appHideButton: ImageView = itemView.appHide
        val appRenameButton: TextView = itemView.appRename
        val appRenameEdit: EditText = itemView.appRenameEdit
        private val appHideLayout: ConstraintLayout = itemView.appHideLayout
        private val appTitle: TextView = itemView.appTitle
        private val appTitleFrame: FrameLayout = itemView.appTitleFrame
        private val appInfo: ImageView = itemView.appInfo

        @SuppressLint("RtlHardcoded")
        fun bind(
            flag: AppDrawerFlag,
            appLabelGravity: Int,
            appModel: AppModel,
            listener: (AppModel) -> Unit,
            appInfoListener: (AppModel) -> Unit
        ) =
            with(itemView) {
                appHideLayout.visibility = View.GONE

                // set show/hide icon
                val drawable = if (flag == AppDrawerFlag.HiddenApps) { R.drawable.visibility } else { R.drawable.visibility_off }
                appHideButton.setImageDrawable(AppCompatResources.getDrawable(context, drawable))

                appRenameEdit.addTextChangedListener(object : TextWatcher {

                    override fun afterTextChanged(s: Editable) {}

                    override fun beforeTextChanged(
                        s: CharSequence, start: Int,
                        count: Int, after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence, start: Int,
                        before: Int, count: Int
                    ) {
                        if (appRenameEdit.text.isEmpty()) {
                            appRenameButton.text = context.getString(R.string.reset)
                        } else if (appRenameEdit.text.toString() == appModel.appAlias || appRenameEdit.text.toString() == appModel.appLabel) {
                            appRenameButton.text = context.getString(R.string.cancel)
                        } else {
                            appRenameButton.text = context.getString(R.string.rename)
                        }
                    }
                })

                val appName = appModel.appAlias.ifEmpty {
                    appModel.appLabel
                }

                appTitle.text = appName

                // set current name as default text in EditText
                appRenameEdit.text = Editable.Factory.getInstance().newEditable(appName)

                // set text gravity
                val params = appTitle.layoutParams as FrameLayout.LayoutParams
                params.gravity = appLabelGravity
                appTitle.layoutParams = params


                // add icon next to app name to indicate that this app is installed on another profile
                if (appModel.user != android.os.Process.myUserHandle()) {
                    val icon = AppCompatResources.getDrawable(context, R.drawable.work_profile)
                    val prefs = Prefs(context)
                    val px = dp2px(resources, prefs.textSizeLauncher)
                    icon?.setBounds(0, 0, px, px)
                    if (appLabelGravity == LEFT) {
                        appTitle.setCompoundDrawables(null, null, icon, null)
                    } else {
                        appTitle.setCompoundDrawables(icon, null, null, null)
                    }
                    appTitle.compoundDrawablePadding = 20
                } else {
                    appTitle.setCompoundDrawables(null, null, null, null)
                }

                appTitleFrame.setOnClickListener { listener(appModel) }
                appTitleFrame.setOnLongClickListener {
                    appHideLayout.visibility = View.VISIBLE
                    true
                }

                appInfo.apply {
                    setOnClickListener { appInfoListener(appModel) }
                    setOnLongClickListener {
                        uninstallApp(context, appModel.appPackage)
                        true
                    }
                }
                appHideLayout.setOnClickListener { appHideLayout.visibility = View.GONE }
            }
    }
}
