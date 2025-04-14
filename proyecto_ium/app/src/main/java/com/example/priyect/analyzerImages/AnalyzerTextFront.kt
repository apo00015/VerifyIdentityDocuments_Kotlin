package com.example.priyect.analyzerImages

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.core.graphics.toRectF
import com.example.priyect.R
import com.example.priyect.Utils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class AnalyzerTextFront {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var dni: String = ""

    private var pais = "'"
    fun checkImage(bitmap: Bitmap) {
        this.dni = ""
        Utils.boundingBoxDni = null
        val image = InputImage.fromBitmap(bitmap, 0)

        // [START run_detector]
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Comprobamos la cara que se está analizando
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        println(line.text)
                        when (line.text) {
                            "RÉPUBLIQUE FRANÇAISE" -> {
                                pais = "RÉPUBLIQUE FRANÇAISE"
                            }
                            "REINO DE ESPANA" -> {
                                pais = "REINO DE ESPANA"
                            }
                        }
                    }
                }

                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        for (element in line.elements) {
                            println(element.text)
                            val palabra = getAlfanumeric(element.text)
                            this.validarDNI(palabra, element)
                        }
                    }
                }
                // Comprobamos si se ha obtenido el dni
                if (this.dni != "") {
                    Utils.dniTextViewFront.visibility = View.VISIBLE
                    Utils.dniEditTextFront.visibility = View.VISIBLE
                    Utils.dniEditTextFront.setText(this.dni)

                    Utils.contextFront.speakOut("${Utils.contextFront.getString(R.string.speak_front_dni)} ${this.dni}")
                } else {
                    Utils.dniTextViewFront.visibility = View.INVISIBLE
                    Utils.dniEditTextFront.visibility = View.INVISIBLE
                    Utils.dniEditTextFront.setText("")
                }
                Utils.contextFront.pintarImagen()
            }
            .addOnFailureListener { e ->
                println(e.message)
            }
    }

    private fun getAlfanumeric(linea: String): String {
        var fraseFinal = ""
        for (i in linea) {
            if (i in 'A'..'Z' || i in 'a'..'z' || i in '0'..'9') {
                fraseFinal += i
            }
        }

        return fraseFinal
    }

    private fun validarDNI(listadni: String, element: Text.Element) {

        // Obtenemos los 8 números
        if (listadni.length == 9) {
            when (pais) {
                "RÉPUBLIQUE FRANÇAISE" -> {
                    var nNumeros = 0
                    var nLetras = 0

                    for (i in listadni) {
                        if (i in 'A'..'Z' || i in 'a'..'z') {
                            nLetras += 1
                        }
                        if (i in '0'..'9') {
                            nNumeros += 1
                        }
                    }
                    if (nLetras > 2 && nNumeros > 1) {
                        this.dni = listadni
                        Utils.dniFront = listadni
                        Utils.boundingBoxDni = element.boundingBox!!
                    }
                }
                else-> {
                    val numeroString = listadni.substring(0, 8)
                    println("El número es: $numeroString")
                    val letra: String = "" + listadni[8]
                    println("La letra es: $letra")
                    try {
                        val numero = numeroString.toInt()
                        val modulo = numero % 23
                        var letraCorrecta = ""
                        when (modulo) {
                            0 -> letraCorrecta = "T"
                            1 -> letraCorrecta = "R"
                            2 -> letraCorrecta = "W"
                            3 -> letraCorrecta = "A"
                            4 -> letraCorrecta = "G"
                            5 -> letraCorrecta = "M"
                            6 -> letraCorrecta = "Y"
                            7 -> letraCorrecta = "F"
                            8 -> letraCorrecta = "P"
                            9 -> letraCorrecta = "D"
                            10 -> letraCorrecta = "X"
                            11 -> letraCorrecta = "B"
                            12 -> letraCorrecta = "N"
                            13 -> letraCorrecta = "J"
                            14 -> letraCorrecta = "Z"
                            15 -> letraCorrecta = "S"
                            16 -> letraCorrecta = "Q"
                            17 -> letraCorrecta = "V"
                            18 -> letraCorrecta = "H"
                            19 -> letraCorrecta = "L"
                            20 -> letraCorrecta = "C"
                            21 -> letraCorrecta = "K"
                            22 -> letraCorrecta = "E"
                        }

                        if (letra == letraCorrecta) {
                            this.dni = listadni
                            Utils.dniFront = listadni
                            println("EL Bounding es: ${element.boundingBox}")
                            Utils.boundingBoxDni = element.boundingBox!!

                        }else{
                            Utils.contextFront.speakOut(Utils.contextFront.getString(R.string.error_back_dni))
                        }

                    } catch (e: Exception) {
                        println(e.message)
                    }
                }
            }
        }

    }
}
