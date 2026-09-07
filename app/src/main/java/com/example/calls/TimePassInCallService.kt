package com.example.calls

import android.telecom.Call
import android.telecom.InCallService

class TimePassInCallService : InCallService() {

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call?, state: Int) {
            super.onStateChanged(call, state)
            TelecomCallHub.updateCall(call)
        }

        override fun onDetailsChanged(call: Call?, details: Call.Details?) {
            super.onDetailsChanged(call, details)
            TelecomCallHub.updateCall(call)
        }
    }

    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)
        TelecomCallHub.registerInCallService(this)
        call?.registerCallback(callCallback)
        TelecomCallHub.updateCall(call)
    }

    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        call?.unregisterCallback(callCallback)
        TelecomCallHub.updateCall(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        TelecomCallHub.unregisterInCallService()
    }
}
