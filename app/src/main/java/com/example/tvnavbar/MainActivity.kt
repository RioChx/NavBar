package com.example.tvnavbar
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
class MainActivity : AppCompatActivity() { override fun onCreate(s: Bundle?) { super.onCreate(s); startService(Intent(this, FloatingNavService::class.java)); finish() } }