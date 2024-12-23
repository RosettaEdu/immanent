package com.rosettaedu.immanent

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doOnTextChanged
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import coil3.dispose
import coil3.load
import com.rosettaedu.immanent.databinding.ActivityMainBinding
import com.rosettaedu.immanent.databinding.DialogSettingsBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val imageUrlFlow: Flow<String?>
        get() = dataStore.data.map { it[IMAGE_URL_KEY] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.getInsetsController(window, binding.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        lifecycleScope.launch {
            imageUrlFlow.collect { setRefreshingImage(it) }
        }
        binding.settingsButton.setOnClickListener { showSettingsDialog() }
    }

    private fun setRefreshingImage(url: String?) {
        if (url != null) {
            binding.image.load(url)
        }
    }

    private fun showSettingsDialog() {
        val dialogSettingsBinding = DialogSettingsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Image URL")
            .setMessage("Enter a URL")
            .setView(dialogSettingsBinding.root)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", { dialog, _ ->
                dialog.cancel()
            })
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            saveButton.isEnabled = false

            dialogSettingsBinding.imageUrl.doOnTextChanged { text, _, _, _ ->
                saveButton.isEnabled = !text.isNullOrEmpty() && text.toString().toHttpUrlOrNull() != null
            }

            saveButton.setOnClickListener {
                val inputText = dialogSettingsBinding.imageUrl.text.toString()
                lifecycleScope.launch {
                    updateImageUrl(inputText)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroy() {
        binding.image.dispose()
        super.onDestroy()
    }

    private suspend fun updateImageUrl(imageUrl: String) {
        dataStore.edit { it[IMAGE_URL_KEY] = imageUrl }
    }

    companion object {
        private val IMAGE_URL_KEY = stringPreferencesKey("image_url")
    }
}