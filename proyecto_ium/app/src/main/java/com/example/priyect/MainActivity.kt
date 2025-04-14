package com.example.priyect

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.example.priyect.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializamos las preferencias iniciales
        inicializarPreferencias()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        Utils.contextMain = this
    }

    /**
     * Función para cargar las preferncias guardadas de la aplicación
     */
    private fun inicializarPreferencias() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Inicializamos el tema seleccionado
        val tema = prefs.getBoolean("key_theme", false)
        if (tema) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }


        // Inicializamos el idioma seleccionado todo FALLA EN API > 27
        val idiomaGuardado = prefs.getString("key_language", "es")
        // Cambiamos el idioma
        val locale = idiomaGuardado?.let { Locale(it) }
        if (locale != null) {
            Locale.setDefault(locale)
        }
        val config = Configuration()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            applicationContext.createConfigurationContext(config)
        } else {
            config.locale = locale
        }
        resources.updateConfiguration(config, resources.displayMetrics)

        // Inicializamos la sintesis de voz
        val sintesisVoz = prefs.getBoolean("key_sintesisVoz", false)
        Utils.sintesisVozEnable = sintesisVoz

    }

}