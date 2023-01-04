package com.stc.flashlight

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.Camera.getCameraInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.TorchCallback
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.GONE
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.util.LogWriter
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private var mMaxStrength: Int = 1
    val cameraManager by lazy { getSystemService(CAMERA_SERVICE) as CameraManager }
    val cameraId by lazy { cameraManager.cameraIdList[0] }
    var mTorchEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!doesDeviceHaveFlash()) {
            setContentView(View(this))
            showDialog(
                getString(R.string.dialog_not_supported_title),
                getString(R.string.dialog_not_supported_message),
                android.R.drawable.ic_dialog_alert,
                { exitProcess(0) },
                getString(R.string.dialog_not_supported_button),
                false
            )
            return
        }
        var currentStrength = 0
        cameraManager.registerTorchCallback(torchCallback, null)
        imageFlash.setOnClickListener {
            cameraManager.setTorchMode(cameraId, !mTorchEnabled)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            showSimpleVersion()
        } else {
            showExtendedVersion()
            currentStrength = cameraManager.getTorchStrengthLevel(cameraId)
        }
        textStatus.text = formatStatus(mTorchEnabled, currentStrength)
    }

    private fun showSimpleVersion() {
        seekBar.visibility = GONE
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showExtendedVersion() {
        mMaxStrength = cameraManager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL)!!
        seekBar.max = mMaxStrength
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress : Int, fromUser: Boolean) {
                if(progress == 0){
                    cameraManager.setTorchMode(cameraManager.cameraIdList[0], false)
                }else {
                    cameraManager.turnOnTorchWithStrengthLevel(
                        cameraManager.cameraIdList[0],
                        progress
                    )

                }
                textStatus.text = formatStatus(mTorchEnabled, progress)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }

        })
    }

    private fun doesDeviceHaveFlash(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }


    var torchCallback: TorchCallback = object : TorchCallback(){
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            Log.d(TAG, "onTorchModeChanged() called with: cameraId = $cameraId, enabled = $enabled")
            mTorchEnabled = enabled
            if(enabled) {
                imageFlash.setImageResource(R.drawable.flashlight_on)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    seekBar.progress = cameraManager.getTorchStrengthLevel(cameraId)
                }
            }
            else {
                imageFlash.setImageResource(R.drawable.flashlight_off)
                seekBar.progress = 0
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            super.onTorchModeUnavailable(cameraId)
        }

        override fun onTorchStrengthLevelChanged(cameraId: String, newStrengthLevel: Int) {
            Log.d(
                TAG,
                "onTorchStrengthLevelChanged() called with: cameraId = $cameraId, newStrengthLevel = $newStrengthLevel"
            )
            //seekBar.progress = newStrengthLevel
            textStatus.text = formatStatus(mTorchEnabled, newStrengthLevel)
        }
    }

    private fun formatStatus(enabled: Boolean, strengthLevel: Int) : String {
        return if(strengthLevel == 0 || !enabled) "Off"
        else {
            val percentage = strengthLevel * 100 / mMaxStrength
            "On $percentage%"
        }
    }
}