package com.example.priyect

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.SparseIntArray
import android.view.Surface
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.priyect.ui.ImageBack
import com.example.priyect.ui.ImageFront
import com.example.priyect.ui.Selfie

class Utils {
    companion object{

        lateinit var contextFront : ImageFront
        lateinit var contextBack : ImageBack
        lateinit var contextMain: MainActivity
        lateinit var contextSelfie: Selfie

        // Image Back
        lateinit var fechaNacimiento: EditText
        lateinit var fechaCaducidad: EditText
        lateinit var dni: EditText
        lateinit var nombreApellidosEditTextView: EditText
        lateinit var sexoEditTextView: EditText
        lateinit var nacionalidadEditTextView: EditText

        // Image Front
        lateinit var dniTextViewFront: TextView
        lateinit var dniEditTextFront: EditText
        var boundingBoxDni: Rect? = null
        lateinit var imageFrontCompleta: ImageView
        // Sintesis de voz
        var sintesisVozEnable: Boolean = false

        // DNI obtenido en FRONT y BACK
        lateinit var dniFront: String
        lateinit var dniBack : String
        private val ORIENTATIONS = SparseIntArray()

        // Caras para Compare_face
        lateinit var caraFront : Bitmap
        lateinit var selfie : Bitmap

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 0)
            ORIENTATIONS.append(Surface.ROTATION_90, 90)
            ORIENTATIONS.append(Surface.ROTATION_180, 180)
            ORIENTATIONS.append(Surface.ROTATION_270, 270)
        }

        fun mostrarAlert(titulo: String, contenido: String, context: Context){
            // Mostramos un mensaje de alerta
            val alert = AlertDialog.Builder(context).apply {
                setTitle(titulo)
                setMessage(contenido)
                setPositiveButton(contextFront.getString(R.string.aceptar)) { _, _ -> }
            }
            // Mostramos la ventana
            alert.show()
        }

        /**
         * Get the angle by which an image must be rotated given the device's current
         * orientation.
         */
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Throws(CameraAccessException::class)
        fun getRotationCompensation(
            cameraId: String,
            activity: Activity,
            isFrontFacing: Boolean
        ): Int {
            // Get the device's current rotation relative to its "native" orientation.
            // Then, from the ORIENTATIONS table, look up the angle the image must be
            // rotated to compensate for the device's rotation.
            val deviceRotation = activity.windowManager.defaultDisplay.rotation
            var rotationCompensation = ORIENTATIONS.get(deviceRotation)

            // Get the device's sensor orientation.
            val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION)!!

            rotationCompensation = if (isFrontFacing) {
                (sensorOrientation + rotationCompensation) % 360
            } else { // back-facing
                (sensorOrientation - rotationCompensation + 360) % 360
            }
            return rotationCompensation
        }

        fun getCameraId(context: Context, facing: Int): String {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            return manager.cameraIdList.first {
                manager
                    .getCameraCharacteristics(it)
                    .get(CameraCharacteristics.LENS_FACING) == facing
            }
        }
    }
}