package com.example.dandolalata

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.entities.Lata
import com.example.dandolalata.data.entities.Marca
import com.example.dandolalata.ui.adapters.LatasAdapter
import com.example.dandolalata.viewmodel.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var spinnerMarcas: Spinner
    private lateinit var recyclerViewLatas: RecyclerView
    private lateinit var latasAdapter: LatasAdapter

    private val viewModel: MainViewModel by viewModels()

    private var todasLasLatas: List<Lata> = listOf()
    private var marcas: List<Marca> = listOf()

    private lateinit var toggle: ActionBarDrawerToggle

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


        // Configurar la Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Configurar el DrawerLayout
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.abrir_menu, R.string.cerrar_menu
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Manejar clics en las opciones del menú
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_crear -> {
                    // Acción para la opción 1
                    Toast.makeText(this, "Seleccionaste Opción 1", Toast.LENGTH_SHORT).show()
                }
                R.id.action_importar -> {
                    // Acción para la opción 2
                    Toast.makeText(this, "Seleccionaste Opción 2", Toast.LENGTH_SHORT).show()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }



        // Observar las marcas
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

        // Observar las latas y actualizar el RecyclerView
        viewModel.latas.observe(this) { latas ->
            latasAdapter.actualizarLista(latas)
        }


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
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_main, menu)  // Inflamos el menú
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_crear -> {
                // Maneja la opción 1
                true
            }
            R.id.action_importar -> {
                // Maneja la opción 2
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
*/
}
/*
class MainActivity : AppCompatActivity() {
    private lateinit var googleDriveHelper: GoogleDriveHelper
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // El usuario ha iniciado sesión correctamente
            handleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(result.data))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        googleDriveHelper = GoogleDriveHelper(this)

        btn_sign_in.setOnClickListener {
            signInToGoogleDrive()
        }

        btn_upload.setOnClickListener {
            uploadDatabaseToDrive()
        }

        btn_download.setOnClickListener {
            downloadDatabaseFromDrive()
        }
    }

    private fun signInToGoogleDrive() {
        val account = googleDriveHelper.getSignedInAccount()
        if (account != null) {
            // Ya está autenticado
            Toast.makeText(this, "Ya has iniciado sesión", Toast.LENGTH_SHORT).show()
        } else {
            // Iniciar flujo de autenticación
            val signInOptions = googleDriveHelper.getSignInOptions()
            val client = GoogleSignIn.getClient(this, signInOptions)
            signInLauncher.launch(client.signInIntent)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            Toast.makeText(this, "Sesión iniciada correctamente", Toast.LENGTH_SHORT).show()
        } catch (e: ApiException) {
            Toast.makeText(this, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadDatabaseToDrive() {
        lifecycleScope.launch {
            val account = googleDriveHelper.getSignedInAccount()
            if (account != null) {
                val dbFile = File(getDatabasePath("your_database_name").absolutePath)
                val fileId = googleDriveHelper.uploadFile(account, dbFile, "application/x-sqlite3")

                if (fileId != null) {
                    Toast.makeText(this@MainActivity,
                        "Base de datos subida correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity,
                        "Error al subir la base de datos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity,
                    "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadDatabaseFromDrive() {
        // Necesitarías guardar el fileId cuando subes el archivo
        val fileId = "ID_DEL_ARCHIVO_EN_DRIVE"

        lifecycleScope.launch {
            val account = googleDriveHelper.getSignedInAccount()
            if (account != null) {
                val dbFile = File(getDatabasePath("your_database_name_restored").absolutePath)
                val success = googleDriveHelper.downloadFile(account, fileId, dbFile)

                if (success) {
                    Toast.makeText(this@MainActivity,
                        "Base de datos descargada correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity,
                        "Error al descargar la base de datos", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity,
                    "Debes iniciar sesión primero", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

suspend fun uploadFolder(account: GoogleSignInAccount, folder: File): String? {
    return withContext(Dispatchers.IO) {
        try {
            val driveClient = getDriveClient(account)
            val driveResourceClient = getDriveResourceClient(account)

            // Crear metadatos de la carpeta
            val folderMetadata = MetadataChangeSet.Builder()
                .setTitle(folder.name)
                .setMimeType(DriveFolder.MIME_TYPE)
                .build()

            // Crear la carpeta
            val createFolderTask = driveResourceClient
                .createFolder(folderMetadata)
                .addOnFailureListener { exception ->
                    throw exception
                }

            val driveFolder = Tasks.await(createFolderTask)
            val folderId = driveFolder.driveId.encodeToString()

            // Subir cada archivo en la carpeta
            folder.listFiles()?.forEach { file ->
                uploadFileToFolder(account, file, folderId)
            }

            folderId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private suspend fun uploadFileToFolder(account: GoogleSignInAccount, file: File, folderId: String) {
    withContext(Dispatchers.IO) {
        try {
            val driveResourceClient = getDriveResourceClient(account)
            val driveId = DriveId.decodeFromString(folderId)

            // Obtener la carpeta
            val folder = driveId.asDriveFolder()

            // Crear metadatos del archivo
            val metadata = MetadataChangeSet.Builder()
                .setTitle(file.name)
                .setMimeType(getMimeType(file))
                .build()

            // Crear contenido del archivo
            val fileContent = FileInputStream(file).use { inputStream ->
                DriveContents.create().apply {
                    outputStream.write(IOUtils.toByteArray(inputStream))
                }
            }

            // Subir el archivo a la carpeta
            Tasks.await(driveResourceClient.createFile(folder, metadata, fileContent, null))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun getMimeType(file: File): String {
    return when (file.extension.toLowerCase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        else -> "application/octet-stream"
    }
}
 */