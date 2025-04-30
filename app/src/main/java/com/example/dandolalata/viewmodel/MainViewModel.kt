package com.example.dandolalata.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.dandolalata.data.database.AppDatabase
import com.example.dandolalata.data.entities.Lata
import com.example.dandolalata.data.entities.Marca
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.obtenerInstancia(application)

    private val _marcas = MutableLiveData<List<Marca>>()
    val marcas: LiveData<List<Marca>> get() = _marcas

    private val _latas = MutableLiveData<List<Lata>>()
    val latas: LiveData<List<Lata>> get() = _latas

    init {
        cargarMarcas()
        cargarLatas()
    }

    private fun cargarMarcas() {
        viewModelScope.launch {
            db.marcaDao().obtenerTodasFlowPornombre().collect { marcasFromDb ->
                val listaConTodas = listOf(Marca(id = 0, nombre = "Todas las marcas")) + marcasFromDb
                _marcas.postValue(listaConTodas)
            }
        }
    }

    private fun cargarLatas() {
        viewModelScope.launch {
            db.lataDao().obtenerTodasFlow().collect { latasFromDb ->
                _latas.postValue(latasFromDb)
            }
        }
    }

    fun filtrarLatas(marca: Marca?) {
        viewModelScope.launch(Dispatchers.IO) {
            val latasFiltradas = if (marca == null || marca.id == 0) {
                db.lataDao().obtenerTodas() // Mostrar todas
            } else {
                db.lataDao().obtenerPorMarcaId(marca.id)
            }
            _latas.postValue(latasFiltradas)
        }
    }
}
