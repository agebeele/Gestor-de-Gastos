package com.example.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.currency.CurrencyManager
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.security.EncryptionService
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {

    fun generateAndSharePdf(
        context: Context,
        transactions: List<Transaction>,
        categories: Map<Long, Category>,
        monthYear: String,
        totalIncome: Double,
        totalExpense: Double,
        currencyCode: String
    ): Result<File> {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileName = "Estado_De_Cuenta_${monthYear.replace("-", "_")}.pdf"
            val file = File(exportDir, fileName)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.DKGRAY
            }

            // Header Background Bar
            paint.color = Color.parseColor("#059669")
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            // Header Title
            textPaint.color = Color.WHITE
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 20f
            canvas.drawText("ESTADO FINANCIERO MENSUAL", 30f, 40f, textPaint)

            textPaint.textSize = 12f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText("Gestor de Gastos Inteligente • Periodo: $monthYear", 30f, 65f, textPaint)

            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
            textPaint.textSize = 10f
            canvas.drawText("Generado el: $nowStr", 400f, 65f, textPaint)

            // Summary Cards Box
            var yPos = 115f
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(25f, yPos, 570f, yPos + 75f, 10f, 10f, paint)

            val netBalance = totalIncome - totalExpense
            val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome) * 100.0 else 0.0

            textPaint.color = Color.BLACK
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 11f
            canvas.drawText("RESUMEN GENERAL ($currencyCode)", 40f, yPos + 22f, textPaint)

            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 10f
            textPaint.color = Color.parseColor("#059669")
            canvas.drawText("Ingresos: +${CurrencyManager.formatCompact(totalIncome, currencyCode)}", 40f, yPos + 45f, textPaint)

            textPaint.color = Color.parseColor("#DC2626")
            canvas.drawText("Gastos: -${CurrencyManager.formatCompact(totalExpense, currencyCode)}", 180f, yPos + 45f, textPaint)

            textPaint.color = if (netBalance >= 0) Color.parseColor("#059669") else Color.parseColor("#DC2626")
            canvas.drawText("Balance Neto: ${CurrencyManager.formatCompact(netBalance, currencyCode)}", 320f, yPos + 45f, textPaint)

            textPaint.color = Color.parseColor("#4B5563")
            canvas.drawText("Tasa Ahorro: ${String.format(Locale.US, "%.1f%%", savingsRate)}", 460f, yPos + 45f, textPaint)

            // Table Header
            yPos += 95f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawRect(25f, yPos, 570f, yPos + 25f, paint)

            textPaint.color = Color.parseColor("#1E293B")
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 10f
            canvas.drawText("FECHA", 35f, yPos + 17f, textPaint)
            canvas.drawText("CONCEPTO", 100f, yPos + 17f, textPaint)
            canvas.drawText("CATEGORÍA", 280f, yPos + 17f, textPaint)
            canvas.drawText("MÉTODO", 410f, yPos + 17f, textPaint)
            canvas.drawText("MONTO", 495f, yPos + 17f, textPaint)

            // Table Rows
            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            yPos += 25f
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textSize = 9f

            var rowIndex = 0
            for (t in transactions.take(30)) { // Display up to 30 items per page
                yPos += 20f
                if (rowIndex % 2 == 1) {
                    paint.color = Color.parseColor("#F8FAFC")
                    canvas.drawRect(25f, yPos - 14f, 570f, yPos + 6f, paint)
                }

                val cat = categories[t.categoryId]?.name ?: "General"
                val dateText = dateFormatter.format(Date(t.date))
                val titleTrunc = if (t.title.length > 28) t.title.take(25) + "..." else t.title
                val catTrunc = if (cat.length > 18) cat.take(16) + "..." else cat

                textPaint.color = Color.DKGRAY
                canvas.drawText(dateText, 35f, yPos, textPaint)
                canvas.drawText(titleTrunc, 100f, yPos, textPaint)
                canvas.drawText(catTrunc, 280f, yPos, textPaint)
                canvas.drawText(t.paymentMethod.displayName.take(12), 410f, yPos, textPaint)

                if (t.type == TransactionType.INCOME) {
                    textPaint.color = Color.parseColor("#059669")
                    canvas.drawText("+${CurrencyManager.formatCompact(t.amount, currencyCode)}", 495f, yPos, textPaint)
                } else {
                    textPaint.color = Color.parseColor("#DC2626")
                    canvas.drawText("-${CurrencyManager.formatCompact(t.amount, currencyCode)}", 495f, yPos, textPaint)
                }
                rowIndex++
            }

            // Footer
            val hash = EncryptionService.generateFingerprint("$monthYear-$totalIncome-$totalExpense-${transactions.size}")
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(25f, 790f, 570f, 790f, paint)
            textPaint.color = Color.GRAY
            textPaint.textSize = 8f
            canvas.drawText("Seguridad Cifrada E2E • Hash de verificación: $hash", 30f, 805f, textPaint)
            canvas.drawText("Total de transacciones: ${transactions.size}", 440f, 805f, textPaint)

            document.finishPage(page)

            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()

            shareFile(context, file, "application/pdf", "Estado de Cuenta $monthYear")
            return Result.success(file)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }

    fun generateAndShareExcel(
        context: Context,
        transactions: List<Transaction>,
        categories: Map<Long, Category>,
        monthYear: String,
        totalIncome: Double,
        totalExpense: Double,
        currencyCode: String
    ): Result<File> {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val fileName = "Estado_Financiero_${monthYear.replace("-", "_")}.csv"
            val file = File(exportDir, fileName)

            val fos = FileOutputStream(file)
            val writer = OutputStreamWriter(fos, StandardCharsets.UTF_8)

            // Write UTF-8 BOM for Excel to open Spanish accents perfectly
            fos.write(0xEF)
            fos.write(0xBB)
            fos.write(0xBF)

            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

            // Header Info
            writer.write("REPORTE FINANCIERO MENSUAL - GESTOR DE GASTOS\n")
            writer.write("Periodo:,$monthYear\n")
            writer.write("Moneda:,$currencyCode\n")
            writer.write("Total Ingresos:,$totalIncome\n")
            writer.write("Total Gastos:,$totalExpense\n")
            writer.write("Balance Neto:,${totalIncome - totalExpense}\n\n")

            // Column Headers
            writer.write("ID,Fecha,Tipo,Concepto,Categoría,Monto,Moneda,Monto Original,Moneda Original,Método de Pago,Notas,Etiquetas\n")

            for (t in transactions) {
                val cat = categories[t.categoryId]?.name ?: "General"
                val dateStr = dateFormatter.format(Date(t.date))
                val typeStr = if (t.type == TransactionType.INCOME) "Ingreso" else "Gasto"
                val notesSanitized = t.notes.replace(",", " ").replace("\n", " ")
                val tagsSanitized = t.tags.joinToString(";").replace(",", " ")

                writer.write("${t.id},\"$dateStr\",\"$typeStr\",\"${t.title.replace("\"", "\"\"")}\",\"$cat\",${t.amount},\"$currencyCode\",${t.originalAmount},\"${t.currencyCode}\",\"${t.paymentMethod.displayName}\",\"$notesSanitized\",\"$tagsSanitized\"\n")
            }

            writer.flush()
            writer.close()
            fos.close()

            shareFile(context, file, "text/csv", "Exportación Excel $monthYear")
            return Result.success(file)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, subject: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Adjunto tu reporte financiero de Gestor de Gastos.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Compartir o Guardar Estado de Cuenta").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
