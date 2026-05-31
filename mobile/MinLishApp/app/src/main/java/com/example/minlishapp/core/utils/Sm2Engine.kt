package com.example.minlishapp.core.utils

import kotlin.math.ceil
import kotlin.math.max

object Sm2Engine {
    /**
     * Calculates the next review parameters based on the SM-2 algorithm.
     * @param repetitions The number of consecutive successful reviews.
     * @param easeFactor The easiness factor of the word (default is 2.5).
     * @param intervalDays The previous interval in days.
     * @param quality The user's response quality (0 = Again, 3 = Hard, 4 = Good, 5 = Easy).
     * @return A Triple containing (newRepetitions, newEaseFactor, newIntervalDays).
     */
    fun calculate(
        repetitions: Int,
        easeFactor: Double,
        intervalDays: Int,
        quality: Int
    ): Triple<Int, Double, Int> {
        val newRepetitions: Int
        val newIntervalDays: Int
        
        // Update Ease Factor
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        val q = quality.coerceIn(0, 5)
        var newEaseFactor = easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        if (newEaseFactor < 1.3) {
            newEaseFactor = 1.3
        }

        if (q >= 3) {
            newRepetitions = repetitions + 1
            newIntervalDays = when (newRepetitions) {
                1 -> 1
                2 -> 6
                else -> ceil(intervalDays * newEaseFactor).toInt()
            }
        } else {
            // Again or failed recollection
            newRepetitions = 0
            newIntervalDays = 1 // Review next day (or within 1 min in active session)
        }

        return Triple(newRepetitions, newEaseFactor, newIntervalDays)
    }
}
