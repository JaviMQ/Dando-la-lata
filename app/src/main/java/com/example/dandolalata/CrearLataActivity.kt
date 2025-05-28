package com.example.dandolalata


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.entities.Lata
import com.example.dandolalata.data.entities.Marca
import com.example.dandolalata.utils.AppPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class CrearLataActivity : AppCompatActivity() {

    // Variables para los componentes de la UI
    private lateinit var editTextNombre: EditText
    private lateinit var editTextDescripcion: EditText
    private lateinit var imageView: ImageView
    private lateinit var buttonGuardar: Button
    private lateinit var buttonNuevaMarca: Button
    private lateinit var spinnerMarcas: Spinner
    private var rutaFoto: String? = null
    private val db = AppDatabase.obtenerInstancia(this)
    private var listaMarcas = mutableListOf<Marca>()

    // Llamada al launcher para tomar la foto
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess) {
                // Si la toma de la foto fue exitosa
                rutaFoto?.let {
                    val uriOriginal = Uri.parse(it)
                    val uriComprimida = comprimirImagen(uriOriginal)

                    uriComprimida?.let { comprimida ->
                        imageView.setImageURI(comprimida)
                        rutaFoto = comprimida.toString() // Actualiza la ruta con la versión comprimida
                    }

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
        setContentView(R.layout.activity_crear_lata)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.colorFondoGaleria)

        // Inicializa los componentes de la UI
        inicializarVariablesUI()

        // Cargar las marcas en el Spinner
        cargarMarcas()

        configurarListeners()
    }

    // Crea un archivo para almacenar la foto capturada
    private fun crearArchivoFoto(): File {
        // Directorio para guardar las fotos
        val directorio = File(filesDir,  AppPaths.IMAGENES_DIR) // Carpeta interna "imagenes"
        if (!directorio.exists()) {
            directorio.mkdirs()
        }

        return File.createTempFile("lata_", ".jpg", directorio)

    }

    private fun cargarMarcas(nombreMarca: String? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            val marcas = db.marcaDao().obtenerTodasPorNombre()
            withContext(Dispatchers.Main) {
                listaMarcas.clear()
                listaMarcas.add(Marca(0, "Elige una marca")) // Opción inicial
                listaMarcas.addAll(marcas)
                val nombresMarcas = listaMarcas.map { it.nombre }

                val adapter = ArrayAdapter(
                    this@CrearLataActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    nombresMarcas
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

    private fun configurarListeners(){
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
            val marcaSeleccionada = listaMarcas.getOrNull(posicionSeleccionada)


            if (nombre.isNotEmpty() && rutaFoto != null && marcaSeleccionada != null && marcaSeleccionada.id != 0) {
                // Crea la nueva lata
                val nuevaLata = Lata(
                    marcaId = marcaSeleccionada.id,
                    procedencia = descripcion,
                    nombre = nombre,
                    foto = rutaFoto!!
                )

                // Guarda la lata en la base de datos usando coroutines
                lifecycleScope.launch {
                    try {
                        db.lataDao().insertar(nuevaLata)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CrearLataActivity, "Lata guardada", Toast.LENGTH_SHORT).show()
                            finish() // Cierra la actividad después de guardar
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CrearLataActivity, "Error al guardar la lata - ${e.message}", Toast.LENGTH_SHORT).show()
                            //Log.d("javi", "Error al guardar la lata - ${e.message}")
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        buttonNuevaMarca.setOnClickListener {
            val intent = Intent(this, CrearMarcaActivity::class.java)
            nuevaMarcaLauncher.launch(intent)
        }
    }

    private fun comprimirImagen(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val byteArray = reducirBitmapAMax1MB(bitmap)

            // Sobrescribir el archivo original
            val archivoOriginal = File(Uri.parse(rutaFoto).path!!)
            val outputStream = FileOutputStream(archivoOriginal)
            outputStream.write(byteArray)
            outputStream.close()

            Uri.fromFile(archivoOriginal)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun reducirBitmapAMax1MB(bitmap: Bitmap): ByteArray {
        var calidad = 100
        val maxSize = 1024 * 1024 // 1 MB
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, stream)

        while (stream.size() > maxSize && calidad > 10) {
            calidad -= 5
            stream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, stream)
        }

        return stream.toByteArray()
    }


}
