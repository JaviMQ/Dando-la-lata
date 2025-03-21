package com.example.dandolalata

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.entities.Lata
import com.example.dandolalata.data.entities.Marca
import com.example.dandolalata.ui.adapters.LatasAdapter
import com.example.dandolalata.viewmodel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var spinnerMarcas: Spinner
    private lateinit var recyclerViewLatas: RecyclerView
    private lateinit var latasAdapter: LatasAdapter

    private val viewModel: MainViewModel by viewModels()

    private var todasLasLatas: List<Lata> = listOf()
    private var marcas: List<Marca> = listOf()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido: puedes acceder a las imágenes
            // loadImages()
        } else {
            // Permiso denegado
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            checkPermission()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.navigationBarColor = ContextCompat.getColor(this, R.color.colorFondoGaleria)

        checkPermission()

        spinnerMarcas = findViewById(R.id.spinnerMarcas)
        recyclerViewLatas = findViewById(R.id.recyclerViewLatas)

       // Galería con 2 columnas
       recyclerViewLatas.layoutManager = GridLayoutManager(this, 2)



        recyclerViewLatas.layoutManager = LinearLayoutManager(this)
        latasAdapter = LatasAdapter(emptyList())
        recyclerViewLatas.adapter = latasAdapter

        // Observar las marcas
        viewModel.marcas.observe(this) { marcas ->

            val adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                marcas.map { it.nombre }
            )
            spinnerMarcas.adapter = adapter
            // spinnerMarcas.dropDownVerticalOffset = spinnerMarcas.height + 30

            adapter.notifyDataSetChanged()

            spinnerMarcas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    viewModel.filtrarLatas(marcas[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        // Observar las latas y actualizar el RecyclerView
        viewModel.latas.observe(this) { latas ->
            latasAdapter.actualizarLista(latas)
        }

       // Cargar marcas y latas desde la base de datos
        /*
       loadLatas()
       loadMarcas()
        */
       val fab = findViewById<FloatingActionButton>(R.id.fab_add_lata)
       fab.setOnClickListener {
           val intent = Intent(this, AgregarLataActivity::class.java)
           startActivity(intent)
       }

    }

    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                // ✅ Permiso concedido: ya puedes usar la cámara
            }
            else -> {
                // 🚀 Pedir el permiso al usuario
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
/*
    private fun loadMarcas() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.obtenerInstancia(applicationContext)
            val marcasFromDb = db.marcaDao().obtenerTodas()

            // Agregar la opción "Todas" al inicio
            val listaConTodas = listOf(Marca(id = 0, nombre = "Todas las marcas 1")) + marcasFromDb

            withContext(Dispatchers.Main) {
                marcas = listaConTodas

                // Configurar el Spinner con las marcas
                val adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    marcas.map { it.nombre }
                )
                spinnerMarcas.adapter = adapter

                // Manejar selección en el Spinner
                spinnerMarcas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        filtrarLatas(marcas[position])
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        filtrarLatas(null)
                    }
                }
            }
        }
    }

    private fun loadLatas() {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.obtenerInstancia(applicationContext)
            val latasFromDb = db.lataDao().obtenerTodas()

            withContext(Dispatchers.Main) {
                todasLasLatas = latasFromDb
                latasAdapter = LatasAdapter(todasLasLatas)
                    recyclerViewLatas.adapter = latasAdapter
            }
        }
    }
*/
    private fun filtrarLatas(marca: Marca?) {
        val latasFiltradas = if (marca == null || marca.id == 0) {
            todasLasLatas // Mostrar todas
        } else {
            todasLasLatas.filter { it.marcaId == marca.id }
        }

        latasAdapter.actualizarLista(latasFiltradas)
    }
}


