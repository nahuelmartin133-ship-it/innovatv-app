package com.innovatv.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.innovatv.app.databinding.ItemCanalBinding
import com.innovatv.app.databinding.ItemPeliculaBinding
import com.innovatv.app.models.Canal
import com.innovatv.app.models.Pelicula

class CanalesAdapter(
    private val canales: List<Canal>,
    private val onClick: (Canal) -> Unit
) : RecyclerView.Adapter<CanalesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCanalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCanalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val canal = canales[position]
        holder.binding.textNombre.text = canal.nombre
        holder.binding.textCategoria.text = canal.categoria
        holder.binding.textNumero.text = canal.num.toString()

        if (canal.epgActual.isNotEmpty()) {
            holder.binding.textEPG.text = canal.epgActual
            holder.binding.textEPG.visibility = android.view.View.VISIBLE
            holder.binding.textCategoria.visibility = android.view.View.GONE
        } else {
            holder.binding.textEPG.visibility = android.view.View.GONE
            holder.binding.textCategoria.visibility = android.view.View.VISIBLE
        }

        if (canal.logo.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(canal.logo)
                .placeholder(R.color.surface_variant)
                .error(R.color.surface_variant)
                .into(holder.binding.imgLogo)
        } else {
            holder.binding.imgLogo.setImageResource(R.color.surface_variant)
        }

        holder.itemView.setOnClickListener { onClick(canal) }
    }

    override fun getItemCount(): Int = canales.size
}

class PeliculasAdapter(
    private val peliculas: List<Pelicula>,
    private val onClick: (Pelicula) -> Unit
) : RecyclerView.Adapter<PeliculasAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPeliculaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPeliculaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val peli = peliculas[position]
        holder.binding.textTitulo.text = peli.titulo
        holder.binding.textInfo.text = "${peli.categoria} ${peli.anio}".trim()

        if (peli.caratula.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(peli.caratula)
                .placeholder(R.color.surface_variant)
                .error(R.color.surface_variant)
                .into(holder.binding.imgCaratula)
        } else {
            holder.binding.imgCaratula.setImageResource(R.color.surface_variant)
        }

        holder.itemView.setOnClickListener { onClick(peli) }
    }

    override fun getItemCount(): Int = peliculas.size
}