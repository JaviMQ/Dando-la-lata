package com.example.dandolalata

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.entities.Marca
import com.example.dandolalata.databinding.ActivityMainBinding
import com.example.dandolalata.ui.adapters.LatasAdapter
import com.example.dandolalata.utils.AppPaths
import com.example.dandolalata.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var latasAdapter: LatasAdapter
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
        authHelper = GoogleAuthHelper(this)
        val db = AppDatabase.obtenerInstancia(this)
        eliminarImagenesSinLata(db)

        checkPermission()
        configurarUI()

        configurarListenerMenuLateral()
        configurarListenerCrearLata()

        configurarObservadorLatas()
        configurarObservadorMarcas()
    }


    override fun onResume() {
        // Cuando creo una lata y vuelvo al main, debo recargar las latas segun la marca seleccionada
        super.onResume()
        val pos = binding.spinnerMarcas.selectedItemPosition
        val marcaSeleccionada = viewModel.marcas.value?.get(pos)
        viewModel.filtrarLatas(marcaSeleccionada)
    }

    private fun configurarListenerCrearLata(){
        binding.fabAddLata.setOnClickListener {
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

    private fun configurarUI(){
        window.navigationBarColor = ContextCompat.getColor(this, R.color.colorFondoGaleria)


        // Galería con 2 columnas
        binding.recyclerViewLatas.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewLatas.layoutManager = LinearLayoutManager(this)
        latasAdapter = LatasAdapter(
            emptyList(),
            onItemClick = { lataId ->
                val intent = Intent(this, EditarLataActivity::class.java)
                intent.putExtra("lata_id", lataId)
                startActivity(intent)
            },
            onImagenClick = { uri ->
                mostrarImagenAmpliada(uri)
            }
        )

        binding. recyclerViewLatas.adapter = latasAdapter

        // Configurar la Toolbar
        setSupportActionBar(binding.toolbar)

        // Configurar el DrawerLayout
        toggle = ActionBarDrawerToggle(
            this, binding.mainDrawerLayout, binding.toolbar, R.string.abrir_menu, R.string.cerrar_menu
        )
        binding.mainDrawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun configurarListenerMenuLateral(){
        binding.navMenuLateral.setNavigationItemSelectedListener { menuItem ->
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
                                    binding.barraProgreso.visibility = View.VISIBLE
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

                            lifecycleScope.launch {
                                try{
                                    binding.barraProgreso.visibility = View.VISIBLE
                                    binding.progressOverlay.visibility = View.VISIBLE

                                    val resultado = withContext(Dispatchers.IO) {
                                        driveHelper.importarDesdeDrive() { progreso ->
                                            runOnUiThread {
                                                binding.barraProgreso.progress = progreso
                                            }
                                        }
                                    }
                                    if (resultado) {
                                        Toast.makeText(this@MainActivity, "Importación OK", Toast.LENGTH_SHORT).show()
                                        reiniciarApp(this@MainActivity)
                                    } else {
                                        Toast.makeText(this@MainActivity, "Error durante la importación", Toast.LENGTH_SHORT).show()
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
            }
            binding.mainDrawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun configurarObservadorLatas()
    {
        // Observar las latas y actualizar el RecyclerView y el total de latas
        viewModel.latas.observe(this) { latas ->
            val total = latas.size
            binding.textViewTotalLatas.text = getString(R.string.total_latas, total)
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


    private fun mostrarImagenAmpliada(uri: String) {
        Glide.with(this)
            .load(Uri.parse(uri))
            .into(binding.imagenAmpliada)

        binding.overlayImagenAmpliada.visibility = View.VISIBLE

        binding.overlayImagenAmpliada.setOnClickListener {
            binding.overlayImagenAmpliada.visibility = View.GONE
        }
    }

    private fun eliminarImagenesSinLata(db: AppDatabase) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {

                val lataDao = db.lataDao()
                val archivosDir = File(filesDir, AppPaths.IMAGENES_DIR)
                if (!archivosDir.exists()) return@launch

                // Obtener todas las rutas de foto que están en la base de datos
                val latasUsadas = lataDao.obtenerTodas()
                val nombresUsados = latasUsadas.mapNotNull {
                    val path = it.foto
                    path?.let { p -> File(Uri.parse(p).path ?: p).name }
                }.toSet()

                // Lista todos los archivos en la carpeta imagenes
                val archivosLocales = archivosDir.listFiles() ?: return@launch

                var cantidadEliminadas = 0
                for (archivo in archivosLocales) {
                    val nombre = archivo.name
                    if (nombre !in nombresUsados) {
                        Log.e("JAVI", "Eliminando: ${archivo.absolutePath}")
                        // archivo.delete()
                        cantidadEliminadas++
                    }
                }

                withContext(Dispatchers.Main) {
                    if(cantidadEliminadas > 0) {
                        Toast.makeText(
                            this@MainActivity,
                            "$cantidadEliminadas imágenes a eliminar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error al eliminar imágenes: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
