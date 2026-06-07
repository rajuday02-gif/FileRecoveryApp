package com.recovery.filecarver

class RecoveryEngine {
    companion object {
        init {
            System.loadLibrary("recovery-core")
        }
    }

    external fun initEngine()
    external fun startScan(inputPath: String, outputDir: String, maxSize: Long)
    external fun stopScan()
    external fun getProgress(): ScanProgress
    external fun getRecoveredFiles(): Array<RecoveredFileInfo>
    external fun enableRootMode(enable: Boolean)
    external fun hasRootAccess(): Boolean
    external fun destroyEngine()
}
