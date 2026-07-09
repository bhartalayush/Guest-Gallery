package com.example.guestgallery

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.guestgallery.data.GalleryRepository
import com.example.guestgallery.ui.screens.AlbumDetailScreen
import com.example.guestgallery.ui.screens.AlbumGridScreen
import com.example.guestgallery.ui.screens.AuthScreen
import com.example.guestgallery.ui.screens.PhotoDetailScreen
import com.example.guestgallery.ui.screens.SettingsScreen
import com.example.guestgallery.ui.screens.SetupScreen

@Composable
fun MainNavigation(repository: GalleryRepository) {
  val isAppLocked by repository.isAppLocked.collectAsState()

  if (isAppLocked && repository.isSetupCompleted) {
      AuthScreen(
          action = "unlock",
          repository = repository,
          onSuccess = {
              repository.setAppLocked(false)
          },
          onCancel = {
              // Stay on lock screen
          },
          modifier = Modifier.fillMaxSize()
      )
  } else {
      // Determine start destination: setup wizard if PIN not configured, otherwise gallery
      val startDestination = remember(repository) {
          if (repository.isSetupCompleted) AlbumGrid else Setup
      }
      
      val backStack = rememberNavBackStack(startDestination)

      NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
          entryProvider {
        entry<Setup> {
          SetupScreen(
              repository = repository,
              onComplete = {
                  backStack.add(AlbumGrid)
              },
              modifier = Modifier.fillMaxSize()
          )
        }
        
        entry<AlbumGrid> {
          AlbumGridScreen(
              repository = repository,
              onAlbumClick = { album ->
                  backStack.add(AlbumDetail(album.id, album.name))
              },
              onPhotoClick = { albumId, photoId ->
                  backStack.add(PhotoDetail(albumId, photoId))
              },
              onSettingsClick = {
                  backStack.add(Auth("settings"))
              },
              onExitClick = {
                  if (repository.requireAuthOnExit.value) {
                      backStack.add(Auth("exit"))
                  } else {
                      repository.setGuestMode(false)
                  }
              },
              modifier = Modifier.fillMaxSize()
          )
        }
        
        entry<AlbumDetail> { key ->
          AlbumDetailScreen(
              albumId = key.albumId,
              albumName = key.albumName,
              repository = repository,
              onPhotoClick = { photo ->
                  backStack.add(PhotoDetail(key.albumId, photo.id))
              },
              onBack = { backStack.removeLastOrNull() },
              modifier = Modifier.fillMaxSize()
          )
        }
        
        entry<PhotoDetail> { key ->
          PhotoDetailScreen(
              albumId = key.albumId,
              selectedPhotoId = key.selectedPhotoId,
              repository = repository,
              onBack = { backStack.removeLastOrNull() },
              modifier = Modifier.fillMaxSize()
          )
        }
        
        entry<Auth> { key ->
          AuthScreen(
              action = key.targetAction,
              repository = repository,
              onSuccess = {
                  backStack.removeLastOrNull() // Pop Auth screen
                  if (key.targetAction == "settings") {
                      backStack.add(Settings)
                  } else if (key.targetAction == "exit") {
                      repository.setGuestMode(false)
                      backStack.add(AlbumGrid)
                  } else if (key.targetAction == "decoy_edit") {
                      repository.setDecoyEditMode(true)
                      backStack.add(AlbumGrid)
                  }
              },
              onCancel = { backStack.removeLastOrNull() },
              modifier = Modifier.fillMaxSize()
          )
        }
        
        entry<Settings> {
          SettingsScreen(
              repository = repository,
              onBack = { backStack.removeLastOrNull() },
              onAuthClick = { action -> backStack.add(Auth(action)) },
              modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
  }
}
