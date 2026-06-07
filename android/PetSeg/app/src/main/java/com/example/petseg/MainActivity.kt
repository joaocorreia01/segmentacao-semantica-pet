package com.example.petseg

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var helper: SegmentationHelper
    private lateinit var originalView: ImageView
    private lateinit var maskView: ImageView
    private var selectedBitmap: Bitmap? = null

    // Photo Picker do Android: nao precisa de permissao de armazenamento.
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = uriToBitmap(uri)
            selectedBitmap = bmp
            originalView.setImageBitmap(bmp)
            maskView.setImageDrawable(null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        helper = SegmentationHelper(this)
        originalView = findViewById(R.id.originalImage)
        maskView = findViewById(R.id.maskImage)

        findViewById<Button>(R.id.selectButton).setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        findViewById<Button>(R.id.runButton).setOnClickListener {
            val bmp = selectedBitmap
            if (bmp == null) {
                Toast.makeText(this, "Selecione uma imagem primeiro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mask = helper.segment(bmp)
            val overlay = helper.overlay(bmp, mask)
            maskView.setImageBitmap(overlay)
        }
    }

    /** Converte o Uri escolhido em Bitmap ARGB_8888 mutavel (software). */
    private fun uriToBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                // IMPORTANTE: bitmap de hardware nao permite getPixels(); forcar software.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        helper.close()
    }
}
