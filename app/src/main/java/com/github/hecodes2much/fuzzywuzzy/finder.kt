package com.github.hecodes2much.fuzzywuzzy

import com.github.hecodes2much.mlauncher.data.AppModel
import java.util.*

fun scoreApp(app: AppModel, searchChars: String): Float {
    val appChars = app.appAlias.ifEmpty {
        app.appLabel
    }

    return calculateFuzzyScore(
        appChars.uppercase(Locale.getDefault())
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[-_+,.]"), ""),
        searchChars.uppercase()
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[-_+,.]"), "")
    )
}

fun calculateFuzzyScore(s1: String, s2: String): Float {
    val m = s1.length
    val n = s2.length
    var matchCount = 0
    var s1Index = 0

    // Iterate over each character in s2 and check if it exists in s1
    for (i in 0 until n) {
        val c2 = s2[i]
        var found = false

        // Start searching for c2 from the current s1Index
        for (j in s1Index until m) {
            val c1 = s1[j]
            if (c1 == c2) {
                found = true
                // Update s1Index to the next position for the next iteration
                s1Index = j + 1
                break
            }
        }

        // If the current character in s2 is not found in s1, return a score of 0
        if (!found) {
            return 0f
        }

        // Increment the match count
        matchCount++
    }

    // Calculate the score as the ratio of matched characters to the longer string length
    val maxLength = maxOf(m, n)
    return matchCount.toFloat() / maxLength
}