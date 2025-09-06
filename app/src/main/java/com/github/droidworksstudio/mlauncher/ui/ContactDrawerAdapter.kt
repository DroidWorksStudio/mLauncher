package com.github.droidworksstudio.mlauncher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.common.getLocalizedString
import com.github.droidworksstudio.fuzzywuzzy.FuzzyFinder
import com.github.droidworksstudio.mlauncher.R
import com.github.droidworksstudio.mlauncher.data.ContactListItem
import com.github.droidworksstudio.mlauncher.data.Prefs
import com.github.droidworksstudio.mlauncher.databinding.AdapterAppDrawerBinding
import java.text.Normalizer

class ContactDrawerAdapter(
    private val context: Context,
    private val gravity: Int,
    private val contactClickListener: (ContactListItem) -> Unit,
    private val contactDeleteListener: (ContactListItem) -> Unit,
    private val contactRenameListener: (String, String) -> Unit
) : RecyclerView.Adapter<ContactDrawerAdapter.ViewHolder>(), Filterable {

    private lateinit var prefs: Prefs
    private var contactFilter = createContactFilter()
    var contactsList: MutableList<ContactListItem> = mutableListOf()
    var contactFilteredList: MutableList<ContactListItem> = mutableListOf()
    private lateinit var binding: AdapterAppDrawerBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = AdapterAppDrawerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        prefs = Prefs(parent.context)
        val fontColor = prefs.appColor
        binding.appTitle.setTextColor(fontColor)

        binding.appTitle.textSize = prefs.appSize.toFloat()
        val padding: Int = prefs.textPaddingSize
        binding.appTitle.setPadding(0, padding, 0, padding)
        return ViewHolder(binding, prefs)
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (contactFilteredList.isEmpty() || position !in contactFilteredList.indices) return

        val contactModel = contactFilteredList[holder.absoluteAdapterPosition]

        holder.bind(gravity, contactModel, contactClickListener, contactDeleteListener, contactRenameListener, prefs)
    }

    override fun getItemCount(): Int = contactFilteredList.size

    override fun getFilter(): Filter = this.contactFilter

    private fun createContactFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSearch: CharSequence?): FilterResults {
                prefs = Prefs(context)
                val searchChars = charSearch.toString().trim().lowercase()
                val filteredContacts: MutableList<ContactListItem>

                val query = searchChars
                val normalizeField: (ContactListItem) -> String = { contact -> normalize(contact.label) }

                filteredContacts = if (searchChars.isEmpty()) {
                    contactsList.toMutableList()
                } else {
                    contactsList.filter { contact ->
                        if (prefs.searchFromStart) {
                            normalizeField(contact).startsWith(query)
                        } else {
                            FuzzyFinder.isMatch(normalizeField(contact), query)
                        }
                    }.toMutableList()
                }

                val filterResults = FilterResults()
                filterResults.values = filteredContacts
                return filterResults
            }

            fun normalize(input: String): String {
                val temp = Normalizer.normalize(input, Normalizer.Form.NFC)
                return temp
                    .lowercase()
                    .filter { it.isLetterOrDigit() }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results?.values is MutableList<*>) {
                    contactFilteredList = results.values as MutableList<ContactListItem>
                    notifyDataSetChanged()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setContactList(contactsList: MutableList<ContactListItem>) {
        this.contactsList = contactsList
        this.contactFilteredList = contactsList
        notifyDataSetChanged()
    }

    class ViewHolder(
        itemView: AdapterAppDrawerBinding,
        private val prefs: Prefs
    ) : RecyclerView.ViewHolder(itemView.root) {

        val appRenameEdit: EditText = itemView.appRenameEdit
        val appSaveRename: TextView = itemView.appSaveRename
        val appDelete: TextView = itemView.appDelete
        private val appRenameLayout: LinearLayout = itemView.appRenameLayout
        private val appTitle: TextView = itemView.appTitle
        private val appTitleFrame: FrameLayout = itemView.appTitleFrame
        private val context = itemView.root.context

        fun bind(
            contactLabelGravity: Int,
            contactItem: ContactListItem,
            contactClickListener: (ContactListItem) -> Unit,
            contactDeleteListener: (ContactListItem) -> Unit,
            contactRenameListener: (String, String) -> Unit,
            prefs: Prefs
        ) = with(itemView) {

            appTitle.text = contactItem.label

            // set text gravity
            val params = appTitle.layoutParams as FrameLayout.LayoutParams
            params.gravity = contactLabelGravity
            appTitle.layoutParams = params

            // handle rename
            appRenameEdit.apply {
                addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable) {}
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                        if (appRenameEdit.text.isEmpty()) {
                            appSaveRename.text = getLocalizedString(R.string.reset)
                        } else if (appRenameEdit.text.toString() == contactItem.customLabel) {
                            appSaveRename.text = getLocalizedString(R.string.cancel)
                        } else {
                            appSaveRename.text = getLocalizedString(R.string.rename)
                        }
                    }
                })
                text = Editable.Factory.getInstance().newEditable(contactItem.label)
            }

            appSaveRename.setOnClickListener {
                val newName = appRenameEdit.text.toString().trim()
                contactItem.customLabel = newName
                contactRenameListener(contactItem.displayName, contactItem.customLabel)
            }

            appDelete.setOnClickListener {
                contactDeleteListener(contactItem)
            }

            appTitleFrame.setOnClickListener {
                contactClickListener(contactItem)
            }

            val padding = 24
            appTitle.updatePadding(left = padding, right = padding)
        }
    }
}
