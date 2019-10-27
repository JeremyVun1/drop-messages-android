package com.example.drop_messages_android.api

import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

/**
 * socket message protocoll definitions
 * Note: Scarlet websocket API automatically opens/closes sockets when
 * application goes to background/forefound
 */
interface DropMessageService {
    /**
     * Send Message declarations
     */
    @Send
    fun authenticate(model: AuthenticateSocket)

    @Send
    fun changeGeolocation(model: ChangeGeolocation)

    @Send
    fun requestDrops(model: RequestDrops)

    @Send
    fun requestDropsRange(model: RequestDropsRange)

    @Send
    fun upvote(model: Upvote)

    @Send
    fun downvote(model: Downvote)

    @Send
    fun createDrop(model: CreateDrop)

    @Send
    fun close(model: CloseSocket)

    /**
     * Response message models
     */
    @Receive
    fun observeWebSocketEvent(): Flowable<WebSocket.Event>

    @Receive
    fun observeAuthResponse(): Flowable<SocketResponse>

    @Receive
    fun observeSocketResponse(): Flowable<SocketResponse>
}