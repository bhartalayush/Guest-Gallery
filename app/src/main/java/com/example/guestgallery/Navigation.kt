package com.example.guestgallery

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.guestgallery.data.GalleryRepository
import com.example.guestgallery.ui.screens.AlbumDetailScreen
import com.example.guestgallery.ui.screens.AlbumGridScreen
import com.example.guestgallery.ui.screens.PhotoDetailScreen
import com.example.guestgallery.ui.screens.SettingsScreen

@Composable
fun MainNavigation(repository: GalleryRepository) {
  val context = LocalContext.current
  val activity = remember(context) { context as? MainActivity }
  
  val backStack = rememberNavBackStack(AlbumGrid)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
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
                  backStack.add(Settings)
              },
              onExitClick = {
                  activity?.lockPhonePhysically()
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
        
        entry<Settings> {
          SettingsScreen(
              repository = repository,
              onBack = { backStack.removeLastOrNull() },
              onAuthClick = { /* Obsolete settings auth removed */ },
              modifier = Modifier.fillMaxSize()
          )
        }
      },
  )
}
