package com.example.priyect.analyzerImages

import android.graphics.Bitmap
import android.widget.Toast
import com.example.priyect.R
import com.example.priyect.Utils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class AnalyzerText {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lineasCorrecta = ""
    private var lineasIncorrectas = ""
    private var fechaCaducidadFinal = ""
    private var dniFinal = ""
    private var nacionalidadFinal = ""
    private var fechaNacimientoFinal = ""
    private var sexoFinal = ""
    private var nombreApellidosFinal = ""

    private var camposFallo = false

    fun checkImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        // [START run_detector]
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                //Comprobamos que hay al menos 3 líneas
                var nombreApellidos = ""
                var linea2 = ""
                var dni = ""

                for (i in visionText.textBlocks) {
                    println("La linea actual ha revisar es: ${i.text}")

                    if (i.text.startsWith("ID") and i.text.contains("<")) {
                        dni = i.text
                        i.boundingBox?.let { Utils.contextBack.pintarImagen(it) }
                    }

                    if (i.text.contains("<") && this.getAlfa(i.text)) {
                        nombreApellidos = i.text
                        i.boundingBox?.let { Utils.contextBack.pintarImagen(it) }
                    }

                    if (i.text.contains("<") && this.getNumeric(i.text)) {
                        linea2 = i.text
                        i.boundingBox?.let { Utils.contextBack.pintarImagen(it) }
                    }
                }

                println("------------------------------")
                if (visionText.textBlocks.size >= 3) {

                    println("El dni en blocks > 3 es: $dni")
                    println("La linea 2 en blocks > 3 es: $linea2")
                    println("El nombre y apellidos en blocks > 3 es: $nombreApellidos")

                    // Obtenemos el nombre y apellidos
                    this.validarNombreApellidos(nombreApellidos)

                    // Obtenemos las fechas, sexo y nacionalidad y dni
                    this.validarLinea12(dni, linea2)

                    // Reproducimos los resultados
                    this.speak()
                }

            }
            .addOnFailureListener { e ->
                println(e.toString())
            }
        // [END run_detector]
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

    private fun getAlfa(linea: String): Boolean {
        var numeros = 0
        var letras = 0
        for (i in linea) {
            if (i in '0'..'9') {
                numeros += 1
            } else {
                if (i in 'A'..'Z' || i in 'a'..'z') {
                    letras += 1
                }
            }
        }

        if (letras > numeros)
            return true

        return false
    }

    private fun getNumeric(linea: String): Boolean {
        println("********Linea2: $linea")
        var numeros = 0
        var letras = 0
        for (i in linea) {
            if (i in '0'..'9') {
                numeros += 1
            } else {
                if (i in 'A'..'Z' || i in 'a'..'z') {
                    letras += 1
                }
            }
        }

        if (letras == 4 && numeros > 13)
            return true

        return false
    }

    private fun validarDNIESP(listadni: String) {
        val dni = listadni.substring(listadni.length - 9, listadni.length)

        // Obtenemos los 8 números
        if (dni.length == 9) {
            val numeroString = dni.substring(0, 8)
            val letra: String = "" + dni[8]
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

                if (letra != letraCorrecta) {
                    Toast.makeText(Utils.contextBack, "El dni no es válido", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    println(dni)
                    Utils.dni.setText(dni)
                    dniFinal = dni
                    Utils.dniBack = dniFinal
                }
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    private fun validarDNIFRA(linea1: String) {
        val dniFrances = getAlfanumeric(linea1)
        val dniFrancesDefinitivo =
            dniFrances.substring(dniFrances.length - 10, dniFrances.length - 1)
        Utils.dni.setText(dniFrancesDefinitivo)
        dniFinal = dniFrancesDefinitivo
        Utils.dniBack = dniFinal
    }

    private fun validarNombreApellidos(nombreApellidos: String) {
        val nombreApellidosDefnitivo: ArrayList<String> = ArrayList()

        val listanombreApellidos = nombreApellidos.replace("<", " ").split(" ")
        for (i in listanombreApellidos) {
            val linea = this.getAlfanumeric(i)
            if (linea != "") {
                nombreApellidosDefnitivo.add(i)
            }
        }
        if(nombreApellidosDefnitivo.isEmpty()){
            camposFallo = true
        }
        Utils.nombreApellidosEditTextView.setText(
            nombreApellidosDefnitivo.reversed().joinToString(" ")
        )
        nombreApellidosFinal =  nombreApellidosDefnitivo.reversed().joinToString(" ")

    }

    private fun validarLinea12(linea1: String, linea2: String) {
        // Obtenemos los valores alfanuméricos
        val linea2 = this.getAlfanumeric(linea2)

        // Comprobamos que tiene 25 caracteres
        println("La linea 2 es : $linea2 -> ${linea2.length}")
        if (linea2.length == 19 || linea2.length == 18) {
            val fechaNacimiento = linea2.substring(0, 6)
            fechaNacimientoFinal =
                fechaNacimiento.substring(0, 2) +
                        "/" + fechaNacimiento.substring(2, 4) +
                        "/" + fechaNacimiento.substring(4, 6)
            val fechaCaducidad = linea2.substring(8, 14)
            fechaCaducidadFinal =
                fechaCaducidad.substring(0, 2) +
                        "/" + fechaCaducidad.substring(2, 4) +
                        "/" + fechaCaducidad.substring(4, 6)

            sexoFinal = "" + linea2[7]

            nacionalidadFinal = linea2.substring(15, 18)
            println("La fecha de nacimiento es: $fechaNacimiento")
            println("La fecha de caducidad es: $fechaCaducidad")
            println("El sexo es: $sexoFinal")
            println("La nacionalidad es: $nacionalidadFinal")
            Utils.fechaNacimiento.setText(fechaNacimientoFinal)
            Utils.fechaCaducidad.setText(fechaCaducidadFinal)
            Utils.sexoEditTextView.setText(sexoFinal)
            Utils.nacionalidadEditTextView.setText(nacionalidadFinal)

            when (nacionalidadFinal) {
                "ESP" -> {
                    val listadni = this.getAlfanumeric(linea1)
                    println("La lista del dni definitiva es: $listadni -> ${listadni.length}")

                    if (listadni.length == 24) {
                        this.validarDNIESP(listadni)
                    }else{
                        Utils.dni.error = Utils.contextBack.getString(R.string.error_back_dni)
                        this.lineasIncorrectas += Utils.contextBack.getString(R.string.error_back_dni)
                        camposFallo = true
                    }
                }
                "FRA" -> this.validarDNIFRA(linea1)

                else -> camposFallo = true
            }

        }else{
            camposFallo = true
        }
    }

    private fun speak(){
        if(!camposFallo){
            lineasCorrecta += "${Utils.contextBack.getString(R.string.speak_nombreApelllidos_back)} $nombreApellidosFinal"


            val fechaNacimientoSeparada = fechaNacimientoFinal.split("/")
            lineasCorrecta += "${Utils.contextBack.getString(R.string.speak_fechaNacimiento_back)} ${fechaNacimientoSeparada[2]} "

            when(fechaNacimientoSeparada[1]){
                "01" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_enero)} "
                "02" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_febrero)} "
                "03" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_marzo)} "
                "04" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_abril)} "
                "05" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_mayo)} "
                "06" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_junio)} "
                "07" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_julio)} "
                "08" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_agosto)} "
                "09" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_septiembre)} "
                "10" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_octubre)} "
                "11" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_noviembre)} "
                "12" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_diciembre)} "
            }

            lineasCorrecta += "${fechaNacimientoSeparada[0]} \n"

            val fechaCaducidadSeparada = fechaCaducidadFinal.split("/")
            lineasCorrecta += "${Utils.contextBack.getString(R.string.speak_fechaCaducidad_back)} ${fechaCaducidadSeparada[2]} "

            when(fechaCaducidadSeparada[1]){
                "01" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_enero)} "
                "02" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_febrero)} "
                "03" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_marzo)} "
                "04" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_abril)} "
                "05" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_mayo)} "
                "06" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_junio)} "
                "07" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_julio)} "
                "08" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_agosto)} "
                "09" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_septiembre)} "
                "10" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_octubre)} "
                "11" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_noviembre)} "
                "12" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.text_diciembre)} "
            }

            lineasCorrecta += "${fechaCaducidadSeparada[0]} \n"

            lineasCorrecta += "${Utils.contextBack.getString(R.string.speak_front_dni)} $dniFinal \n"
            when(sexoFinal){
                "M" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.speak_sexo_back)} ${Utils.contextBack.getString(R.string.text_masculino)} \n"
                "F" -> lineasCorrecta += "${Utils.contextBack.getString(R.string.speak_sexo_back)} ${Utils.contextBack.getString(R.string.text_femenino)} \n"
            }

            when(nacionalidadFinal){
                "ESP" ->  lineasCorrecta += "${Utils.contextBack.getString(R.string.speak_nacionalidad_back)} ${Utils.contextBack.getString(R.string.text_nacionalidad_ESP)}"
                "FRA" ->  lineasCorrecta += "${Utils.contextBack.getString(R.string.speak_nacionalidad_back)} ${Utils.contextBack.getString(R.string.text_nacionalidad_FRA)}"
            }

            Utils.contextBack.speakOut(lineasCorrecta)

        }else{
            if(lineasIncorrectas == ""){
                Utils.contextBack.speakOut(Utils.contextBack.getString(R.string.error_back_campos_no_rellenos))
            }else{
                Utils.contextBack.speakOut(lineasIncorrectas)
            }

        }
        this.lineasCorrecta = ""
        this.lineasIncorrectas = ""
        camposFallo = false
    }
}