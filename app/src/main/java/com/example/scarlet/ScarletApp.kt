package com.example.scarlet

import android.app.Application
import com.example.scarlet.util.Session

/**
 * Application class de Scarlet.
 *
 * Su único propósito hoy es darle a Session un Context de aplicación desde
 * el arranque mismo del proceso, para que la sesión (usuario logueado)
 * pueda restaurarse desde SharedPreferences si Android mata el proceso en
 * segundo plano (por ejemplo, mientras está abierto el diálogo de
 * impresión/PDF de un comprobante) y luego lo vuelve a crear.
 *
 * Debe estar declarada en AndroidManifest.xml con:
 *   <application android:name=".ScarletApp" ...>
 */
class ScarletApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Session.inicializar(this)
    }
}