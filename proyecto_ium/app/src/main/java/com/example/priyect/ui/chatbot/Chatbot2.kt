package com.example.priyect.ui.chatbot

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.priyect.R
import com.example.priyect.Utils
import com.example.priyect.domain.api.ApiRest
import com.example.priyect.databinding.ActivityChatbot2Binding
import com.example.priyect.domain.api.BodyCognigy
import com.example.priyect.ui.chatbot.data.Message
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class Chatbot2 : AppCompatActivity(), TextToSpeech.OnInitListener {
    private val TAG = "Chatbot"

    private var tts: TextToSpeech? = null

    companion object {
        const val SEND_ID = "SEND_ID"
        lateinit var id : String
        const val RECEIVE_ID = "RECEIVE_ID"
        const val URL_COGNIGY =
            "https://endpoint-trial.cognigy.ai/da213bcdbf43fe921073a3acb515d3b9a43a0769213fd7526631103651f901d9/"
        const val SESSION_ID = "0001"
        const val USER_ID = "0001"
    }

    private var audio = true

    private var _binding: ActivityChatbot2Binding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MessagingAdapter

    // Código de audio entrante
    private val REQUEST_CODE_SPEECH_INPUT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        id = UUID.randomUUID().toString()
        println("EL UUID es: ${id}")
        // TextToSpeech(Context: this, OnInitListener: this)
        tts = TextToSpeech(this, this)

        _binding = ActivityChatbot2Binding.inflate(layoutInflater)

        // Agregamos el botón de volver hacia atrás
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Cambiamos el título de la pantalla
        supportActionBar?.title = getString(R.string.tittle_activity_chatbot)

        recyclerView()

        // Inicializamos el botón de escribir un mensaje
        binding.editTextMessage.setOnClickListener {
            //Scroll back to correct position when user clicks on text view
            GlobalScope.launch {
                delay(100)

                withContext(Dispatchers.Main) {
                    binding.recyclerViewMessages.scrollToPosition(adapter.itemCount - 1)
                }
            }
        }

        // Añadimos el listener al editText del contenido del mensaje
        binding.editTextMessage.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                println(s)
                // Si hay texto cambiamos el icono a enviar
                if(s.toString().isNotEmpty()){
                    binding.btnFloatSend.setImageDrawable(getDrawable(R.drawable.ic_baseline_send_24))
                    audio = false
                }else{
                    binding.btnFloatSend.setImageDrawable(getDrawable(R.drawable.ic_baseline_mic_24))
                    audio = true
                }
            }
        })

        // Inicializamos el botón de enviar el mensaje o hablar
        binding.btnFloatSend.setOnClickListener {
            if(!audio){
                CoroutineScope(Dispatchers.IO).launch {
                    sendMessage()
                }
            }else{
                Toast.makeText(this,"Manten presionado para grabar, suelta para enviar",Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnFloatSend.setOnLongClickListener {
            if(audio){
                println("HABLAAAAAR")
                //checkAudioPermission()
                // changing the color of mic icon, which
                // indicates that it is currently listening
                //micIV.setColorFilter(ContextCompat.getColor(this, R.color.mic_enabled_color)) // #FF0E87E7
                //startSpeechToText()
                while(tts!!.isSpeaking){
                    continue
                }

                // Inicializamos la ventana de reconocimiento de audio
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

                // on below line we are passing language model
                // and model free form in our intent
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                // on below line we are passing our
                // language as a default language.
                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                val idiomaGuardado = prefs.getString("key_language", "es")
                intent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    idiomaGuardado
                )

                // on below line we are specifying a prompt
                // message as speak to text on below line.
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Texto a enviar")

                // on below line we are specifying a try catch block.
                // in this block we are calling a start activity
                // for result method and passing our result code.
                try {
                    startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
                } catch (e: Exception) {
                    println("Error")
                }
            }

            true
        }

        setContentView(binding.root)
    }

    /**
     * Método que se llamará al destruir la vista
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
     * Función para vover hacia atrás
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
    }

    /**
     * Método para iniciar el sintetizador de voz
     */
    override fun onInit(p0: Int) {
        if (p0 == TextToSpeech.SUCCESS) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            when(prefs.getString("key_language", "es")){
                "es" -> {
                    val result = tts!!.setLanguage(Locale("es", "ES"))

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        println("Idioma no soportado")
                    }
                }
                "fr" -> {
                    val result = tts!!.setLanguage(Locale("fr", "FR"))

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        println("Idioma no soportado")
                    }
                }
            }
        }
    }

    /**
     * Método para crear una instancia de acceso a la API
     *
     * @return Instancia de Retrofit
     */
    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(URL_COGNIGY)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun recyclerView() {
        this.adapter = MessagingAdapter()
        binding.recyclerViewMessages.adapter = adapter
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(applicationContext)

    }

    override fun onStart() {
        super.onStart()
        //In case there are messages, scroll to bottom when re-opening app
        GlobalScope.launch {
            delay(100)
            withContext(Dispatchers.Main) {
                binding.recyclerViewMessages.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    private suspend fun sendMessage(mensaje: String? = null) {

        val timeStamp = this.timeStamp()
        if(mensaje != null){
            adapter.insertMessage(Message(mensaje, SEND_ID, timeStamp))
            binding.recyclerViewMessages.scrollToPosition(adapter.itemCount - 1)

            botResponse(mensaje)
        }else{
            val message = binding.editTextMessage.text.toString()

            if (message.isNotEmpty()) {
                //Adds it to our local list
                binding.editTextMessage.setText("")

                adapter.insertMessage(Message(message, SEND_ID, timeStamp))
                binding.recyclerViewMessages.scrollToPosition(adapter.itemCount - 1)

                botResponse(message)
            }
        }

    }

    private suspend fun botResponse(message: String) {
        val timeStamp = this.timeStamp()

        withContext(Dispatchers.Main) {
            //Gets the response
            try {
                val body = BodyCognigy(SESSION_ID, id, message)
                val call = getRetrofit().create(ApiRest::class.java).postResponse(body)
                if (call.isSuccessful) {
                    println("Llamada exitosa")
                    val response = call.body()
                    if (response != null) {
                        //Inserts our message into the adapter
                        println("El mensaje de cognigy es ${response.text}")
                        println("El outputStack es: ${response.outputStack}")
                        // Insertamos todos los mensajes enviados
                        for (mensaje in response.outputStack){
                            Message(mensaje.text, RECEIVE_ID, timeStamp)
                                .let { adapter.insertMessage(it) }
                            //Scrolls us to the position of the latest message
                            binding.recyclerViewMessages.scrollToPosition(adapter.itemCount - 1)
                        }

                        speakOut(response.text)
                    }else{
                        println("FALLLO")
                    }
                }else{
                    println("FALLLO")
                }
            } catch (e: Exception) {
                e.message?.let { Log.d(TAG, it) }
            }
        }
    }

    /**
     * Función para obtener la hora actual
     *
     * @return Hora actual en formato (HH:mm)
     */
    fun timeStamp(): String {

        val timeStamp = Timestamp(System.currentTimeMillis())
        val sdf = SimpleDateFormat("HH:mm")
        val time = sdf.format(Date(timeStamp.time))

        return time.toString()
    }

    /**
     * Función para reproducir audio
     *
     * @param texto Texto a reproducir
     */
    fun speakOut(texto: String) {
        if(Utils.sintesisVozEnable)
            tts!!.speak(texto, TextToSpeech.QUEUE_FLUSH, null,"")
    }

    private fun checkAudioPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // M = 23
            if(ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != PackageManager.PERMISSION_GRANTED) {
                // this will open settings which asks for permission
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.example.priyect.ui.chatbot"))
                startActivity(intent)
                Toast.makeText(this, "Allow Microphone Permission", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {

                val res: ArrayList<String> =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                when(prefs.getString("key_language", "es")){
                    "es" -> {
                        println(Objects.requireNonNull(res)[0])
                        CoroutineScope(Dispatchers.IO).launch {
                            sendMessage(Objects.requireNonNull(res)[0])
                        }

                    }
                }
            }
        }
    }

}