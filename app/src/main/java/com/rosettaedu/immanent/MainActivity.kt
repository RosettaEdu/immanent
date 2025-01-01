package com.rosettaedu.immanent

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.ViewCompat
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var windowInsetsControllerCompat: WindowInsetsControllerCompat

    private val imageUrlFlow: Flow<String?>
        get() = dataStore.data.map { it[IMAGE_URL_KEY] }

    private val viewModel by viewModels<MainViewModel>()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        windowInsetsControllerCompat = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsControllerCompat.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_DEFAULT

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { view, windowInsets ->
            if (windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())
                || windowInsets.isVisible(WindowInsetsCompat.Type.statusBars())
            ) {
                viewModel.onFullScreenExited()
            } else {
                viewModel.onFullScreenEntered()
            }
            ViewCompat.onApplyWindowInsets(view, windowInsets)
        }

        lifecycleScope.apply {
            launch { imageUrlFlow.collect { setRefreshingImage(it) } }
            launch { viewModel.isFullScreen.collect { setFullScreenButton(it) } }
        }
        binding.settingsButton.setOnClickListener { showSettingsDialog() }

        binding.root.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                showControls()
            }

            true
        }
    }

    private fun showControls() {
        if (binding.controls.visibility != View.VISIBLE) {
            ObjectAnimator.ofFloat(binding.controls, View.ALPHA, 0f, 1f)
                .setDuration(FADE_IN_OUT_DURATION)
                .apply { doOnStart { binding.controls.visibility = View.VISIBLE } }
                .start()
        }
        lifecycleScope.launch {
            delay(FADE_OUT_DELAY)
            fadeOutControls()
        }
    }

    private fun fadeOutControls() {
        ObjectAnimator.ofFloat(binding.controls, View.ALPHA, 1f, 0f)
            .setDuration(FADE_IN_OUT_DURATION)
            .apply { doOnEnd { binding.controls.visibility = View.INVISIBLE } }
            .start()
    }

    private fun setFullScreenButton(enabled: Boolean) {
        binding.fullScreenButton.apply {
            if (enabled) {
                setOnClickListener {
                    windowInsetsControllerCompat.show(WindowInsetsCompat.Type.systemBars())
                    viewModel.onFullScreenExited()
                }
                setImageResource(R.drawable.baseline_fullscreen_exit_24)
                contentDescription = resources.getString(R.string.exit_full_screen)
            } else {
                setOnClickListener {
                    windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.systemBars())
                    // ideally we would let OnApplyInsetsListener be responsible for calling
                    // onFullScreenExited(). However WindowInsetsCompat.isVisible cannot be relied
                    // up on some older devices (e.g. API 29 and below) - in our testing it always
                    // returns true.
                    viewModel.onFullScreenEntered()
                }
                setImageResource(R.drawable.baseline_fullscreen_24)
                contentDescription = resources.getString(R.string.full_screen)
            }
        }
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
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            saveButton.isEnabled = false

            dialogSettingsBinding.imageUrl.doOnTextChanged { text, _, _, _ ->
                saveButton.isEnabled =
                    !text.isNullOrEmpty() && text.toString().toHttpUrlOrNull() != null
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

        private const val FADE_IN_OUT_DURATION = 300L
        private const val FADE_OUT_DELAY = 3000L
    }
}