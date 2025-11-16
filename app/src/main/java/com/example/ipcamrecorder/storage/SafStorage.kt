package com.example.ipcamrecorder.storage

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

object SafStorage {
    fun createFileInTree(contentResolver: ContentResolver, treeUri: Uri, displayName: String, mime: String): Uri? {
        // Build the document root and create a new file
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val pickedDirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        return DocumentsContract.createDocument(contentResolver, pickedDirUri, mime, displayName)
    }
}
