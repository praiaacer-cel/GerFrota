package com.gerfrota.lite.services

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File

/** Mescla o PDF base com anexos PDF renderizando páginas via PdfRenderer (sem libs pagas). */
object PdfMergeHelper {

    fun mergeBaseWith(baseBytes: ByteArray, pdfPaths: List<String>): ByteArray {
        val out = PdfDocument()
        var pageNum = 1

        fun addFromBytes(bytes: ByteArray) {
            val tmp = File.createTempFile("merge", ".pdf")
            tmp.writeBytes(bytes)
            ParcelFileDescriptor.open(tmp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_DISPLAY)
                            page.close()
                            val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, pageNum++).create()
                            val p = out.startPage(info)
                            p.canvas.drawBitmap(bmp, 0f, 0f, null)
                            out.finishPage(p)
                            bmp.recycle()
                        }
                    }
                }
            }
            tmp.delete()
        }

        addFromBytes(baseBytes)
        pdfPaths.forEach { path -> runCatching { addFromBytes(File(path).readBytes()) } }

        val bos = ByteArrayOutputStream()
        out.writeTo(bos)
        out.close()
        return bos.toByteArray()
    }
}
