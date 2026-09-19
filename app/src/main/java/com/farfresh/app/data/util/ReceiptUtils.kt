package com.farfresh.app.data.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.farfresh.app.R
import com.farfresh.app.data.model.Sale
import com.farfresh.app.data.model.SaleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReceiptUtils {

    suspend fun generateAndShareReceipt(
        context: Context,
        sale: Sale,
        items: List<SaleItem>,
        customerName: String
    ) {
        withContext(Dispatchers.IO) {
            val width = 800
            val headerHeight = 200
            val itemHeight = 60
            val summaryHeight = 250
            val totalHeight = headerHeight + (items.size * itemHeight) + summaryHeight + 100

            val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            // 1. Encabezado
            paint.color = Color.parseColor("#1565C0")
            canvas.drawRect(0f, 0f, width.toFloat(), headerHeight.toFloat(), paint)

            // Dibujar LOGO imagen (Horizontal)
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            val logoFinalWidth = if (logoBitmap != null) {
                val targetHeight = 110f
                val aspectRatio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
                val targetWidth = targetHeight * aspectRatio
                val destRect = RectF(50f, 45f, 50f + targetWidth, 45f + targetHeight)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
                targetWidth
            } else 0f

            textPaint.color = Color.WHITE
            textPaint.textSize = 45f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val textStartX = 50f + logoFinalWidth + if (logoFinalWidth > 0) 30f else 0f
            
            canvas.drawText("RECIBO DE VENTA", textStartX, 90f, textPaint)

            textPaint.textSize = 22f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            canvas.drawText("Fecha: ${sdf.format(Date(sale.timestamp))}", textStartX, 135f, textPaint)
            canvas.drawText("Cliente: $customerName", textStartX, 170f, textPaint)

            // 2. Lista de Productos
            var currentY = headerHeight + 60f
            textPaint.color = Color.BLACK
            textPaint.textSize = 28f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            
            canvas.drawText("PRODUCTO", 50f, currentY, textPaint)
            canvas.drawText("CANT", 450f, currentY, textPaint)
            canvas.drawText("TOTAL", 650f, currentY, textPaint)
            
            paint.color = Color.LTGRAY
            canvas.drawLine(50f, currentY + 10f, width - 50f, currentY + 10f, paint)
            
            currentY += itemHeight
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            items.forEach { item ->
                canvas.drawText(item.productName, 50f, currentY, textPaint)
                canvas.drawText(item.quantity.toInt().toString(), 450f, currentY, textPaint)
                canvas.drawText("S/ ${String.format("%.2f", item.subtotal)}", 650f, currentY, textPaint)
                currentY += itemHeight
            }

            // 3. Resumen de Pago
            currentY += 40f
            paint.color = Color.parseColor("#F5F5F5")
            canvas.drawRect(50f, currentY, width - 50f, currentY + summaryHeight - 50f, paint)

            currentY += 60f
            textPaint.textSize = 32f
            canvas.drawText("TOTAL VENTA:", 80f, currentY, textPaint)
            canvas.drawText("S/ ${String.format("%.2f", sale.totalAmount)}", 500f, currentY, textPaint)

            currentY += 50f
            textPaint.color = Color.parseColor("#4CAF50")
            canvas.drawText("MONTO PAGADO:", 80f, currentY, textPaint)
            canvas.drawText("S/ ${String.format("%.2f", sale.paidAmount)}", 500f, currentY, textPaint)

            if (sale.pendingBalance > 0) {
                currentY += 60f
                paint.color = Color.parseColor("#FFEBEE")
                canvas.drawRect(70f, currentY - 45f, width - 70f, currentY + 30f, paint)
                
                textPaint.color = Color.RED
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("SALDO PENDIENTE:", 80f, currentY, textPaint)
                canvas.drawText("S/ ${String.format("%.2f", sale.pendingBalance)}", 500f, currentY, textPaint)
            }

            // 4. Guardar y Compartir
            val imagesFolder = File(context.cacheDir, "images")
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "recibo_${sale.id}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()

            val uri = FileProvider.getUriForFile(context, "com.farfresh.app.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Enviar Recibo por:"))
        }
    }

    /**
     * Genera un "Estado de Cuenta" acumulado para un cliente con todas sus deudas pendientes.
     */
    suspend fun generateAndShareStatement(
        context: Context,
        customerName: String,
        pendingSales: List<Sale>,
        allItems: List<SaleItem>
    ) {
        withContext(Dispatchers.IO) {
            val totalPending = pendingSales.sumOf { it.pendingBalance }
            val width = 800
            val headerHeight = 220
            val itemHeight = 60
            val summaryHeight = 200
            
            // Agrupar items por nombre de producto para un resumen limpio
            val groupedItems = allItems.groupBy { it.productName }
                .map { (name, items) ->
                    val totalQty = items.sumOf { it.quantity }
                    val totalSub = items.sumOf { it.subtotal }
                    name to (totalQty to totalSub)
                }

            val totalHeight = headerHeight + (groupedItems.size * itemHeight) + summaryHeight + 100

            val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

            // 1. Encabezado
            paint.color = Color.parseColor("#0D47A1") // Azul más oscuro para estados de cuenta
            canvas.drawRect(0f, 0f, width.toFloat(), headerHeight.toFloat(), paint)

            // Dibujar LOGO imagen (Horizontal para Estado de Cuenta)
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            val logoFinalWidth = if (logoBitmap != null) {
                val targetHeight = 120f
                val aspectRatio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
                val targetWidth = targetHeight * aspectRatio
                val destRect = RectF(50f, 40f, 50f + targetWidth, 40f + targetHeight)
                canvas.drawBitmap(logoBitmap, null, destRect, paint)
                targetWidth
            } else 0f

            textPaint.color = Color.WHITE
            textPaint.textSize = 40f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val textStartX = 50f + logoFinalWidth + if (logoFinalWidth > 0) 30f else 0f
            
            canvas.drawText("ESTADO DE CUENTA ACUMULADO", textStartX, 80f, textPaint)

            textPaint.textSize = 28f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("FarFresh - Tu Tienda de Confianza", textStartX, 130f, textPaint)
            canvas.drawText("Cliente: $customerName", textStartX, 180f, textPaint)

            // 2. Lista de Productos Acumulados
            var currentY = headerHeight + 60f
            textPaint.color = Color.BLACK
            textPaint.textSize = 28f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            
            canvas.drawText("PRODUCTOS PENDIENTES", 50f, currentY, textPaint)
            canvas.drawText("CANT", 450f, currentY, textPaint)
            canvas.drawText("SUBTOTAL", 600f, currentY, textPaint)
            
            paint.color = Color.BLACK
            paint.strokeWidth = 2f
            canvas.drawLine(50f, currentY + 10f, width - 50f, currentY + 10f, paint)
            
            currentY += itemHeight
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            groupedItems.forEach { (name, data) ->
                val (qty, sub) = data
                canvas.drawText(name, 50f, currentY, textPaint)
                canvas.drawText(qty.toInt().toString(), 450f, currentY, textPaint)
                canvas.drawText("S/ ${String.format("%.2f", sub)}", 600f, currentY, textPaint)
                currentY += itemHeight
            }

            // 3. Resumen Final
            currentY += 40f
            paint.color = Color.parseColor("#FFEBEE")
            canvas.drawRect(50f, currentY, width - 50f, currentY + summaryHeight - 50f, paint)

            currentY += 80f
            textPaint.textSize = 45f
            textPaint.color = Color.RED
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DEUDA TOTAL:", 80f, currentY, textPaint)
            canvas.drawText("S/ ${String.format("%.2f", totalPending)}", 450f, currentY, textPaint)

            textPaint.textSize = 22f
            textPaint.color = Color.GRAY
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("* Correspondiente a ${pendingSales.size} ventas pendientes.", 80f, currentY + 50f, textPaint)

            // 4. Guardar y Compartir
            val imagesFolder = File(context.cacheDir, "images")
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "estado_cuenta_${customerName.replace(" ", "_")}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()

            val uri = FileProvider.getUriForFile(context, "com.farfresh.app.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir Estado de Cuenta por:"))
        }
    }
}

