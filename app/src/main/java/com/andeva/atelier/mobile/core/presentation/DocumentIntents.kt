package com.andeva.atelier.mobile.core.presentation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Apertura y envío unificado de documentos descargados (`cacheDir/pdfs`, `res/xml/file_paths.xml`):
 * PDFs de proforma, informe IA, flota, flujo de caja, pase de salida y comprobantes SUNAT (PDF, XML, CDR ZIP).
 */
object DocumentIntents {
    const val MIME_PDF = "application/pdf"
    const val MIME_XML = "application/xml"
    const val MIME_ZIP = "application/zip"

    fun openDocument(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String
    ): Boolean {
        return try {
            val uri = uriFor(context, file)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
            true
        } catch (_: ActivityNotFoundException) {
            shareDocument(context, file, mimeType, chooserTitle)
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun shareDocument(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String
    ): Boolean {
        return try {
            val uri = uriFor(context, file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun openPdf(context: Context, file: File, chooserTitle: String): Boolean =
        openDocument(context, file, MIME_PDF, chooserTitle)

    fun sharePdf(context: Context, file: File, chooserTitle: String): Boolean =
        shareDocument(context, file, MIME_PDF, chooserTitle)

    fun openFile(context: Context, file: File, mimeType: String, chooserTitle: String): Boolean =
        openDocument(context, file, mimeType, chooserTitle)

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String): Boolean =
        shareDocument(context, file, mimeType, chooserTitle)

    private fun uriFor(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
