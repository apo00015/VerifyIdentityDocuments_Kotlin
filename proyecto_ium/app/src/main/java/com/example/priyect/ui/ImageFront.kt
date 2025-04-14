package com.example.priyect.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Settings.Global
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.toRectF
import androidx.preference.PreferenceManager
import coil.load
import com.example.priyect.BuildConfig
import com.example.priyect.R
import com.example.priyect.Utils
import com.example.priyect.analyzerImages.AnalyzerTextFront
import com.example.priyect.databinding.ActivityImageFrontBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList


class ImageFront : AppCompatActivity(),  TextToSpeech.OnInitListener{

    private var tts: TextToSpeech? = null

    private val GALLERY_REQUEST_CODE = 2

    private var rostroDetectado = false

    private var _binding: ActivityImageFrontBinding? = null
    private val binding get() = _binding!!

    var faces = ArrayList<Face>()
    private lateinit var bitmap: Bitmap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TextToSpeech(Context: this, OnInitListener: this)
        tts = TextToSpeech(this, this)

        _binding = ActivityImageFrontBinding.inflate(layoutInflater)

        // Agregamos el botón de volver hacia atrás
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Cambiamos el título de la pantalla
        supportActionBar?.title = getString(R.string.title_cara_frontal)

        // Inicializamos el botón de la cámara
        binding.btnCamera.setOnClickListener {
            cameraCheckPermission()
        }

        // Inicializamos el botón de la galeria
        binding.btnGallery.setOnClickListener {
            galleryCheckPermission()
        }

        //Inicializamos al tcocar la imagen
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

        // Inicializamos el botón de validación
        binding.buttonValidar.setOnClickListener {
            if(rostroDetectado){
                if(binding.editTextDniDetectado.text.toString() != ""){
                    val imageBack = Intent(this, ImageBack::class.java)
                    startActivity(imageBack)
                }else{
                    speakOut(getString(R.string.error_speak_front_dni))
                    Utils.mostrarAlert(getString(R.string.error_front_title),getString(R.string.error_front_content),this)
                }

            }else{
                speakOut(getString(R.string.error_speak_front))
                Utils.mostrarAlert(getString(R.string.error_front_title),getString(R.string.error_front_content),this)
            }
        }

        Utils.dniTextViewFront = binding.textViewDniDetectado
        Utils.dniEditTextFront = binding.editTextDniDetectado

        Utils.contextFront = this

        setContentView(binding.root)
    }

    /**
     * Función para volver hacia atrás
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return super.onSupportNavigateUp()
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
     * Función para abrir la galería
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
     * Función para abrir la camára del dispositivo móvil
     */
    private fun camera() {
        //val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
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
        //openCamera.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        //startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    binding.imageView.load(data?.data) {
                        val image: InputImage
                        try {
                            image = data?.data?.let {
                                InputImage.fromFilePath(applicationContext, it)
                            }!!

                            image.bitmapInternal?.let {
                                checkFaces(it)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
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
     * Función para comprobar las caras de la imagen
     */
    private fun checkFaces(bitmap: Bitmap) {
        this.bitmap = bitmap
        val mediaImage: InputImage = InputImage.fromBitmap(bitmap,0)

        val detector = FaceDetection.getClient()
        detector.process(mediaImage)
            .addOnSuccessListener { faces ->
                if(faces.size != 1){
                    binding.textViewCaraDetectada.visibility = View.GONE
                    binding.imageViewDetected.setImageDrawable(null)
                    binding.imageViewDetected.visibility = View.GONE
                    rostroDetectado = false
                    Utils.dniTextViewFront.visibility = View.GONE
                    Utils.dniEditTextFront.visibility = View.GONE
                    Utils.dniEditTextFront.setText("")

                }else {
                    binding.textViewCaraDetectada.visibility = View.VISIBLE
                    getFace(bitmap,faces[0])
                    this.faces = faces as ArrayList<Face>

                    rostroDetectado = true

                    // Analizamos el texto de la imagen
                    val analyzerText = AnalyzerTextFront()
                    analyzerText.checkImage(bitmap)

                }
            }
            .addOnFailureListener { e ->
                println(e.toString())
            }
    }

    /**
     * Método para pintar una imagen
     */
    fun pintarImagen(){
        // Pintamos el recuadro de la cara
        if(this.faces.size > 0){
            println("Vamos a pintar")
            val paint = Paint()
            paint.strokeWidth = 6f
            paint.color = Color.RED
            paint.style = Paint.Style.STROKE

            val tempBitmap = Bitmap.createBitmap(
                bitmap.width,
                bitmap.height,
                Bitmap.Config.RGB_565
            )
            val canvas = Canvas(tempBitmap)
            canvas.drawBitmap(bitmap, 0F, 0F, null)

            val x1 = faces[0].boundingBox.exactCenterX() - (faces[0].boundingBox.width() / 2)
            val y1 = faces[0].boundingBox.exactCenterY() - (faces[0].boundingBox.height() / 2)
            val x2 = x1 + faces[0].boundingBox.width()
            val y2 = y1 + faces[0].boundingBox.height()
            canvas.drawRoundRect(RectF(x1, y1, x2, y2), 2F, 2F, paint)

            paint.strokeWidth = 4f
            Utils.boundingBoxDni?.let { canvas.drawRoundRect(it.toRectF(), 2F, 2F, paint) }
            binding.imageView.setImageDrawable(BitmapDrawable(resources,tempBitmap))
        }

    }

    /**
     * Función para obtener la cara detectada
     */
    private fun getFace(bitmap: Bitmap , face: Face){
        val rect = face.boundingBox

        val x = Math.max(rect.left, 0)
        val y = Math.max(rect.top,0)

        val width = rect.width()
        val height = rect.height()

        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            x,
            y,
            if(x + width > bitmap.width) bitmap.width - x else width,
            if(y + height > bitmap.height) bitmap.height - x else height
        )

        Utils.caraFront = croppedBitmap
        binding.imageViewDetected.setImageBitmap(croppedBitmap)
        binding.imageViewDetected.visibility = View.VISIBLE
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
     * Función para reproducir audio
     *
     * @param texto Texto a reproducir
     */
    fun speakOut(texto: String) {
        if(Utils.sintesisVozEnable)
            tts!!.speak(texto, TextToSpeech.QUEUE_FLUSH, null,"")
    }

    //----------------------------------------------------------------

    /**
     * Atributo para instanciar la cámara
     */
    private val openCamera =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val bitmap = getBitmap()
                //binding.imageView.setImageBitmap(bitmap)
                checkFaces(bitmap)
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