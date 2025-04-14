package com.example.priyect.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import coil.load
import com.example.priyect.R
import com.example.priyect.Utils
import com.example.priyect.analyzerImages.AnalyzerText
import com.example.priyect.databinding.ActivityImageBackBinding
import com.google.mlkit.vision.common.InputImage
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import java.util.*
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.graphics.toRectF
import androidx.preference.PreferenceManager
import com.example.priyect.BuildConfig
import java.io.File
import java.io.OutputStream

class ImageBack : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null

    private val GALLERY_REQUEST_CODE = 2

    private var _binding: ActivityImageBackBinding? = null
    private val binding get() = _binding!!

    lateinit var bitmapBack: Bitmap
    // Código de audio entrante
    private val REQUEST_CODE_SPEECH_INPUT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TextToSpeech(Context: this, OnInitListener: this)
        tts = TextToSpeech(this, this)

        _binding = ActivityImageBackBinding.inflate(layoutInflater)

        // Agregamos el botón de volver hacia atrás
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Cambiamos el título de la pantalla
        supportActionBar?.title = getString(R.string.title_cara_trasera)

        // Inicializamos el botón de la cámara
        binding.btnCamera.setOnClickListener {
            cameraCheckPermission()
        }

        // Inicializamos el botón de la galeria
        binding.btnGallery.setOnClickListener {
            galleryCheckPermission()
        }

        // Inicializamos el botón al hacer click en la imágen
        binding.imageView.setOnClickListener {
            val pictureDialog = this.let { it1 -> AlertDialog.Builder(it1) }
            pictureDialog.setTitle(getString(R.string.select_action))
            val pictureDialogItem = arrayOf(
                getString(R.string.select_galeria),
                getString(R.string.select_camera)
            )
            pictureDialog.setItems(pictureDialogItem) { _, which ->
                when (which) {
                    0 -> gallery()
                    1 -> camera()
                }
            }

            pictureDialog.show()
        }

        // Inicializamos el botón de terminar
        binding.buttonTerminar.setOnClickListener {
            if (revisarCampos()) {

                if(Utils.dniFront == Utils.dniBack){
                    if(Utils.sintesisVozEnable){
                        Utils.contextBack.speakOut(getString(R.string.speak_terminar))

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
                        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_terminar))

                        // on below line we are specifying a try catch block.
                        // in this block we are calling a start activity
                        // for result method and passing our result code.
                        try {
                            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
                        } catch (e: Exception) {
                            println("Error")
                        }
                    }else{
                    // Nos movemos a la pantalla de selfie
                        val selfieIntent = Intent(this, Selfie::class.java)
                        startActivity(selfieIntent)
                    }

                }else{
                    speakOut(getString(R.string.speak_dni_no_iguales))
                    Utils.mostrarAlert(getString(R.string.error_dni_no_iguales_title),getString(R.string.speak_dni_no_iguales),this)
                }
                
            } else {
                speakOut(getString(R.string.speak_error_terminar))
                Utils.mostrarAlert(getString(R.string.error_back_campos_no_rellenos),getString(R.string.speak_error_terminar),this)
            }
        }

        Utils.nombreApellidosEditTextView = binding.editTextNombreApellidos
        Utils.dni = binding.editTextDni
        Utils.fechaCaducidad = binding.editTextFechaCaducidad
        Utils.fechaNacimiento = binding.editTextFechaNacimiento
        Utils.sexoEditTextView = binding.editTextSexo
        Utils.nacionalidadEditTextView = binding.editTextNacionalidad
        Utils.contextBack = this

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
            when(prefs.getString("key_language", "es")){
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

    fun pintarImagen(rect: Rect){
        // Pintamos el recuadro de la cara
        val paint = Paint()
        paint.strokeWidth = 6f
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE

        val tempBitmap = Bitmap.createBitmap(
            bitmapBack.width,
            bitmapBack.height,
            Bitmap.Config.RGB_565
        )
        val canvas = Canvas(tempBitmap)
        canvas.drawBitmap(bitmapBack, 0F, 0F, null)

        canvas.drawRoundRect(rect.toRectF(), 2F, 2F, paint)

        binding.imageView.setImageDrawable(BitmapDrawable(resources,tempBitmap))
        this.bitmapBack = tempBitmap
    }
    /**
     * Función para solicitar los permisos de acceso a la galería
     */
    private fun galleryCheckPermission() {
        Dexter.withContext(this).withPermission(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ).withListener(object : PermissionListener {
            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                gallery()
            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                Toast.makeText(
                    applicationContext,
                    "You have denied the storage permission to select image",
                    Toast.LENGTH_SHORT
                ).show()
                showRotationalDialogForPermission()
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: PermissionRequest?, p1: PermissionToken?
            ) {
                showRotationalDialogForPermission()
            }
        }).onSameThread().check()
    }

    /**
     * Método para abrir la galería del dispositivo móvil
     */
    private fun gallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    /**
     * Función para solicitar los permisos de utilización de la cámara
     */
    private fun cameraCheckPermission() {

        Dexter.withContext(this)
            .withPermissions(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
            ).withListener(

                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {

                            if (report.areAllPermissionsGranted()) {
                                camera()
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        showRotationalDialogForPermission()
                    }

                }
            ).onSameThread().check()
    }

    /**
     * Método para abrir la cámara del dispositivo móvil
     */
    private fun camera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {
            it.resolveActivity(packageManager).also { _ ->
                createPhotoFile()
                val photoUri: Uri =
                    FileProvider.getUriForFile(
                        this,
                        BuildConfig.APPLICATION_ID + ".fileprovider", file
                    )
                it.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }
        }
        openCamera.launch(intent)
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
                        if (Objects.requireNonNull(res)[0].lowercase() == "sí" || Objects.requireNonNull(res)[0].lowercase() == "si") {
                            // Nos movemos a comparar faces
                            val selfieIntent = Intent(this, Selfie::class.java)
                            startActivity(selfieIntent)
                        } else {
                            Utils.contextBack.speakOut(getString(R.string.speak_back_noEnviar))
                        }
                    }
                    "fr" ->{
                        if (Objects.requireNonNull(res)[0].lowercase() == "oui") {
                            // Nos movemos a comparar faces
                            val selfieIntent = Intent(this, Selfie::class.java)
                            startActivity(selfieIntent)
                        } else {
                            Utils.contextBack.speakOut(getString(R.string.speak_back_noEnviar))
                        }
                    }
                }

            }
        } else {
            if (resultCode == Activity.RESULT_OK) {
                when (requestCode) {
                    GALLERY_REQUEST_CODE -> {
                        limpiarCampos()
                        binding.imageView.load(data?.data) {
                            val image: InputImage
                            try {
                                image = data?.data?.let {
                                    InputImage.fromFilePath(applicationContext, it)
                                }!!
                                val analyzerText = AnalyzerText()
                                image.bitmapInternal?.let {
                                    analyzerText.checkImage(it)
                                    bitmapBack = it
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showRotationalDialogForPermission() {
        this.let {
            AlertDialog.Builder(it)
                .setMessage(
                    "It looks like you have turned off permissions"
                            + "required for this feature. It can be enable under App settings!!!"
                )

                .setPositiveButton("Go TO SETTINGS") { _, _ ->

                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", "Package.getPackage().name", null)
                        intent.data = uri
                        startActivity(intent)

                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }

                .setNegativeButton("CANCEL") { dialog, _ ->
                    dialog.dismiss()
                }.show()
        }
    }

    /**
     * Función para comprobar si los campos de la pantalla son correctos
     *
     * @return True si son correctos, False si alguno de ellos está vacío
     */
    private fun revisarCampos(): Boolean{
        if(binding.editTextDni.text.toString() == "")
            return false
        if(binding.editTextNombreApellidos.text.toString() == "")
            return false
        if(binding.editTextNacionalidad.text.toString() == "")
            return false
        if(binding.editTextSexo.text.toString() == "")
            return false
        if(binding.editTextFechaCaducidad.text.toString() == "")
            return false
        if(binding.editTextFechaNacimiento.text.toString() == "")
            return false

        return true
    }

    /**
     * Función para limpiar los campos de la pantalla
     */
    private fun limpiarCampos() {
        Utils.nombreApellidosEditTextView.setText("")
        Utils.dni.setText("")
        Utils.fechaCaducidad.setText("")
        Utils.fechaNacimiento.setText("")
        Utils.sexoEditTextView.setText("")
        Utils.nacionalidadEditTextView.setText("")
        Utils.dni.error = null
    }

    //--------------------------------------------------------------------
    /**
     * Atributo para instanciar la cámara
     */
    private val openCamera =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                limpiarCampos()
                val bitmap = getBitmap()
                // Analizamos la imagen tomada desde la cámara
                binding.imageView.setImageBitmap(bitmap)
                val analyzerText = AnalyzerText()
                analyzerText.checkImage(bitmap)
                this.bitmapBack = bitmap
            }
        }

    /**
     * Atributo del fichero que contiene la imágen tomada
     */
    private lateinit var file: File

    /**
     * Función para crear un fichero de la imagen tomada
     */
    private fun createPhotoFile() {
        val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        file = File.createTempFile("IMG_${System.currentTimeMillis()}_", ".jpg", dir)
    }

    /**
     * Función para guardar en la galería la imágen
     */
    private fun saveToGallery() {
        val content = createContent()
        val uri = save(content)
        clearContents(content, uri)
    }

    /**
     * Función auxiliar para guardar en la galería la imagen
     */
    private fun createContent(): ContentValues {
        val fileName = file.name
        val fileType = "image/jpg"
        return ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, fileType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    private fun save(content: ContentValues): Uri {
        var outputStream: OutputStream?
        var uri: Uri?
        application.contentResolver.also { resolver ->
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, content)
            outputStream = resolver.openOutputStream(uri!!)
        }
        outputStream.use { output ->
            getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, output)
        }
        return uri!!
    }

    private fun clearContents(content: ContentValues, uri: Uri) {
        content.clear()
        content.put(MediaStore.MediaColumns.IS_PENDING,0)
        contentResolver.update(uri,content,null,null)
    }

    /**
     * Función para obtener el bitmap de la imágen
     */
    private fun getBitmap(): Bitmap {
        return BitmapFactory.decodeFile(file.toString())
    }
}