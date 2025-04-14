package com.example.priyect

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler

class SplashScreen : AppCompatActivity() {

    /**
     * Método que se llama al inicializar la actividad para inicializar sus atributos
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // La instrucción de debajo nos permite ocultar la action bar
        supportActionBar?.hide()


        /**
         * Si el usuarios no ha iniciado sesión se envía a la página de registro, en caso contrario a la página incial
         */
        Handler().postDelayed({
            val dashboardIntent = Intent(this, MainActivity::class.java)
            startActivity(dashboardIntent)
            finish()

        }, 2000)
    }
}