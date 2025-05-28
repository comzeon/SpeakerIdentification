package com.example.speakeridentification.model

import kotlin.math.sqrt

object FeatureComparer {

    fun calculateSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        require(vec1.size == vec2.size) { "Vector dimensions do not match" }

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        norm1 = sqrt(norm1)
        norm2 = sqrt(norm2)

        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (norm1 * norm2)
        } else {
            0f
        }
    }

    fun isMatch(similarity: Float, threshold: Float): Boolean {
        return similarity >= threshold
    }
}