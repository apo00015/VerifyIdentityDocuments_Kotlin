package com.example.priyect.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import aws.sdk.kotlin.runtime.auth.credentials.CredentialsProviderChain
import aws.sdk.kotlin.runtime.auth.credentials.EnvironmentCredentialsProvider
import aws.sdk.kotlin.services.rekognition.RekognitionClient
import aws.sdk.kotlin.services.rekognition.model.CompareFacesMatch
import aws.sdk.kotlin.services.rekognition.model.CompareFacesRequest
import aws.sdk.kotlin.services.rekognition.model.Image
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import com.example.priyect.R
import com.example.priyect.Utils
import com.example.priyect.databinding.ActivityCompareFacesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*
import javax.crypto.Cipher.SECRET_KEY


class Compare_faces : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    private val GALLERY_REQUEST_CODE = 2

    private var _binding: ActivityCompareFacesBinding? = null
    private val binding get() = _binding!!

    // Código de audio entrante
    private val REQUEST_CODE_SPEECH_INPUT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TextToSpeech(Context: this, OnInitListener: this)
        tts = TextToSpeech(this, this)

        _binding = ActivityCompareFacesBinding.inflate(layoutInflater)

        // Agregamos el botón de volver hacia atrás
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Cambiamos el título de la pantalla
        supportActionBar?.title = getString(R.string.comparar_faces_title)

        // Insertamos las caras en las imágenes
        binding.btnComparar.setOnClickListener {
            println("Comenzamos a comparar")
            Utils.contextSelfie.finish()
            Utils.contextBack.finish()
            Utils.contextFront.finish()
            this.finish()
            /*
            // Realizamos la comparación de las caras
            CoroutineScope(Dispatchers.IO).launch {
                compareTwoFaces(78F)
            }

             */

        }

        // Insertamos las imágenes obtenidas antes
        binding.imageViewDetectedDNI.setImageBitmap(Utils.caraFront)
        binding.imageViewDetectedSelfie.setImageBitmap(Utils.selfie)

        setContentView(binding.root)
    }

    /**
     * Método que se llamará cuando se destruya la vista
     */
    public override fun onDestroy() {
        // Shutdown TTS
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

    /**
     * Método para incializar el sintetizador de voz
     */
    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            when (prefs.getString("key_language", "es")) {
                "es" -> {
                    val result = tts!!.setLanguage(Locale("es", "ES"))

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        println("El idioma no está disponible")
                    }
                }
                "fr" -> {
                    val result = tts!!.setLanguage(Locale("fr", "FR"))

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        println("El idioma no está disponible")
                    }
                }
            }
        }
    }

    /**
     * Función para reproducir texto
     *
     * @param texto Texto a reproducir
     */
    fun speakOut(texto: String) {
        if (Utils.sintesisVozEnable)
            tts!!.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    /**
     * Función para vover hacia atrás
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    // snippet-start:[rekognition.kotlin.compare_faces.main]
    suspend fun compareTwoFaces(
        similarityThresholdVal: Float
    ) {

        val stream = ByteArrayOutputStream()
        val stream2 = ByteArrayOutputStream()
        Utils.caraFront.compress(Bitmap.CompressFormat.PNG, 100, stream)
        Utils.selfie.compress(Bitmap.CompressFormat.PNG, 100, stream2)

        println("Streams creados")
        // Create an Image object for the source image.
        val souImage = Image {
            bytes = stream.toByteArray()
        }

        val tarImage = Image {
            bytes = stream2.toByteArray()
        }

        val facesRequest = CompareFacesRequest {
            sourceImage = souImage
            targetImage = tarImage
            similarityThreshold = similarityThresholdVal
        }

        /*
        AWS_ACCESS_KEY_ID = AKIAZM7CZ62WGXWSYLXC
        AWS_SECRET_ACCESS_KEY = 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

         */
        val credentials2 = Credentials("","")
        RekognitionClient {
            region = "us-east-1"
        }.use { rekClient ->
            val compareFacesResult = rekClient.compareFaces(facesRequest)
            val faceDetails = compareFacesResult.faceMatches

            if (faceDetails != null) {
                for (match: CompareFacesMatch in faceDetails) {
                    val face = match.face
                    val position = face?.boundingBox
                    if (position != null)
                        println("Face at ${position.left} ${position.top} matches with ${face.confidence} % confidence.")
                }
            }

            val uncompared = compareFacesResult.unmatchedFaces
            if (uncompared != null)
                println("There was ${uncompared.size} face(s) that did not match")

            println("Source image rotation: ${compareFacesResult.sourceImageOrientationCorrection}")
            println("target image rotation: ${compareFacesResult.targetImageOrientationCorrection}")
        }
    }
}