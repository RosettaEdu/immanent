package com.rosettaedu.immanent

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.rosettaedu.immanent.data.PreferencesRepositoryImpl

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ImmanentApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(true)
            .build()

    val preferencesRepository
        get() = PreferencesRepositoryImpl(dataStore)
}
