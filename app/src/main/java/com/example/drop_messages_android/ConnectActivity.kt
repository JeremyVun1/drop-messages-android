package com.example.drop_messages_android

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity


// Signing in from the main activity
class ConnectActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
    }

    /*
    PostRequest(url, json,
                {
                    val response = gson.fromJson(it.toString(), JsonObject::class.java)

                    if (response.has("token")) {
                        Toast.makeText(applicationContext, "successfully authorised", Toast.LENGTH_SHORT).show()
                        //
                    }

                },
                {

                }
            )
     */
}