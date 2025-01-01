package com.rosettaedu.immanent.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface PreferencesRepository {
    val imageUrl: Flow<String?>

    suspend fun getImageUrl(): String?

    suspend fun updateImageUrl(imageUrl: String)
}

class PreferencesRepositoryImpl(private val dataStore: DataStore<Preferences>) : PreferencesRepository {

    override val imageUrl: Flow<String?> = dataStore.data.map { it[IMAGE_URL_KEY] }

    override suspend fun getImageUrl(): String? = dataStore.data.first()[IMAGE_URL_KEY]

    override suspend fun updateImageUrl(imageUrl: String) {
        dataStore.edit { it[IMAGE_URL_KEY] = imageUrl }
    }

    companion object {
        private val IMAGE_URL_KEY = stringPreferencesKey("image_url")
    }
}