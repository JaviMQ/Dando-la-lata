package com.example.dandolalata

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.entities.Marca
import com.example.dandolalata.viewmodel.EditarLataViewModel
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class EditarLataActivity : AppCompatActivity() {

    private lateinit var nombreEditText: EditText
    private lateinit var descripcionEditText: EditText
    private lateinit var imageView: ImageView
    private lateinit var botonGuardar: Button
    private lateinit var spinnerMarcas: Spinner
    private var listaMarcas = mutableListOf<Marca>()
    private val viewModel: EditarLataViewModel by viewModels()
    private val db = AppDatabase.obtenerInstancia(this)
    private var lataId by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_lata)

        inicializarVariablesUI()

        pintarLata()

        configurarListeners()
    }

    private fun inicializarVariablesUI(){
        nombreEditText = findViewById(R.id.editTextNombre)
        descripcionEditText = findViewById(R.id.editTextDescripcion)
        imageView = findViewById(R.id.imageView)
        botonGuardar = findViewById(R.id.buttonGuardar)
        spinnerMarcas = findViewById(R.id.spinnerMarcas)
    }

    private fun pintarLata(){
        cargarMarcas()

        lataId = intent.getIntExtra("lata_id", -1)
        if (lataId == -1) {
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            viewModel.obtenerLataPorId(lataId).observe(this) { lata ->
                nombreEditText.setText(lata.nombre)
                descripcionEditText.setText(lata.procedencia)
                Picasso.get().load(Uri.parse(lata.foto)).into(imageView)

                val posicionMarca = listaMarcas.indexOfFirst { it.id == lata.marcaId }
                if (posicionMarca != -1) {
                    spinnerMarcas.setSelection(posicionMarca)
                }
            }
        }
    }

    private fun configurarListeners(){
        botonGuardar.setOnClickListener {
            val nuevoNombre = nombreEditText.text.toString()
            val nuevaDescripcion = descripcionEditText.text.toString()
            val posicionSeleccionada = spinnerMarcas.selectedItemPosition
            val nuevaMarca = listaMarcas[posicionSeleccionada]

            if (lataId != -1) {
                viewModel.actualizarLata(lataId, nuevoNombre, nuevaDescripcion, nuevaMarca.id)
                Toast.makeText(this, "Lata actualizada", Toast.LENGTH_SHORT).show()
                finish() // Volver atrás
            }
        }
    }

    private fun cargarMarcas() {
        lifecycleScope.launch(Dispatchers.IO) {
            val marcas = db.marcaDao().obtenerTodasPorNombre()
            withContext(Dispatchers.Main) {
                listaMarcas.clear()
                listaMarcas.addAll(marcas)
                val nombresMarcas = listaMarcas.map { it.nombre }

                val adapter = ArrayAdapter(
                    this@EditarLataActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    nombresMarcas
                )
                spinnerMarcas.adapter = adapter
            }
        }
    }
}
