package com.example.drop_messages_android.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.R
import com.example.drop_messages_android.api.DropMessage
import com.example.drop_messages_android.format
import kotlinx.android.synthetic.main.card_drop_fragment.*
import kotlinx.android.synthetic.main.card_drop_fragment.view.*


class DropMessageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.card_drop_fragment, container, false) as ViewGroup

        val args = arguments
        val model = args?.getParcelable<DropMessage>("model")

        if (model != null) {
            rootView.tv_author.text = model.author
            rootView.tv_date.text = model.date
            rootView.tv_drop_message.text = model.message
            rootView.tv_seen_count.text = model.seen.toString()
            rootView.tv_vote_count.text = model.votes.toString()
            rootView.tv_date_label.text = model.date

            val geoloc = "${model.lat.format(2)}, ${model.long.format(2)}"
            rootView.tv_geoloc_label.text = geoloc
        }

        return rootView
    }
}