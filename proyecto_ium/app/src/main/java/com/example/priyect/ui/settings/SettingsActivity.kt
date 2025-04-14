package com.example.priyect.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.example.priyect.R
import com.example.priyect.SplashScreen
import com.example.priyect.Utils
import com.example.priyect.ui.chatbot.Chatbot2
import java.util.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Clase interna que representa un Fragment para la configuración
     */
    class SettingsFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        /**
         * Método para crear las preferencias
         */
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        /**
         * Método que se llama cuando la actividad se reinicia
         */
        override fun onResume() {
            // Registramos el listener
            preferenceScreen.sharedPreferences
                ?.registerOnSharedPreferenceChangeListener(this)
            super.onResume()
        }

        /**
         * Método que se llama cuando la actividad finaliza
         */
        override fun onPause() {
            super.onPause()
            // Unregister the listener whenever a key changes
            preferenceScreen.sharedPreferences
                ?.unregisterOnSharedPreferenceChangeListener(this)
        }

        /**
         * Función para detectar un clik en una prefernecia
         *
         * @param preference Preferencia que se ha seleccionado
         */
        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "key_acercaDe" -> {
                    // Mostramos el pop up de acerca de
                    val builder = AlertDialog.Builder(requireContext())
                    val view = layoutInflater.inflate(R.layout.pop_up_acerca_de, null)

                    // Pasamos el pop-up
                    builder.setView(view)

                    // Creamos el dialogo
                    val popUp = builder.create()
                    popUp.show()
                }
                "key_chatBot" -> {
                    // Abrimos el chatbot
                    val chatBotIntent = Intent(requireContext(), Chatbot2::class.java)
                    startActivity(chatBotIntent)
                }
            }
            return super.onPreferenceTreeClick(preference)
        }

        /**
         * Función para detectar que cambios se han producido sobre las SharedPreferences
         *
         * @param p0 SharedPreferences de la aplicación
         * @param p1 Clave de la preferencia que se ha modificado
         */
        override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
            // Comprobamos que clave a cambiado
            when (p1) {
                "key_language" -> {
                    val preferencia = findPreference<Preference>(p1)
                    if (preferencia != null) {
                        val valor = p0?.getString(p1, "es")

                        // Cambiamos el idioma
                        val locale = valor?.let { Locale(it) }
                        if (locale != null) {
                            Locale.setDefault(locale)
                        }
                        // Cambiamos la configuración
                        val config = Configuration()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            config.setLocale(locale)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                config.setLayoutDirection(locale)
                            }
                            requireContext().createConfigurationContext(config)
                        } else {
                            config.locale = locale
                        }
                        requireContext().resources?.updateConfiguration(config, resources.displayMetrics)
                        val intent = Intent(requireContext(),SplashScreen::class.java)
                        Utils.contextMain.finish()
                        startActivity(intent)

                    }
                }

                "key_theme" -> {
                    // Comprobamos si está activado el switch
                    val preferencia = findPreference<SwitchPreferenceCompat>(p1)
                    if (preferencia != null) {
                        if (preferencia.isChecked) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        }

                        // Cambiamos el idioma al español
                        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        with(prefs.edit()) {
                            putString("key_language", "es")
                            apply()
                        }

                        val intent = Intent(requireContext(),SplashScreen::class.java)
                        Utils.contextMain.finish()
                        startActivity(intent)
                    }
                }

                "key_sintesisVoz" -> {
                    // Comprobamos si está activado el switch del sintesis de voz
                    val preferencia = findPreference<SwitchPreferenceCompat>(p1)
                    if (preferencia != null) {
                        Utils.sintesisVozEnable = preferencia.isChecked
                    }
                }

            }
        }
    }
}