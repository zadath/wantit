package com.lions.wantit.application

import android.app.Application
import com.lions.wantit.data.network.fcm.VolleyHelper

class WantitApplication  : Application() {
    companion object{
        lateinit var volleyHelper: VolleyHelper
    }

    override fun onCreate(){
        super.onCreate()

        volleyHelper = VolleyHelper.getInstance(this)
    }
}