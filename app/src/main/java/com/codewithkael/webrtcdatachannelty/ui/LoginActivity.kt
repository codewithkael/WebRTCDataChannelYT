package com.codewithkael.webrtcdatachannelty.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.codewithkael.webrtcdatachannelty.R
import com.codewithkael.webrtcdatachannelty.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var views: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(views.root)

        views.enterBtn.setOnClickListener {
            if (views.usernameEt.text.isNullOrEmpty()) {
                Toast.makeText(this, "please fill the username", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    putExtra("username", views.usernameEt.text.toString())
                }
            )
        }
    }
}