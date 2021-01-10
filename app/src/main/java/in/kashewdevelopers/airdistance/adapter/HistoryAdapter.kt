package `in`.kashewdevelopers.airdistance.adapter

import `in`.kashewdevelopers.airdistance.data_containers.HistoryObject
import `in`.kashewdevelopers.airdistance.databinding.HistoryListItemBinding
import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter

class HistoryAdapter(context: Context, cursor: Cursor, flags: Int) : CursorAdapter(context, cursor, flags) {

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        val binding = HistoryListItemBinding
                .inflate(LayoutInflater.from(context), parent, false)
        val view = binding.root
        view.tag = binding
        return view
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        cursor ?: return
        view ?: return

        val binding = view.tag as HistoryListItemBinding
        val data = HistoryObject(cursor)

        binding.sourceName.text = data.sourceName
        binding.sourceLatLng.text = data.sourceLatLng

        binding.destinationName.text = data.destinationName
        binding.destinationLatLng.text = data.destinationLatLng

        binding.distance.text = data.distance

        binding.parent.setOnClickListener { onHistoryClickListener?.onHistoryClickListener(binding) }
        binding.deleteIcon.setOnClickListener { onDeleteClickListener?.onDeleteClickListener(data.hashCode) }
    }


    // interfaces to implement click listeners
    interface OnHistoryClickListener {
        fun onHistoryClickListener(binding: HistoryListItemBinding)
    }

    interface OnDeleteClickListener {
        fun onDeleteClickListener(hashCode: String)
    }

    private var onHistoryClickListener: OnHistoryClickListener? = null
    private var onDeleteClickListener: OnDeleteClickListener? = null

    fun setOnHistoryClickListener(onHistoryClickListener: OnHistoryClickListener?) {
        this.onHistoryClickListener = onHistoryClickListener
    }

    fun setOnDeleteClickListener(onDeleteClickListener: OnDeleteClickListener?) {
        this.onDeleteClickListener = onDeleteClickListener
    }

}