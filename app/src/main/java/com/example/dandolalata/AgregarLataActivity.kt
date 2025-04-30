package com.example.dandolalata


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.entities.Lata
import com.example.dandolalata.utils.AppPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

const val REQUEST_CODE_NUEVA_MARCA = 1


class AgregarLataActivity : AppCompatActivity() {

    // Variables para los componentes de la UI
    private lateinit var editTextNombre: EditText
    private lateinit var editTextDescripcion: EditText
    private lateinit var imageView: ImageView
    private lateinit var buttonGuardar: Button
    private lateinit var buttonNuevaMarca: Button
    private lateinit var spinnerMarcas: Spinner
    private var rutaFoto: String? = null
    private var idMarcaSeleccionada: Int = 1 // Este valor debe provenir de la selección de la marca (por ejemplo, Spinner)
    private val db = AppDatabase.obtenerInstancia(this)
    private var listaMarcas = mutableListOf<String>()

    // Llamada al launcher para tomar la foto
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                // Si la toma de la foto fue exitosa
                rutaFoto?.let {
                    // Muestra la imagen en el ImageView
                    imageView.setImageURI(Uri.parse(it)) // Mostrar la imagen en el ImageView

                    // En este punto, la foto ya se ha tomado y está almacenada en el archivo
                }
            } else {
                // Si hubo un error al tomar la foto
                Toast.makeText(this, "Error al tomar la foto", Toast.LENGTH_SHORT).show()
            }
        }

    private val nuevaMarcaLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val nuevaMarca = result.data?.getStringExtra("NUEVA_MARCA")
                nuevaMarca?.let {
                    cargarMarcas(it) // Recargar el Spinner
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_lata)

        // Inicializa los componentes de la UI
        inicializarVariablesUI()

        // Cargar las marcas en el Spinner
        cargarMarcas()

        // Configurar el Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listaMarcas)
        spinnerMarcas.adapter = adapter

        // Botón para tomar una foto
        imageView.setOnClickListener {
            val photoFile = crearArchivoFoto()
            val uri = FileProvider.getUriForFile(
                this,
                "com.example.dandolalata.fileprovider",  // Asegúrate de que el nombre del proveedor coincide con el de tu AndroidManifest.xml
                photoFile
            )
            // Llama al launcher para capturar la foto
            rutaFoto = uri.toString()
            takePictureLauncher.launch(uri)
        }

        // Botón para guardar la lata
        buttonGuardar.setOnClickListener {
            val nombre = editTextNombre.text.toString()
            val descripcion = editTextDescripcion.text.toString()
            val posicionSeleccionada = spinnerMarcas.selectedItemPosition

            if (nombre.isNotEmpty() && descripcion.isNotEmpty() && rutaFoto != null && posicionSeleccionada > 0) {
                // Crea la nueva lata
                val nuevaLata = Lata(
                    marcaId = idMarcaSeleccionada,
                    procedencia = descripcion,  // Asumiendo que "procedencia" es la descripción
                    nombre = nombre,
                    foto = rutaFoto!!  // Ruta de la foto
                )

                // Guarda la lata en la base de datos usando coroutines
                // TODO: GlobalScope
                lifecycleScope.launch {
                    try {
                        // Inserta la lata en la base de datos
                        db.lataDao().insertar(nuevaLata)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AgregarLataActivity, "Lata guardada", Toast.LENGTH_SHORT).show()
                            finish() // Cierra la actividad después de guardar
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@AgregarLataActivity, "Error al guardar la lata - ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.d("javi", "Error al guardar la lata - ${e.message}")
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        buttonNuevaMarca.setOnClickListener {
            val intent = Intent(this, AgregarMarcaActivity::class.java)
            nuevaMarcaLauncher.launch(intent)
        }
    }

    // Crea un archivo para almacenar la foto capturada
    private fun crearArchivoFoto(): File {
        // Directorio para guardar las fotos
        val directorio = File(filesDir,  AppPaths.IMAGENES_DIR) // Carpeta interna "imagenes"
        if (!directorio.exists()) {
            directorio.mkdirs() // Crea la carpeta si no existe
        }

        return File.createTempFile("lata_", ".jpg", directorio)

    }

    private fun cargarMarcas(nombreMarca: String? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            val marcas = db.marcaDao().obtenerTodasPorNombre()
            withContext(Dispatchers.Main) {
                listaMarcas.clear()
                listaMarcas.add("Elige una marca") // Opción inicial
                listaMarcas.addAll(marcas.map { it.nombre })

                val adapter = ArrayAdapter(
                    this@AgregarLataActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    listaMarcas
                )
                spinnerMarcas.adapter = adapter

                nombreMarca?.let {
                    val position = adapter.getPosition(it)
                    spinnerMarcas.setSelection(position)
                }

            }
        }
    }

    private fun inicializarVariablesUI(){
        editTextNombre = findViewById(R.id.editTextNombre)
        editTextDescripcion = findViewById(R.id.editTextDescripcion)
        imageView = findViewById(R.id.imageView)
        buttonGuardar = findViewById(R.id.buttonGuardar)
        buttonNuevaMarca = findViewById(R.id.buttonNuevaMarca)
        spinnerMarcas = findViewById(R.id.spinnerMarcas)
    }

}
