package com.lions.wantit.data.network.fcm

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class VolleyHelper(context: Context) {
    companion object{
        @Volatile // se supone que a continuación se realiza el patrón Singleton
        private var INSTANCE: VolleyHelper? = null
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this){
            INSTANCE ?: VolleyHelper(context).also { INSTANCE = it }
        }
    }

    private val requestQueue: RequestQueue by lazy{
        Volley.newRequestQueue(context.applicationContext)
    }

    fun<T> addToRequestQueue(req: Request<T>){
        requestQueue.add(req)
    }
}