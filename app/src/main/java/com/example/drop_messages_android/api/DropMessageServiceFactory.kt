package com.example.drop_messages_android.api

import android.app.Application
import com.example.drop_messages_android.R
import com.example.drop_messages_android.location.Geolocation
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.retry.ExponentialWithJitterBackoffStrategy
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object DropMessageServiceFactory {

    fun createSocket(application: Application, loc: Geolocation): DropMessageService {
        val socketUrl = "${application.resources.getString(R.string.web_socket_url)}${loc.formattedString()}/"
        println("websocket created to $socketUrl")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        val lifecycle = AndroidLifecycle.ofApplicationForeground(application)

        val backoffStrategy = ExponentialWithJitterBackoffStrategy(5000, 5000)

        val scarlet = Scarlet.Builder()
            .webSocketFactory(client.newWebSocketFactory(socketUrl))
            .lifecycle(lifecycle)
            .addMessageAdapterFactory(GsonMessageAdapter.Factory())
            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
            .backoffStrategy(backoffStrategy)
            .build()
        return scarlet.create()
    }
}