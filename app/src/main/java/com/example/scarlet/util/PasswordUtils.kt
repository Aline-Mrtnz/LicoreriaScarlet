package com.example.scarlet.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Hashea y verifica PINs con PBKDF2 + salt aleatorio por cuenta. */
object PasswordUtils {
    private const val ITERACIONES = 10_000
    private const val LARGO_LLAVE = 256
    private const val ALGORITMO = "PBKDF2WithHmacSHA1"

    fun hash(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derivar(pin, salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    /** true si el pin coincide. Soporta cuentas viejas sin migrar (texto plano, sin ":"). */
    fun verificar(pin: String, guardado: String): Boolean {
        if (!guardado.contains(":")) return pin == guardado // compatibilidad temporal
        val (saltB64, hashB64) = guardado.split(":", limit = 2)
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val hashGuardado = Base64.decode(hashB64, Base64.NO_WRAP)
        val hashIngresado = derivar(pin, salt)
        return hashGuardado.contentEquals(hashIngresado)
    }

    private fun derivar(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERACIONES, LARGO_LLAVE)
        return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded
    }
}