package app.mlauncher.ui

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.Gravity.LEFT
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import app.mlauncher.R
import app.mlauncher.data.AppModel
import app.mlauncher.data.Constants.AppDrawerFlag
import app.mlauncher.data.Prefs
import app.mlauncher.databinding.AdapterAppDrawerBinding
import app.mlauncher.helper.dp2px
import app.mlauncher.helper.uninstallApp
import java.text.Normalizer

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
        binding.appTitle.textSize = prefs.textSize.toFloat()
        return ViewHolder(binding)
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (appFilteredList.size == 0) return
        val appModel = appFilteredList[holder.adapterPosition]
        holder.bind(flag, gravity, appModel, clickListener, appInfoListener)

        holder.appHideButton.setOnClickListener {
            appFilteredList.removeAt(holder.adapterPosition)
            appsList.remove(appModel)
            notifyItemRemoved(holder.adapterPosition)
            appHideListener(flag, appModel)
        }

        holder.appRenameButton.setOnClickListener {
            val name = holder.appRenameEdit.text.toString().trim()
            appModel.appAlias = name
            notifyItemChanged(holder.adapterPosition)
            Log.d("rename", "$appModel")
            appRenameListener(appModel.appPackage, appModel.appAlias)
        }

        autoLaunch(position)
    }

    override fun getItemCount(): Int = appFilteredList.size

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val searchChars = constraint.toString()
/*                 val appFilteredList = (if (searchChars.isEmpty()) appsList
 *                 else appsList.filter { app -> appLabelMatches(app.appLabel, searchChars) } as MutableList<AppModel>)
 *  */
                val appFilteredList = (if (searchChars.isEmpty()) appsList
                else appsList.filter { app ->
                    if (app.appAlias.isEmpty()) {
                        appLabelMatches(app.appLabel, searchChars)
                    } else {
                        appLabelMatches(app.appAlias, searchChars)
                    }
                } as MutableList<AppModel>)

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

    private fun appLabelMatches(appLabel: String, searchChars: String): Boolean {
        return (appLabel.contains(searchChars, true) or
                Normalizer.normalize(appLabel, Normalizer.Form.NFD)
                    .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                    .replace(Regex("[-_+,. ]"), "")
                    .contains(searchChars, true))
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
                    val px = dp2px(resources, prefs.textSize)
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
