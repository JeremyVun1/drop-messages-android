package com.example.drop_messages_android.api

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

/**
 * Mr postie helps us make post requests to our REST api server
 * three cheers for mr postie
 */
class Postie {
    // Generic POST request
    fun sendPostRequest(context: Context, url: String, json: String,
                                responseListener: (it: JSONObject) -> Unit,
                                errorListener: (it: VolleyError) -> Unit ) {

        // build the request
        val request = object : JsonObjectRequest(
            Request.Method.POST, url, null,
            Response.Listener { responseListener(it) },
            Response.ErrorListener { errorListener(it) }
        ) {
            override fun getBody(): ByteArray {
                println("json: $json")
                return json.toByteArray()
            }
        }

        // add it to our volley request queue
        NetworkSingleton.getInstance(context).addToRequestQueue(request)
    }
}