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

    fun init(app: Application, loc: Geolocation) : SocketManager {
        if (::socket.isInitialized)
            return this
        else {
            application = app
            lifecycleSwitch = LifecycleRegistry(0L)
            socket = createSocket(loc)
        }
        return this
    }

    fun changeSocket(loc: Geolocation) {
        closeSocket()
        socket = createSocket(loc)
    }

    fun getWebSocket() : DropMessageService? {
        if (::socket.isInitialized)
            return socket
        return null
    }

    fun closeSocket() {
        lifecycleSwitch.onNext(Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL))
    }

    private fun openSocket() {
        lifecycleSwitch.onNext(Lifecycle.State.Started)
    }


    fun createSocket(loc: Geolocation): DropMessageService {
        val socketUrl = "${application.resources.getString(R.string.web_socket_url)}${loc.formattedString()}/"
        println("websocket created to $socketUrl")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val lifecycle = AndroidLifecycle.ofApplicationForeground(application)
            .combineWith(lifecycleSwitch)

        val backoffStrategy = ExponentialWithJitterBackoffStrategy(5000, 5000)

        val scarlet = Scarlet.Builder()
            .webSocketFactory(client.newWebSocketFactory(socketUrl))
            .lifecycle(lifecycle)
            .addMessageAdapterFactory(GsonMessageAdapter.Factory())
            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
            .backoffStrategy(backoffStrategy)
            .build()
        socket = scarlet.create()
        return socket
    }
}