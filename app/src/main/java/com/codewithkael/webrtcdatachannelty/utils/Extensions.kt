package com.codewithkael.webrtcdatachannelty.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore

fun Uri.getFilePath(context: Context): String? {
    var imagePath: String? = null

    // Check if the URI scheme is "content"
    if (ContentResolver.SCHEME_CONTENT == this.scheme) {
        val contentResolver = context.contentResolver

        // Check if the document ID authority is "com.android.providers.media.documents"
        if (DocumentsContract.isDocumentUri(
                context, this
            ) && this.authority == "com.android.providers.media.documents"
        ) {
            // Extract the document ID from the URI
            val docId = DocumentsContract.getDocumentId(this)

            // Split the document ID into different parts
            val split = docId.split(":".toRegex()).toTypedArray()

            // Get the content URI based on the document type
            val contentUri: Uri? = when (split[0]) {
                "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> null
            }

            // Build the selection criteria with the document ID
            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])

            // Query the MediaStore to retrieve the actual content URI of the document
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            contentUri?.let {
                val cursor = contentResolver.query(it, projection, selection, selectionArgs, null)
                cursor?.use { crsr ->
                    if (crsr.moveToFirst()) {
                        val columnIndex: Int =
                            crsr.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        imagePath = crsr.getString(columnIndex)
                    }
                }
            }
        } else {
            // For other content URIs, directly query the MediaStore to retrieve the actual file path
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(this, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    imagePath = it.getString(columnIndex)
                }
            }
        }
    }
    // Check if the URI scheme is "file"
    else if (ContentResolver.SCHEME_FILE == this.scheme) {
        imagePath = this.path
    }

    return imagePath
}