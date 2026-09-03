package com.example.scarlet

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream


class QR : AppCompatActivity() {

    private lateinit var imgQR: ImageView

    // Código para abrir la galería
    private val PICK_IMAGE_QR = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_qr)

        // ============================================
        // REFERENCIAS
        // ============================================

        imgQR = findViewById(R.id.imgQR)

        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        val btnSeleccionarQR =
            findViewById<Button>(R.id.btnSeleccionarQR)

        val btnConfirmarPagoQR =
            findViewById<Button>(R.id.btnConfirmarPagoQR)

        val txtMonto =
            findViewById<TextView>(R.id.txtMontoQR)


        // ============================================
        // EDGE TO EDGE
        // ============================================

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // ============================================
        // BOTÓN VOLVER
        // ============================================

        btnBack.setOnClickListener {
            finish()
        }

        // ============================================
        // MOSTRAR TOTAL
        // ============================================

        val total =
            intent.getStringExtra("total")

        if (total != null) {
            txtMonto.text = total
        }

        // ============================================
        // CARGAR QR GUARDADO
        // ============================================

        cargarQRGuardado()

        // ============================================
        // SELECCIONAR QR DESDE GALERÍA
        // ============================================

        btnSeleccionarQR.setOnClickListener {

            val intent =
                Intent(Intent.ACTION_OPEN_DOCUMENT)

            intent.type = "image/*"

            intent.addCategory(
                Intent.CATEGORY_OPENABLE
            )

            startActivityForResult(
                intent,
                PICK_IMAGE_QR
            )
        }

        // ============================================
        // CONFIRMAR PAGO
        // ============================================

        btnConfirmarPagoQR.setOnClickListener {

            Toast.makeText(
                this,
                "Pago QR confirmado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================
    // RESULTADO DE LA GALERÍA
    // ============================================

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == PICK_IMAGE_QR &&
            resultCode == RESULT_OK
        ) {

            val uri: Uri? = data?.data

            if (uri != null) {

                guardarQR(uri)

                Toast.makeText(
                    this,
                    "Código QR guardado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ============================================
    // GUARDAR QR EN EL TELÉFONO
    // ============================================

    private fun guardarQR(uri: Uri) {

        try {

            val inputStream =
                contentResolver.openInputStream(uri)

            if (inputStream == null) {
                Toast.makeText(
                    this,
                    "No se pudo abrir la imagen",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val archivoQR =
                File(
                    filesDir,
                    "qr_pago.jpg"
                )

            val outputStream =
                FileOutputStream(archivoQR)

            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            cargarQRGuardado()

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error al guardar el QR",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================
    // CARGAR QR GUARDADO
    // ============================================

    private fun cargarQRGuardado() {

        try {

            val archivoQR =
                File(
                    filesDir,
                    "qr_pago.jpg"
                )

            if (archivoQR.exists()) {

                val bitmap =
                    BitmapFactory.decodeFile(
                        archivoQR.absolutePath
                    )

                imgQR.setImageBitmap(bitmap)

            } else {

                // Todavía no se seleccionó ningún QR
                imgQR.setImageDrawable(null)
            }

        } catch (e: Exception) {

            e.printStackTrace()
        }
    }
}