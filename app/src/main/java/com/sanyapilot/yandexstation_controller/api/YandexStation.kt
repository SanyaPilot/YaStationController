package com.sanyapilot.yandexstation_controller.api

import android.app.Activity
import com.google.android.material.snackbar.Snackbar
import com.sanyapilot.yandexstation_controller.R
import com.sanyapilot.yandexstation_controller.fragments.DeviceViewModel
import kotlin.concurrent.thread

// Wrapper class
class YandexStation(val context: Activity, val speaker: Speaker, val client: GlagolClient, val viewModel: DeviceViewModel) {
    var localMode = false
    var localTTS = true

    init {
        localMode = mDNSWorker.deviceExists(speaker.quasar_info.device_id)
        if (localMode) {
            startLocal(false)
        } else {
            mDNSWorker.addListener(speaker.quasar_info.device_id) { startLocal(true) }
            viewModel.isLocal.value = false
        }
    }

    fun endLocal() {
        if (localMode) {
            localMode = false
            thread(start = true) { client.stop() }
        }
    }

    private fun showExitSnack() {
        context.runOnUiThread {
            Snackbar.make(
                context.findViewById(R.id.deviceLayout), context.getString(R.string.revertToCloud),
                Snackbar.LENGTH_LONG
            ).show()
            viewModel.isLocal.value = false
        }
    }

    fun startLocal(showSnack: Boolean) {
        thread(start = true) {
            client.start { context.runOnUiThread { viewModel.update(it) } }
            client.setOnSocketClosedListener {
                showExitSnack()
                localMode = false
            }
            client.setOnFailureListener {
                showExitSnack()
                localMode = false
                mDNSWorker.removeDevice(speaker.quasar_info.device_id)
                mDNSWorker.addListener(speaker.quasar_info.device_id) { startLocal(true) }
            }
            if (showSnack) {
                context.runOnUiThread {
                    Snackbar.make(
                        context.findViewById(R.id.deviceLayout),
                        context.getString(R.string.revertToLocal),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
            context.runOnUiThread {
                viewModel.isLocal.value = true
            }
        }
        localMode = true
    }

    fun sendTTS(text: String) {
        // Currently selecting TTS mode is not implemented
        if (localMode && localTTS) {
            client.send(GlagolPayload(command = "sendText", text = "?????????????? ???? ???????? $text"))
        } else {
            QuasarClient.send(speaker, text, true)
        }
    }
    fun sendCommand(text: String) {
        if (localMode) {
            client.send(GlagolPayload(command = "sendText", text = text))
        } else {
            QuasarClient.send(speaker, text, false)
        }
    }
    fun play() {
        if (localMode) {
            client.send(GlagolPayload(command = "play"))
        } else {
            QuasarClient.send(speaker, "????????????????", false)
            viewModel.isPlaying.value = true
        }
    }
    fun pause() {
        if (localMode) {
            client.send(GlagolPayload(command = "stop"))
        } else {
            QuasarClient.send(speaker, "??????????", false)
            viewModel.isPlaying.value = false
        }
    }
    fun nextTrack() {
        if (localMode) {
            client.send(GlagolPayload(command = "next"))
        } else {
            QuasarClient.send(speaker, "?????????????????? ????????", false)
            viewModel.isPlaying.value = true
        }
    }
    fun prevTrack() {
        if (localMode) {
            client.send(GlagolPayload(command = "prev"))
        } else {
            QuasarClient.send(speaker, "???????????????????? ????????", false)
            viewModel.isPlaying.value = true
        }
    }
    fun increaseVolume(value: Float) {
        if (localMode) {
            client.send(GlagolPayload(
                command = "setVolume",
                volume = viewModel.volume.value!! + (value / 100)
            ))
        } else {
            QuasarClient.send(speaker, "???????????? ?????????????????? ???? ${value.toInt()} ??????????????????", false)
        }
    }
    fun decreaseVolume(value: Float) {
        if (localMode) {
            client.send(GlagolPayload(
                command = "setVolume",
                volume = viewModel.volume.value!! - (value / 100)
            ))
        } else {
            QuasarClient.send(speaker, "???????????? ?????????????????? ???? ${value.toInt()} ??????????????????", false)
        }
    }
    fun seek(value: Int) {
        viewModel.seekTime.value = value
        client.send(GlagolPayload(
            command = "rewind",
            position = value
        ))
    }
}