package com.example.ipcamrecorder.youtube

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import java.io.File

/**
 * This activity demonstrates obtaining Google credentials for YouTube upload.
 * It obtains an OAuth token with scope youtube.upload, then you should perform a resumable upload
 * to https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable using that token.
 *
 * NOTE: For a full resumable upload implementation you may want to delegate uploading to a backend
 * server which stores refresh tokens securely.
 */

class YouTubeUploaderActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val btn = Button(this).apply { text = "Sign in with Google (YouTube)" }
        setContentView(btn)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/youtube.upload"))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                // You have Google account. Retrieve auth token and perform resumable upload to YouTube.
                // For resumable upload flow see README for steps.
                Log.d("YT", "Signed in: " + account.email)
            } catch (e: ApiException) {
                Log.w("YT", "signInResult:failed code=" + e.statusCode)
            }
        }
    }
}
