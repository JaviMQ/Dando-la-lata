package com.example.dandolalata.ui.adapters

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.dandolalata.R
import com.squareup.picasso.Picasso
import com.example.dandolalata.data.entities.Lata


class LatasAdapter(private var latasList: List<Lata>, private val onItemClick: (Int) -> Unit) :
    RecyclerView.Adapter<LatasAdapter.LataViewHolder>() {

    class LataViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imagen: ImageView = view.findViewById(R.id.imageView)
        val nombre: TextView = view.findViewById(R.id.textView)

        fun bind(lata: Lata) {
            nombre.text = lata.nombre

            val sizeInPx = (64 * imagen.context.resources.displayMetrics.density).toInt()
            Glide.with(imagen.context)
                .load(Uri.parse(lata.foto))
                .override(sizeInPx, sizeInPx)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .into(imagen)


            // Picasso.get().load(Uri.parse(lata.foto)).into(imagen)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LataViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lata, parent, false)
        return LataViewHolder(view)
    }

    override fun onBindViewHolder(holder: LataViewHolder, position: Int) {
        val lata = latasList[position]
        holder.bind(lata)

        holder.itemView.setOnClickListener {
            onItemClick(lata.id)
        }
    }

    override fun getItemCount() = latasList.size

    fun actualizarLista(nuevaLista: List<Lata>) {
        val diffCallback = LatasDiffCallback(latasList, nuevaLista)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        latasList = nuevaLista // Reemplazamos la lista con la nueva
        diffResult.dispatchUpdatesTo(this) // Aplica los cambios al RecyclerView
    }
}
