package com.example.dandolalata.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.entities.Lata
import kotlinx.coroutines.launch

class EditarLataViewModel(application: Application) : AndroidViewModel(application) {

    private val lataDao = AppDatabase.obtenerInstancia(application).lataDao()

    fun obtenerLataPorId(id: Int): LiveData<Lata> {
        return lataDao.obtenerPorId(id)
    }

    fun actualizarLata(id: Int, nuevoNombre: String, nuevaDescripcion: String, nuevaMarca: Int) {
        viewModelScope.launch {
            val lata = lataDao.obtenerPorIdDirecto(id)
            val actualizada = lata.copy(nombre = nuevoNombre, procedencia = nuevaDescripcion, marcaId = nuevaMarca)
            lataDao.actualizar(actualizada)
        }
    }
}
