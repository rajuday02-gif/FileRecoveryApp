package com.recovery.filecarver

class CarverEngine {
    companion object {
        init {
            System.loadLibrary("filecarver")
        }
    }

    external fun initCarver()
    external fun startCarving(inputPath: String, outputDir: String, maxSize: Int, fileTypes: Array<String>)
    external fun getRecoveredCount(): Int
    external fun getRecoveredFiles(): Array<String>
    external fun stopCarving()
    external fun destroyCarver()
}
