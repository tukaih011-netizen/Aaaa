package com.example.calls

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import com.example.data.model.CallInfo
import com.example.data.model.PhoneCallStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CallCapabilityReport(
    val hasTelephonyHardware: Boolean,
    val hasReadPhoneStatePermission: Boolean,
    val hasAnswerCallsPermission: Boolean,
    val isDefaultDialer: Boolean,
    val canAnswerDirectly: Boolean,
    val canEndDirectly: Boolean,
    val statusSummary: String
)

class CallCapabilityManager(private val context: Context) {

    fun checkCapability(): CallCapabilityReport {
        val pm = context.packageManager
        val hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val hasAnswerCalls = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val isDefaultDialer = telecomManager?.defaultDialerPackage == context.packageName

        val canAnswer = isDefaultDialer || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && hasAnswerCalls)
        val canEnd = isDefaultDialer || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)

        val summary = when {
            !hasTelephony -> "Device does not support telephony hardware."
            !hasPhoneState -> "READ_PHONE_STATE permission missing."
            !isDefaultDialer -> "TIME PASS is active in Assistant Mode. For full hardware direct-pickup, set as Default Dialer."
            else -> "Full hardware Telecom control active."
        }

        return CallCapabilityReport(
            hasTelephonyHardware = hasTelephony,
            hasReadPhoneStatePermission = hasPhoneState,
            hasAnswerCallsPermission = hasAnswerCalls,
            isDefaultDialer = isDefaultDialer,
            canAnswerDirectly = canAnswer,
            canEndDirectly = canEnd,
            statusSummary = summary
        )
    }

    fun requestDefaultDialerIntent(): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            }
        } else {
            val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
            return intent
        }
        return null
    }
}

object TelecomCallHub {
    private val _callInfo = MutableStateFlow(CallInfo())
    val callInfo: StateFlow<CallInfo> = _callInfo.asStateFlow()

    private var activeCall: Call? = null
    private var inCallServiceInstance: InCallService? = null

    fun registerInCallService(service: InCallService) {
        inCallServiceInstance = service
    }

    fun unregisterInCallService() {
        inCallServiceInstance = null
    }

    fun updateCall(call: Call?) {
        activeCall = call
        if (call == null) {
            _callInfo.value = CallInfo(status = PhoneCallStatus.IDLE)
            return
        }

        val state = when (call.state) {
            Call.STATE_RINGING -> PhoneCallStatus.RINGING
            Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_HOLDING -> PhoneCallStatus.ACTIVE
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> PhoneCallStatus.DISCONNECTED
            else -> PhoneCallStatus.IDLE
        }

        val callerHandle = call.details?.handle?.schemeSpecificPart
        val callerName = call.details?.callerDisplayName ?: callerHandle ?: "Unknown Caller"

        _callInfo.value = _callInfo.value.copy(
            status = state,
            callerName = callerName,
            callerNumber = callerHandle,
            canAnswerDirectly = true,
            canEndDirectly = true
        )
    }

    fun answerCall(context: Context): CallActionResult {
        // 1. Try active InCallService
        activeCall?.let {
            if (it.state == Call.STATE_RINGING) {
                it.answer(0)
                _callInfo.value = _callInfo.value.copy(status = PhoneCallStatus.ACTIVE)
                return CallActionResult.Success("Call answered via Telecom service")
            }
        }

        // 2. Try TelecomManager.acceptRingingCall() (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm && telecomManager != null) {
                try {
                    telecomManager.acceptRingingCall()
                    _callInfo.value = _callInfo.value.copy(status = PhoneCallStatus.ACTIVE)
                    return CallActionResult.Success("Call accepted via TelecomManager")
                } catch (e: SecurityException) {
                    return CallActionResult.NeedsRole("Direct call answer requires default dialer role or ANSWER_PHONE_CALLS permission")
                } catch (e: Exception) {
                    return CallActionResult.Failed("Failed to answer call: ${e.message}")
                }
            }
        }

        return CallActionResult.NeedsRole("Eta direct control korte Android-er required call role lagbe.")
    }

    fun endCall(context: Context): CallActionResult {
        // 1. Try active InCallService
        activeCall?.let {
            it.disconnect()
            _callInfo.value = CallInfo(status = PhoneCallStatus.IDLE)
            return CallActionResult.Success("Call disconnected via Telecom service")
        }

        // 2. Try TelecomManager.endCall() (Android P+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm && telecomManager != null) {
                try {
                    val ended = telecomManager.endCall()
                    _callInfo.value = CallInfo(status = PhoneCallStatus.IDLE)
                    return if (ended) CallActionResult.Success("Call ended successfully")
                    else CallActionResult.Failed("Call could not be terminated")
                } catch (e: SecurityException) {
                    return CallActionResult.NeedsRole("Direct call ending requires Telecom role")
                } catch (e: Exception) {
                    return CallActionResult.Failed("Failed: ${e.message}")
                }
            }
        }

        return CallActionResult.NeedsRole("Eta direct control korte Android-er required call role lagbe.")
    }

    fun toggleSpeaker(context: Context, enable: Boolean): Boolean {
        inCallServiceInstance?.let {
            it.setAudioRoute(if (enable) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
            _callInfo.value = _callInfo.value.copy(isSpeakerOn = enable)
            return true
        }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let {
            it.isSpeakerphoneOn = enable
            _callInfo.value = _callInfo.value.copy(isSpeakerOn = enable)
            return true
        }
        return false
    }

    fun toggleMute(context: Context, mute: Boolean): Boolean {
        inCallServiceInstance?.let {
            it.setMuted(mute)
            _callInfo.value = _callInfo.value.copy(isMuted = mute)
            return true
        }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.let {
            it.isMicrophoneMute = mute
            _callInfo.value = _callInfo.value.copy(isMuted = mute)
            return true
        }
        return false
    }

    fun simulateIncomingCallForTesting(caller: String = "Sabbir Ahmed (+8801712345678)") {
        _callInfo.value = CallInfo(
            status = PhoneCallStatus.RINGING,
            callerName = caller,
            callerNumber = "+8801712345678",
            canAnswerDirectly = true,
            canEndDirectly = true
        )
    }

    fun dismissCallSimulation() {
        _callInfo.value = CallInfo(status = PhoneCallStatus.IDLE)
    }
}

sealed class CallActionResult {
    data class Success(val message: String) : CallActionResult()
    data class NeedsRole(val explanation: String) : CallActionResult()
    data class Failed(val error: String) : CallActionResult()
}
