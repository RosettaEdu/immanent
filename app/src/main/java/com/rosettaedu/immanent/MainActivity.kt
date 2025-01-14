package com.rosettaedu.immanent

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
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
import androidx.lifecycle.lifecycleScope
import coil3.dispose
import coil3.load
import com.rosettaedu.immanent.databinding.ActivityMainBinding
import com.rosettaedu.immanent.databinding.DialogSettingsBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dialogSettingsBinding: DialogSettingsBinding
    private lateinit var windowInsetsControllerCompat: WindowInsetsControllerCompat

    private val viewModel by viewModels<MainViewModel> { MainViewModel.Factory }

    private lateinit var settingsDialog: AlertDialog
    private var currentImageUrl: String = ""

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialogSettingsBinding = DialogSettingsBinding.inflate(layoutInflater, null, false)

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
            launch {
                viewModel.imageUrl.collect {
                    setRefreshingImage(it)
                    currentImageUrl = it.orEmpty()
                }
            }
            launch {
                viewModel.isFullScreen.collect { enabled ->
                    setFullScreenButton(enabled)
                    setKeepScreenOn(enabled)
                }
            }
        }
        binding.settingsButton.setOnClickListener { showSettingsDialog() }

        binding.root.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                showControls()
            }

            true
        }

        createSettingsDialog()
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

    private fun setKeepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun setRefreshingImage(url: String?) {
        if (url != null) {
            binding.image.load(url)
        }
    }

    private fun createSettingsDialog() {
        settingsDialog = AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.settings)
            .setMessage(R.string.settings_message)
            .setView(dialogSettingsBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            .create()

        settingsDialog.setOnShowListener {
            val saveButton = settingsDialog.getButton(DialogInterface.BUTTON_POSITIVE)
            saveButton.isEnabled = false

            dialogSettingsBinding.imageUrl.setText(currentImageUrl)
            val textWatcher = dialogSettingsBinding.imageUrl.doOnTextChanged { text, _, _, _ ->
                saveButton.isEnabled =
                    !text.isNullOrEmpty() && text.toString().toHttpUrlOrNull() != null
            }

            settingsDialog.setOnDismissListener {
                dialogSettingsBinding.imageUrl.removeTextChangedListener(textWatcher)
            }

            saveButton.setOnClickListener {
                val inputText = dialogSettingsBinding.imageUrl.text.toString()
                viewModel.updateImageUrl(inputText)
                settingsDialog.dismiss()
            }
        }
    }

    private fun showSettingsDialog() {
        settingsDialog.show()
    }

    override fun onDestroy() {
        binding.image.dispose()
        super.onDestroy()
    }

    companion object {
        private const val FADE_IN_OUT_DURATION = 300L
        private const val FADE_OUT_DELAY = 3000L
    }
}