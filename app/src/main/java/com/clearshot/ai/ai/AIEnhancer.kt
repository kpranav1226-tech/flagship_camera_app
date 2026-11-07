package com.clearshot.ai.ai

import java.io.File

/**
 * Contract for AI and neural clarity enhancement models.
 */
interface AIEnhancer {
    /**
     * Applies machine-learned enhancement models (e.g. edge clarity, neural super-resolution).
     */
    suspend fun enhance(inputFile: File): Result<File>
}
