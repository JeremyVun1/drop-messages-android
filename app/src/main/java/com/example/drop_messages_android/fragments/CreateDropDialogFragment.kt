package com.example.drop_messages_android.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.widget.addTextChangedListener
import com.example.drop_messages_android.R
import kotlinx.android.synthetic.main.card_create_drop.view.*


class CreateDropDialogFragment : AppCompatDialogFragment() {

    private var listener: CreateDropListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.card_create_drop, null)

        val bundle = requireArguments()
        view.tv_author.text = bundle.getString("author")

        val charCounter = view.tv_char_count
        val msgText = view.et_message

        msgText.addTextChangedListener {
            val charsLeft = "${resources.getInteger(R.integer.MAX_MESSAGE_LENGTH) - msgText.text.length} characters remaining"
            charCounter.text =  charsLeft
        }

        val builder = AlertDialog.Builder(activity)
        builder.setView(view)
            .setNegativeButton("Cancel",DialogInterface.OnClickListener { dialogInterface, i ->
                // nothing
            })
            .setPositiveButton("Drop", DialogInterface.OnClickListener { dialogInterface, i ->
                listener?.onCreateDrop(view.et_message.text.toString())
            })

        println("dialog created")
        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        listener = context as CreateDropListener
    }

    interface CreateDropListener {
        fun onCreateDrop(msg: String)
    }
}