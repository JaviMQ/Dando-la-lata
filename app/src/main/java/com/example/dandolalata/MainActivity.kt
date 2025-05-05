package com.example.dandolalata

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dandolalata.data.entities.Marca
import com.example.dandolalata.databinding.ActivityMainBinding
import com.example.dandolalata.ui.adapters.LatasAdapter
import com.example.dandolalata.viewmodel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewLatas: RecyclerView
    private lateinit var latasAdapter: LatasAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var barraProgreso: ProgressBar
    private val viewModel: MainViewModel by viewModels()
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var authHelper: GoogleAuthHelper
    private lateinit var binding: ActivityMainBinding


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Permiso denegado
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            checkPermission()
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        checkPermission()
        inicializarVariables()
        configurarUI()


        configurarListenerMenuLateral()
        configurarListenerCrearLata()

        configurarObservadorLatas()
        configurarObservadorMarcas()
    }

    override fun onResume() {
        // Cuando creo una lata y vuelvo al main, debo recargar las latas segun la marca seleccionada
        super.onResume()
        val marcaSeleccionada = binding.spinnerMarcas.selectedItem as? Marca
        viewModel.filtrarLatas(marcaSeleccionada)
    }


    private fun configurarListenerCrearLata(){
        val fab = findViewById<FloatingActionButton>(R.id.fab_add_lata)
        fab.setOnClickListener {
            val intent = Intent(this, CrearLataActivity::class.java)
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


        recyclerViewLatas = findViewById(R.id.recyclerViewLatas)
        barraProgreso = findViewById(R.id.barra_progreso)

        // Galería con 2 columnas
        recyclerViewLatas.layoutManager = GridLayoutManager(this, 2)
        recyclerViewLatas.layoutManager = LinearLayoutManager(this)
        latasAdapter = LatasAdapter(emptyList()) { lataId ->
            val intent = Intent(this, EditarLataActivity::class.java)
            intent.putExtra("lata_id", lataId)
            startActivity(intent)
        }
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
                        val token  = authHelper.signIn()
                        if (token != null) {
                            // ¡Autenticación exitosa!
                            Toast.makeText(this@MainActivity, "Auth OK", Toast.LENGTH_SHORT).show()

                            val driveHelper = GoogleDriveHelper(this@MainActivity, token)
                            lifecycleScope.launch {
                                try{
                                    barraProgreso.visibility = View.VISIBLE
                                    binding.progressOverlay.visibility = View.VISIBLE

                                    val resultado = withContext(Dispatchers.IO) {
                                        driveHelper.exportarADrive() { progreso ->
                                            runOnUiThread {
                                                binding.barraProgreso.progress = progreso
                                            }
                                        }
                                    }
                                    if (resultado) {
                                        Toast.makeText(this@MainActivity, "Exportación OK", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Error durante la exportación", Toast.LENGTH_SHORT).show()
                                    }

                                } catch (e: Exception) {
                                    Toast.makeText(this@MainActivity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
                                    Log.e("DRIVE", "Error en exportarADrive: ${e.message}", e)
                                } finally {
                                    binding.barraProgreso.visibility = View.GONE
                                    binding.progressOverlay.visibility = View.GONE
                                }

                            }

                        }else{
                            Toast.makeText(this@MainActivity, "Error en autenticación", Toast.LENGTH_SHORT).show()
                        }
                    }

                }
                R.id.action_importar -> {
                    lifecycleScope.launch {
                        val token = authHelper.signIn()
                        if (token != null) {
                            val driveHelper = GoogleDriveHelper(this@MainActivity, token)
                            if( driveHelper.importarDesdeDrive()){
                                Toast.makeText(this@MainActivity, "Importación OK", Toast.LENGTH_SHORT).show()
                                reiniciarApp(this@MainActivity)
                            }

                        }else{
                            Toast.makeText(this@MainActivity, "Error en autenticación", Toast.LENGTH_SHORT).show()
                        }
                    }

                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun configurarObservadorLatas()
    {
        // Observar las latas y actualizar el RecyclerView y el total de latas
        viewModel.latas.observe(this) { latas ->
            val total = latas.size
            findViewById<TextView>(R.id.textViewTotalLatas).text = getString(R.string.total_latas, total)
            latasAdapter.actualizarLista(latas)
        }
    }
    private fun configurarObservadorMarcas()
    {
        viewModel.marcas.observe(this) { marcas ->

            val adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                marcas.map { it.nombre }
            )
            binding.spinnerMarcas.adapter = adapter
            adapter.notifyDataSetChanged()

            binding.spinnerMarcas.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    viewModel.filtrarLatas(marcas[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
    }

    private fun reiniciarApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0) // Finaliza el proceso actual
    }

}
