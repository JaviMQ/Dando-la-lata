package com.example.dandolalata

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dandolalata.data.database.DatabaseConfig
import com.example.dandolalata.ui.adapters.LatasAdapter
import com.example.dandolalata.viewmodel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerMarcas: Spinner
    private lateinit var recyclerViewLatas: RecyclerView
    private lateinit var latasAdapter: LatasAdapter
    private lateinit var drawerLayout: DrawerLayout
    private val viewModel: MainViewModel by viewModels()
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var authHelper: GoogleAuthHelper

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


        checkPermission()
        inicializarVariables()
        configurarUI()


        configurarListenerMenuLateral()

        configurarObservadorLatas()
        configurarObservadorMarcas()



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

    private fun inicializarVariables(){
        authHelper = GoogleAuthHelper(this)
    }

    private fun configurarUI(){
        window.navigationBarColor = ContextCompat.getColor(this, R.color.colorFondoGaleria)

        spinnerMarcas = findViewById(R.id.spinnerMarcas)
        recyclerViewLatas = findViewById(R.id.recyclerViewLatas)

        // Galería con 2 columnas
        recyclerViewLatas.layoutManager = GridLayoutManager(this, 2)
        recyclerViewLatas.layoutManager = LinearLayoutManager(this)
        latasAdapter = LatasAdapter(emptyList())
        recyclerViewLatas.adapter = latasAdapter

        // Configurar la Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configurar el DrawerLayout
        drawerLayout = findViewById(R.id.mainDrawerLayout)
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.abrir_menu, R.string.cerrar_menu
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun configurarListenerMenuLateral(){
        // Manejar clics en las opciones del menú
        val navigationView: NavigationView = findViewById(R.id.nav_menu_lateral)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_crear_backup -> {
                    lifecycleScope.launch {

                        val result = authHelper.signIn(this@MainActivity)
                        result?.let { (email, token) ->
                            // ¡Autenticación exitosa!
                            Toast.makeText(this@MainActivity, "Auth OK", Toast.LENGTH_SHORT).show()

                            val dbPath = this@MainActivity.getDatabasePath(DatabaseConfig.DATABASE_NAME).absolutePath


                            // startExportToDrive(email, token)
                        } ?: run {
                            Toast.makeText(this@MainActivity, "Error en autenticación", Toast.LENGTH_SHORT).show()
                        }
                    }
                    // Acción para la opción 1

                }
                R.id.action_importar -> {
                    // Acción para la opción 2
                    Toast.makeText(this@MainActivity, "Seleccionaste Opción 2", Toast.LENGTH_SHORT).show()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun configurarObservadorLatas()
    {
        // Observar las latas y actualizar el RecyclerView
        viewModel.latas.observe(this) { latas ->
            latasAdapter.actualizarLista(latas)
        }
    }
    private fun configurarObservadorMarcas()
    {
        viewModel.marcas.observe(this) { marcas ->

            val adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                marcas.map { it.nombre }
            )
            spinnerMarcas.adapter = adapter
            adapter.notifyDataSetChanged()

            spinnerMarcas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    viewModel.filtrarLatas(marcas[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

}
/*
private fun getMimeType(file: File): String {
    return when (file.extension.toLowerCase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        else -> "application/octet-stream"
    }
}
 */