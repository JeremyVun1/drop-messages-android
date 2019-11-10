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
import kotlinx.android.synthetic.main.card_map_fragment.*


class MapDropMessageFragment : Fragment() {

    var msgId: String? = null
    private var voteState: VoteState = VoteState.NONE
    private var upvoted: Boolean = false

    private var listener: DropMessageFragmentListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.card_map_fragment, container, false) as ViewGroup

        return rootView
    }

    override fun onStart() {
        super.onStart()

        val args = arguments
        if (args != null) {
            val model = args.getParcelable<DropMessage>("model")
            val canDelete = args.getBoolean("canDelete")
            if (!canDelete)
                btn_delete.visibility = View.GONE

            if (model != null)
                loadModel(model)

            setupButtonHandlers(canDelete)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as DropMessageFragmentListener
    }

    private fun setupButtonHandlers(canDelete: Boolean) {
        // SHARE
        btn_share.setOnClickListener {
            val i = Intent()
            i.action = Intent.ACTION_SEND

            val author = tv_author.text.toString()
            val geoloc = "${tv_lat.text}, ${tv_long.text}"
            val msg = tv_drop_message.text.toString()
            val sharedMsg = "I picked up a message @$geoloc from $author - \"$msg\""

            i.putExtra(Intent.EXTRA_TEXT, sharedMsg)
            i.type = "text/plain"

            startActivity(Intent.createChooser(i, "Share Drop message to:"))
        }

        // UPVOTE
        btn_upvote.setOnClickListener {
            when(voteState) {
                VoteState.UPVOTE -> {
                    voteState = VoteState.NONE
                    btn_upvote.setBackgroundResource(R.drawable.ic_vote_arrow_up)
                    listener?.onDownvote(msgId)
                    tv_vote_count.text = (tv_vote_count.text.toString().toInt() - 1).toString()
                }
                VoteState.DOWNVOTE -> {
                    voteState = VoteState.UPVOTE
                    btn_upvote.setBackgroundResource(R.drawable.ic_vote_arrow_up_green)
                    btn_downvote.setBackgroundResource(R.drawable.ic_vote_arrow_down)
                    listener?.onUpvote(msgId)
                    listener?.onUpvote(msgId)
                    tv_vote_count.text = (tv_vote_count.text.toString().toInt() + 2).toString()
                }
                VoteState.NONE -> {
                    voteState = VoteState.UPVOTE
                    btn_upvote.setBackgroundResource(R.drawable.ic_vote_arrow_up_green)
                    listener?.onUpvote(msgId)
                    tv_vote_count.text = (tv_vote_count.text.toString().toInt() + 1).toString()
                }
            }
        }

        // DOWNVOTE
        btn_downvote.setOnClickListener {
            when(voteState) {
                VoteState.UPVOTE -> {
                    voteState = VoteState.DOWNVOTE
                    btn_upvote.setBackgroundResource(R.drawable.ic_vote_arrow_up)
                    btn_downvote.setBackgroundResource(R.drawable.ic_vote_arrow_down_red)
                    listener?.onDownvote(msgId)
                    listener?.onDownvote(msgId)
                    tv_vote_count.text = (tv_vote_count.text.toString().toInt() -2).toString()
                }
                VoteState.NONE -> {
                    voteState = VoteState.DOWNVOTE
                    btn_downvote.setBackgroundResource(R.drawable.ic_vote_arrow_down_red)
                    listener?.onDownvote(msgId)
                    tv_vote_count.text = (tv_vote_count.text.toString().toInt() - 1).toString()
                }
                VoteState.DOWNVOTE -> {
                    voteState = VoteState.NONE
                    btn_downvote.setBackgroundResource(R.drawable.ic_vote_arrow_down)
                    listener?.onUpvote(msgId)
                    tv_vote_count.text = (tv_vote_count.text.toString().toInt() + 1).toString()
                }
            }
        }

        if (canDelete) {
            btn_delete.setOnClickListener {
                listener?.onDelete(msgId)
            }
        }
    }

    fun loadModel(model: DropMessage) {
        msgId = model.id

        val lat = model.lat.format(2)
        val long = model.long.format(2)

        tv_lat.text = "latitude: $lat"
        tv_long.text = "longtitude: $long"
        tv_author.text = model.author
        tv_date.text = model.date.toDate().toString()
        tv_drop_message.text = model.message
        tv_seen_count.text = "seen: ${(model.seen + 1)}"
        tv_vote_count.text = model.votes.toString()
    }
}