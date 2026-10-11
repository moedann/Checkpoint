package com.example.nanisini

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private lateinit var bridge: FlutterBridge
    private lateinit var repository: NativeSettingsRepository

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        repository = NativeSettingsRepository(applicationContext)
        bridge = FlutterBridge(this, repository)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME).setMethodCallHandler(bridge)
    }

    override fun onStart() {
        super.onStart()
        if (::repository.isInitialized && repository.readPauseSummary().mode == PauseMode.UNTIL_NEXT_OPEN) {
            repository.resumeFeature()
        }
    }

    companion object {
        private const val CHANNEL_NAME = "nanishini/bridge"
    }
}
