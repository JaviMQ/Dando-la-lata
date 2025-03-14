package com.example.dandolalata.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dandolalata.R
import com.squareup.picasso.Picasso
import com.example.dandolalata.data.entities.Lata
import java.io.File

class LatasAdapter(private var latasList: List<Lata>) :
    RecyclerView.Adapter<LatasAdapter.LataViewHolder>() {

    class LataViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imagen: ImageView = view.findViewById(R.id.imageView)
        val nombre: TextView = view.findViewById(R.id.textView)

        fun bind(lata: Lata) {
            nombre.text = lata.nombre
            Picasso.get().load(File(lata.foto)).into(imagen)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LataViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lata, parent, false)
        return LataViewHolder(view)
    }

    override fun onBindViewHolder(holder: LataViewHolder, position: Int) {
        val lata = latasList[position]
        holder.nombre.text = lata.nombre

        val archivoImagen = File(lata.foto)
        Picasso.get().load(archivoImagen).into(holder.imagen)
    }

    override fun getItemCount() = latasList.size

    fun actualizarLista(nuevasLatas: List<Lata>) {
        latasList = nuevasLatas
        notifyDataSetChanged()
    }
}
