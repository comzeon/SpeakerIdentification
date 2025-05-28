package com.example.speakeridentification.model

import android.content.Context
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream

object ModelRunner {
    private lateinit var module: Module

    fun initialize(context: Context, modelFileName: String = "model.pt") {
        val assetManager = context.assets
        val inputStream = assetManager.open(modelFileName)
        val modelFile = File(context.cacheDir, modelFileName)
        val outputStream = FileOutputStream(modelFile)
        inputStream.copyTo(outputStream)
        require(modelFile.exists()) { "Model file not found: $modelFileName" }
        module = Module.load(modelFile.absolutePath)
    }

    fun infer(inputFeatures: FloatArray, shape: LongArray): FloatArray {
        val inputTensor = Tensor.fromBlob(inputFeatures, shape)
        val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
        return outputTensor.dataAsFloatArray
    }
}