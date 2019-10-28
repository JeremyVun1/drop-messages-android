package com.example.drop_messages_android.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.drop_messages_android.R
import com.example.drop_messages_android.VoteState
import com.example.drop_messages_android.api.DropMessage
import com.example.drop_messages_android.format
import kotlinx.android.synthetic.main.card_create_drop.*
import kotlinx.android.synthetic.main.card_create_drop.tv_author
import kotlinx.android.synthetic.main.card_drop_fragment.*
import kotlinx.android.synthetic.main.card_drop_fragment.view.*


class DropMessageFragment : Fragment() {

    var msgId: Int? = null
    private var voteState: VoteState = VoteState.NONE
    private var upvoted: Boolean = false

    private var listener: DropMessageFragmentListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.card_drop_fragment, container, false) as ViewGroup

        val args = arguments
        val model = args!!.getParcelable<DropMessage>("model")
        val canDelete = args.getBoolean("canDelete")
        if (!canDelete)
            rootView.btn_delete.visibility = View.GONE

        if (model != null) {
            msgId = model.id

            val geoloc = "${model.lat.format(2)}, ${model.long.format(2)}"
            val author = "${model.author} @($geoloc)"

            rootView.tv_author.text = author
            rootView.tv_date.text = model.date
            rootView.tv_drop_message.text = model.message
            rootView.tv_seen_count.text = (model.seen + 1).toString()
            rootView.tv_vote_count.text = model.votes.toString()
            rootView.tv_date_label.text = model.date
            rootView.tv_geoloc_label.text = geoloc
        }

        setupButtonHandlers(rootView, canDelete)

        return rootView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as DropMessageFragmentListener
    }

    private fun setupButtonHandlers(view: View, canDelete: Boolean) {
        // SHARE
        view.btn_share.setOnClickListener {
            val i = Intent()
            i.action = Intent.ACTION_SEND

            val author = tv_author.text.toString()
            val geoloc = tv_geoloc_label.text.toString()
            val msg = tv_drop_message.text.toString()
            val sharedMsg = "I picked up a message @$geoloc from $author - \"$msg\""

            i.putExtra(Intent.EXTRA_TEXT, sharedMsg)
            i.type = "text/plain"

            startActivity(Intent.createChooser(i, "Share Drop message to:"))
        }

        // UPVOTE
        view.btn_upvote.setOnClickListener {
            when(voteState) {
                VoteState.UPVOTE -> {
                    voteState = VoteState.NONE
                    view.btn_upvote.setBackgroundResource(R.drawable.ic_vote_arrow_up)
                    listener?.onDownvote(msgId)
                    view.tv_vote_count.text = (view.tv_vote_count.text.toString().toInt() - 1).toString()
                }
                VoteState.DOWNVOTE -> {
                    voteState = VoteState.UPVOTE
                    view.btn_upvote.setBackgroundResource(R.drawable.ic_vote_arrow_up_green)
                    view.btn_downvote.setBackgroundResource(R.drawable.ic_vote_arrow_down)
                    listener?.onUpvote(msgId)
                    listener?.onUpvote(msgId)
                    view.tv_vote_count.text = (view.tv_vote_count.text.toString().toInt() + 2).toString()
                }
                VoteState.NONE -> {
                    voteState = VoteState.UPVOTE
                    view.btn_upvote.setBackgroundResource(R.drawable.ic_vote_arrow_up_green)
                    listener?.onUpvote(msgId)
                    view.tv_vote_count.text = (view.tv_vote_count.text.toString().toInt() + 1).toString()
                }
            }
        }

        // DOWNVOTE
        view.btn_downvote.setOnClickListener {
            when(voteState) {
                VoteState.UPVOTE -> {
                    voteState = VoteState.DOWNVOTE
                    view.btn_upvote.setBackgroundResource(R.drawable.ic_vote_arrow_up)
                    view.btn_downvote.setBackgroundResource(R.drawable.ic_vote_arrow_down_red)
                    listener?.onDownvote(msgId)
                    listener?.onDownvote(msgId)
                    view.tv_vote_count.text = (view.tv_vote_count.text.toString().toInt() -2).toString()
                }
                VoteState.NONE -> {
                    voteState = VoteState.DOWNVOTE
                    view.btn_downvote.setBackgroundResource(R.drawable.ic_vote_arrow_down_red)
                    listener?.onDownvote(msgId)
                    view.tv_vote_count.text = (view.tv_vote_count.text.toString().toInt() - 1).toString()
                }
                VoteState.DOWNVOTE -> {
                    voteState = VoteState.NONE
                    view.btn_downvote.setBackgroundResource(R.drawable.ic_vote_arrow_down)
                    listener?.onUpvote(msgId)
                    view.tv_vote_count.text = (view.tv_vote_count.text.toString().toInt() + 1).toString()
                }
            }
        }

        if (canDelete) {
            view.btn_delete.setOnClickListener {
                listener?.onDelete(msgId)
            }
        }
    }

    interface DropMessageFragmentListener {
        fun onUpvote(id: Int?)
        fun onDownvote(id: Int?)
        fun onDelete(id: Int?)
    }

}