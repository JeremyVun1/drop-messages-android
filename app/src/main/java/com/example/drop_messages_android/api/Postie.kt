package com.example.drop_messages_android.api

import android.content.Context
import com.android.volley.*
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

        request.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        // add it to our volley request queue
        NetworkSingleton.getInstance(context).addToRequestQueue(request)
    }
}