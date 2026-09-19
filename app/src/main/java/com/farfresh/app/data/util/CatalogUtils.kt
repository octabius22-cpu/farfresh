package com.farfresh.app.data.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.farfresh.app.R
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.farfresh.app.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object CatalogUtils {

    suspend fun generateAndShareCatalog(context: Context, products: List<Product>) {
        withContext(Dispatchers.IO) {
            val width = 1080
            val itemHeight = 300
            val headerHeight = 250
            val totalHeight = headerHeight + (products.size * itemHeight) + 100

            val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 50f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // 1. Dibujar Encabezado Azul
            paint.color = Color.parseColor("#1565C0")
            canvas.drawRect(0f, 0f, width.toFloat(), headerHeight.toFloat(), paint)

            // Dibujar LOGO y Frase CENTRADOS
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            if (logoBitmap != null) {
                val targetHeight = 120f
                val aspectRatio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
                val targetWidth = targetHeight * aspectRatio
                
                // Calcular X para centrar el logo
                val logoX = (width - targetWidth) / 2
                val destRect = RectF(logoX, 40f, logoX + targetWidth, 40f + targetHeight)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)

                // Dibujar Frase centrada debajo del logo
                textPaint.color = Color.WHITE
                textPaint.textSize = 40f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.textAlign = Paint.Align.CENTER
                
                val slogan = "¡Calidad y frescura al mejor precio!"
                canvas.drawText(slogan, width / 2f, 210f, textPaint)
            }

            // 2. Dibujar Productos
            // Resetear alineación para los productos
            textPaint.textAlign = Paint.Align.LEFT
            var currentY = headerHeight + 50f
            val imageLoader = ImageLoader(context)

            products.forEach { product ->
                // Fondo de item
                paint.color = Color.parseColor("#F5F5F5")
                canvas.drawRoundRect(50f, currentY, width - 50f, currentY + itemHeight - 40f, 20f, 20f, paint)

                // Cargar Imagen de Coil
                val productBitmap = if (!product.imageUrl.isNullOrBlank()) {
                    val request = ImageRequest.Builder(context)
                        .data(product.imageUrl)
                        .allowHardware(false) // Necesario para Canvas
                        .build()
                    val result = imageLoader.execute(request)
                    if (result is SuccessResult) {
                        (result.drawable as android.graphics.drawable.BitmapDrawable).bitmap
                    } else null
                } else null

                if (productBitmap != null) {
                    val destRect = RectF(80f, currentY + 30f, 80f + 200f, currentY + itemHeight - 70f)
                    canvas.drawBitmap(productBitmap, null, destRect, paint)
                } else {
                    paint.color = Color.LTGRAY
                    canvas.drawRect(80f, currentY + 30f, 280f, currentY + itemHeight - 70f, paint)
                }

                // Texto Nombre
                textPaint.color = Color.BLACK
                textPaint.textSize = 45f
                canvas.drawText(product.name, 320f, currentY + 100f, textPaint)

                // Texto Medida (ml)
                if (product.volumeMl != null) {
                    textPaint.textSize = 35f
                    textPaint.color = Color.GRAY
                    canvas.drawText("${product.volumeMl}ml", 320f, currentY + 160f, textPaint)
                }

                // Texto Precio (Enmarcado)
                paint.color = Color.parseColor("#4CAF50")
                canvas.drawRoundRect(width - 400f, currentY + 110f, width - 100f, currentY + 190f, 10f, 10f, paint)
                
                textPaint.color = Color.WHITE
                textPaint.textSize = 45f
                val priceText = "S/ ${String.format("%.2f", product.sellingPrice)}"
                canvas.drawText(priceText, width - 380f, currentY + 170f, textPaint)

                currentY += itemHeight
            }

            // 3. Guardar y Compartir
            val imagesFolder = File(context.cacheDir, "images")
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "catalogo_farfresh.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.flush()
            stream.close()

            val uri = FileProvider.getUriForFile(context, "com.farfresh.app.fileprovider", file)
            shareImage(context, uri)
        }
    }

    private fun shareImage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Catálogo por:"))
    }
}
