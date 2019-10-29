package com.example.drop_messages_android.api

import android.app.Application
import com.example.drop_messages_android.R
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.retry.ExponentialWithJitterBackoffStrategy
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.lifecycle.LifecycleRegistry


/**
 * lateinit singleton for persisting our web socket over activities
 * Better than parcelable alternative
 */
object SocketManager {
    private lateinit var socket: DropMessageService
    private lateinit var application: Application
    private lateinit var lifecycleSwitch: LifecycleRegistry
    private var open: Boolean = false

    var authenticated = false

    fun init(app: Application) : SocketManager {
        if (::socket.isInitialized) {
            openSocket()
            return this
        }
        else {
            lifecycleSwitch = LifecycleRegistry(0L)
            application = app
            socket = createSocket()
        }
        return this
    }

    fun getWebSocket() : DropMessageService? {
        if (::socket.isInitialized)
            return socket
        return null
    }

    fun closeSocket() {
        if (open) {
            socket.close(CloseSocket(DropRequest.DISCONNECT.value))
            lifecycleSwitch.onNext(Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL))
            open = false
        }
    }

    fun openSocket() {
        if (::socket.isInitialized && !open) {
            lifecycleSwitch.onNext(Lifecycle.State.Started)
            open = true
        }
    }


    private fun createSocket(): DropMessageService {
        try {
            if (::socket.isInitialized)
                closeSocket()
        } catch(ex: Exception) {}

        val socketUrl = "${application.resources.getString(R.string.web_socket_url)}"

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val lifecycle = AndroidLifecycle.ofApplicationForeground(application).combineWith(lifecycleSwitch)

        val backoffStrategy = ExponentialWithJitterBackoffStrategy(5000, 5000)

        val scarlet = Scarlet.Builder()
            .webSocketFactory(client.newWebSocketFactory(socketUrl))
            .lifecycle(lifecycle)
            .addMessageAdapterFactory(GsonMessageAdapter.Factory())
            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
            .backoffStrategy(backoffStrategy)
            .build()
        socket = scarlet.create()

        println("websocket created to $socketUrl")
        openSocket()

        return socket
    }
}