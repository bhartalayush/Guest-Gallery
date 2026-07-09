package com.example.guestgallery

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.guestgallery.data.DefaultGalleryRepository
import com.example.guestgallery.theme.GuestGalleryTheme

class MainActivity : FragmentActivity() {
  private lateinit var repository: DefaultGalleryRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    repository = DefaultGalleryRepository(applicationContext)

    enableEdgeToEdge()
    setContent {
      GuestGalleryTheme(repository = repository) { 
        Surface(
          modifier = Modifier.fillMaxSize(), 
          color = MaterialTheme.colorScheme.background
        ) { 
          MainNavigation(repository) 
        } 
      }
    }
  }

  override fun onStop() {
    super.onStop()
    if (::repository.isInitialized && repository.isSetupCompleted) {
      repository.setAppLocked(true)
    }
  }
}
