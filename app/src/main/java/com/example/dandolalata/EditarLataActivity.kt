package com.example.dandolalata

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.dandolalata.viewmodel.EditarLataViewModel
import com.squareup.picasso.Picasso

class EditarLataActivity : AppCompatActivity() {

    private lateinit var nombreEditText: EditText
    private lateinit var descripcionEditText: EditText
    private lateinit var imageView: ImageView
    private lateinit var botonGuardar: Button
    private val viewModel: EditarLataViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_lata)

        nombreEditText = findViewById(R.id.editTextNombre)
        descripcionEditText = findViewById(R.id.editTextDescripcion)
        imageView = findViewById(R.id.imageView)

        botonGuardar = findViewById(R.id.buttonGuardar)


        val lataId = intent.getIntExtra("lata_id", -1)
        if (lataId == -1) {
            Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            viewModel.obtenerLataPorId(lataId).observe(this) { lata ->
                nombreEditText.setText(lata.nombre)
                descripcionEditText.setText(lata.procedencia)
                Picasso.get().load(Uri.parse(lata.foto)).into(imageView)
            }
        }

        botonGuardar.setOnClickListener {
            val nuevoNombre = nombreEditText.text.toString()
            val nuevaDescripcion = descripcionEditText.text.toString()

            if (lataId != -1) {
                viewModel.actualizarLata(lataId, nuevoNombre, nuevaDescripcion)
                Toast.makeText(this, "Lata actualizada", Toast.LENGTH_SHORT).show()
                finish() // Volver atrás
            }
        }
    }
}
