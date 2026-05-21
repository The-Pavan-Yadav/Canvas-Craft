package com.example

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class CollageViewModel : ViewModel() {
    val layers = mutableStateListOf<Layer>()

    fun addLayer(layer: Layer) {
        // Deselect all existing layers first if the new one is to be selected?
        // Let's assume standard behavior is keeping selection as is, or we might want to just add it.
        layers.add(layer)
        layers.sortBy { it.zIndex }
    }

    fun removeLayer(layerId: String) {
        layers.removeAll { it.id == layerId }
    }

    fun selectLayer(layerId: String?) {
        for (i in layers.indices) {
            val layer = layers[i]
            if (layer.id == layerId) {
                if (!layer.isSelected) {
                    layers[i] = updateLayerSelection(layer, true)
                }
            } else {
                if (layer.isSelected) {
                    layers[i] = updateLayerSelection(layer, false)
                }
            }
        }
    }

    private fun updateLayerSelection(layer: Layer, isSelected: Boolean): Layer {
        return when (layer) {
            is ImageLayer -> layer.copy(isSelected = isSelected)
            is TextLayer -> layer.copy(isSelected = isSelected)
            else -> layer
        }
    }

    fun updateLayerTransform(layerId: String, offsetX: Float, offsetY: Float, scale: Float, rotation: Float) {
        val index = layers.indexOfFirst { it.id == layerId }
        if (index != -1) {
            val layer = layers[index]
            layers[index] = when (layer) {
                is ImageLayer -> layer.copy(offsetX = offsetX, offsetY = offsetY, scale = scale, rotation = rotation)
                is TextLayer -> layer.copy(offsetX = offsetX, offsetY = offsetY, scale = scale, rotation = rotation)
                else -> layer
            }
        }
    }
}
