package com.example.drop_messages_android.api

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.util.Log
import android.widget.Toast
import com.example.drop_messages_android.R
import com.google.gson.Gson
import com.google.gson.JsonObject
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
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import io.reactivex.disposables.Disposable


/**
 * lateinit singleton for persisting our web socket over activities
 * Better than parcelable alternative
 */
object SocketManager {
    private lateinit var socket: DropMessageService
    private lateinit var application: Application
    private lateinit var lifecycleSwitch: LifecycleRegistry

    private lateinit var authRequestObserver: Disposable
    private lateinit var reAuthObserver: Disposable

    private lateinit var mLocation: Geolocation
    private lateinit var mUserModel: UserModel

    private var open: Boolean = false

    var authenticated = false

    fun init(app: Application, location: Geolocation, userModel: UserModel) : SocketManager {
        if (::socket.isInitialized) {
            closeSocket()

            application = app
            mLocation = location
            mUserModel = userModel

            createAuthObservers()
            openSocket()
        }
        else {
            lifecycleSwitch = LifecycleRegistry(0L)

            application = app
            mLocation = location
            mUserModel = userModel

            socket = createSocket()
            createAuthObservers()
            openSocket()
        }
        return this
    }

    fun getWebSocket() : DropMessageService? {
        if (::socket.isInitialized)
            return socket
        return null
    }

    fun closeSocket() {
        if (::socket.isInitialized && open) {
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

    private fun createAuthObservers() {
        if (::authRequestObserver.isInitialized)
            authRequestObserver.dispose()
        if (::reAuthObserver.isInitialized)
            reAuthObserver.dispose()

        authRequestObserver = socket.observeWebSocketEvent()
            .filter { it is WebSocket.Event.OnConnectionOpened<*> }
            .subscribe {
                socket.authenticate(
                    AuthenticateSocket(
                        DropRequest.AUTHENTICATE.value,
                        mUserModel.token as String,
                        mLocation.lat.toFloat(),
                        mLocation.long.toFloat()
                    )
                )
                println("<<[SND]attempt auth: ${mUserModel.token} @${mLocation}")
            }

        reAuthObserver = socket.observeSocketResponse()
            .subscribe {
                val category = it.category
                val data = it.data

                if (DropResponse.getEnum(category) == DropResponse.TOKEN) {
                    handleTokenResponses(data)
                }
            }
    }

    private fun handleTokenResponses(data: String) {
        Log.d("DEBUG", data)

        // get a fresh new token
        fetchJsonWebToken()
    }

    fun setNewUserLocation(userModel: UserModel) {
        mUserModel = userModel
        createAuthObservers()
    }

    /**
     * For refreshing JWT when it expires
     */
    private fun fetchJsonWebToken() {
        closeSocket()

        val url = application.resources.getString(R.string.get_token_url)
        val gson = Gson()

        val jsonRequest = gson.toJson(
            GetTokenModel(
                mUserModel.username as String,
                mUserModel.password as String
            )
        )

        // ask mr postie to request a new token
        Postie().sendPostRequest(application.applicationContext, url, jsonRequest,
            {
                val response = gson.fromJson(it.toString(), JsonObject::class.java)
                if (response.has("token")) {
                    println("JWT response: ${response["token"]}")

                    //strip any " chars pended on by the api server
                    val token = response["token"].toString()
                        .removePrefix("\"")
                        .removeSuffix("\"")

                    // save the new token in memory and in shared preferences
                    mUserModel.token = token

                    val sp = application.getSharedPreferences("Login", MODE_PRIVATE)
                    sp.edit().putString("token", token).apply()

                    openSocket()
                }
                else {
                    Log.e("ERROR", "Server failed to provide a token")
                    Toast.makeText(application.applicationContext,"Server connection lost!",Toast.LENGTH_SHORT).show()
                }
            },
            {
                Log.e("POST", it.toString())
            }
        )
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

        return socket
    }
}