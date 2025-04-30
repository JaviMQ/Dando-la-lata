package com.example.dandolalata

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.entities.Marca
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AgregarMarcaActivity : AppCompatActivity() {

    private lateinit var editTextNombreMarca: EditText
    private lateinit var buttonGuardarMarca: Button
    private val db = AppDatabase.obtenerInstancia(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_marca)

        editTextNombreMarca = findViewById(R.id.editTextNombreMarca)
        buttonGuardarMarca = findViewById(R.id.buttonGuardarMarca)
        editTextNombreMarca.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editTextNombreMarca, InputMethodManager.SHOW_IMPLICIT)

        buttonGuardarMarca.setOnClickListener {
            guardarMarca()
        }
    }

    private fun guardarMarca() {
        val nombreMarca = editTextNombreMarca.text.toString().trim()

        if (nombreMarca.isEmpty()) {
            Toast.makeText(this, "Ingrese un nombre de marca", Toast.LENGTH_SHORT).show()
            return
        }

        // Guardar en la base de datos
        val nuevaMarca = Marca(nombre = nombreMarca) // No necesitas ID, Room lo genera

        CoroutineScope(Dispatchers.IO).launch {
            val idGenerado = db.marcaDao().insertar(nuevaMarca) // Room devuelve el ID autogenerado
            nuevaMarca.id = idGenerado.toInt() // Guardamos el ID en el objeto

            withContext(Dispatchers.Main) {
                val intent = Intent().apply {
                    putExtra("NUEVA_MARCA", nombreMarca)
                }
                setResult(RESULT_OK, intent)
                finish() // Cierra la actividad y vuelve a AgregarLataActivity
            }
        }
    }
}
