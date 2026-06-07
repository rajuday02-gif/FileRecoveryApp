package com.recovery.filecarver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File

sealed class RecoveryState {
    object Idle : RecoveryState()
    object Initializing : RecoveryState()
    data class Scanning(val progress: ScanProgress) : RecoveryState()
    data class Complete(val filesFound: List<RecoveredFileInfo>) : RecoveryState()
    data class Error(val message: String) : RecoveryState()
}

class RecoveryViewModel : ViewModel() {

    private val engine = RecoveryEngine()
    private val storageManager = StorageAccessManager(null) // TODO: pass context
    private val logger = RecoveryLogger(null) // TODO: pass context

    private val _state = MutableStateFlow<RecoveryState>(RecoveryState.Idle)
    val state: StateFlow<RecoveryState> = _state

    private val _recoveredFiles = MutableStateFlow<List<RecoveredFileInfo>>(emptyList())
    val recoveredFiles: StateFlow<List<RecoveredFileInfo>> = _recoveredFiles

    init {
        engine.initEngine()
    }

    fun startRecovery(inputPath: String, outputDir: String, maxSize: Long = 500 * 1024 * 1024) {
        viewModelScope.launch {
            _state.emit(RecoveryState.Initializing)

            try {
                engine.startScan(inputPath, outputDir, maxSize)

                // Monitor progress
                while (true) {
                    val progress = engine.getProgress()
                    _state.emit(RecoveryState.Scanning(progress))

                    if (progress.percentComplete >= 100) break
                    delay(500) // Update every 500ms
                }

                // Get final results
                val files = engine.getRecoveredFiles().toList()
                _recoveredFiles.emit(files)
                _state.emit(RecoveryState.Complete(files))

                logger?.logRecoveryComplete(files.size, 0)
            } catch (e: Exception) {
                _state.emit(RecoveryState.Error(e.message ?: "Unknown error"))
                logger?.logError("Recovery failed", e)
            }
        }
    }

    fun stopRecovery() {
        engine.stopScan()
        _state.value = RecoveryState.Idle
    }

    fun checkRootAccess(): Boolean {
        return engine.hasRootAccess()
    }

    override fun onCleared() {
        engine.destroyEngine()
        super.onCleared()
    }
}
