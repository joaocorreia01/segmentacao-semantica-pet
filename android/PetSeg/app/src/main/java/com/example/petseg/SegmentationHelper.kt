package com.example.petseg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Roda o UNet exportado pelo ai-edge-torch (FP32).
 *
 * IMPORTANTE: ai-edge-torch preserva o layout NCHW do PyTorch, entao:
 *   entrada -> [1, 3, 128, 128]  (planar: R inteiro, depois G, depois B)
 *   saida   -> [1, 3, 128, 128]  (logits; argmax no eixo de canal)
 *
 * Classes: 0 = pet, 1 = fundo, 2 = borda
 */
class SegmentationHelper(context: Context, modelAsset: String = "unet_pet.tflite") {

    private val interpreter: Interpreter
    private val inputSize = 128
    private val numClasses = 3

    // pet (vermelho), fundo (transparente/preto), borda (verde)
    private val palette = intArrayOf(
        Color.rgb(255, 0, 0),
        Color.rgb(0, 0, 0),
        Color.rgb(0, 255, 0)
    )

    init {
        val opts = Interpreter.Options().apply { numThreads = 4 }
        interpreter = Interpreter(loadModelFile(context, modelAsset), opts)
    }

    private fun loadModelFile(context: Context, asset: String): ByteBuffer {
        val fd = context.assets.openFd(asset)
        FileInputStream(fd.fileDescriptor).use { stream ->
            return stream.channel.map(
                FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
            )
        }
    }

    /** Pre-processa o bitmap para o buffer NCHW float32 em [0,1]. */
    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        val buffer = ByteBuffer
            .allocateDirect(1 * 3 * inputSize * inputSize * 4)
            .order(ByteOrder.nativeOrder())

        // Ordem PLANAR (NCHW): primeiro todo o canal R, depois G, depois B.
        for (c in 0 until 3) {
            for (i in pixels.indices) {
                val px = pixels[i]
                val value = when (c) {
                    0 -> Color.red(px)
                    1 -> Color.green(px)
                    else -> Color.blue(px)
                }
                buffer.putFloat(value / 255.0f)
            }
        }
        buffer.rewind()
        return buffer
    }

    /** Retorna a mascara argmax como matriz [H][W] de indices de classe. */
    fun segment(bitmap: Bitmap): Array<IntArray> {
        val input = preprocess(bitmap)
        // saida NCHW achatada: [1, 3, 128, 128]
        val output = Array(1) {
            Array(numClasses) { Array(inputSize) { FloatArray(inputSize) } }
        }
        interpreter.run(input, output)

        val mask = Array(inputSize) { IntArray(inputSize) }
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                var best = 0
                var bestVal = output[0][0][y][x]
                for (c in 1 until numClasses) {
                    val v = output[0][c][y][x]
                    if (v > bestVal) { bestVal = v; best = c }
                }
                mask[y][x] = best
            }
        }
        return mask
    }

    /** Sobrepoe a mascara colorida na imagem original (redimensionada ao tamanho da mascara). */
    fun overlay(original: Bitmap, mask: Array<IntArray>, alpha: Float = 0.5f): Bitmap {
        val base = Bitmap.createScaledBitmap(original, inputSize, inputSize, true)
        val result = base.copy(Bitmap.Config.ARGB_8888, true)
        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val cls = mask[y][x]
                if (cls == 1) continue            // fundo: nao pinta
                val c = palette[cls]
                val src = result.getPixel(x, y)
                val r = (Color.red(src) * (1 - alpha) + Color.red(c) * alpha).toInt()
                val g = (Color.green(src) * (1 - alpha) + Color.green(c) * alpha).toInt()
                val b = (Color.blue(src) * (1 - alpha) + Color.blue(c) * alpha).toInt()
                result.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        // volta ao tamanho original para exibir bonito no ImageView
        return Bitmap.createScaledBitmap(result, original.width, original.height, true)
    }

    fun close() = interpreter.close()
}
