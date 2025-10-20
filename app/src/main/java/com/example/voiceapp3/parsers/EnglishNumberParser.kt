package com.example.voiceapp3.parsers

object EnglishNumberParser {
    private val wordToNumber = mapOf(
        "one" to 1,
        "first" to 1,
        "two" to 2,
        "second" to 2,
        "three" to 3,
        "third" to 3,
        "four" to 4,
        "fourth" to 4,
        "five" to 5,
        "fifth" to 5,
        "six" to 6,
        "sixth" to 6,
        "seven" to 7,
        "seventh" to 7,
        "eight" to 8,
        "eighth" to 8,
        "nine" to 9,
        "ninth" to 9,
        "ten" to 10,
        "tenth" to 10,
        "eleven" to 11,
        "twelve" to 12,
        "thirteen" to 13,
        "fourteen" to 14,
        "fifteen" to 15,
        "sixteen" to 16,
        "seventeen" to 17,
        "eighteen" to 18,
        "nineteen" to 19,
        "twenty" to 20,
        "thirty" to 30,
        "forty" to 40,
        "fifty" to 50,
        "sixty" to 60,
        "seventy" to 70,
        "eighty" to 80,
        "ninety" to 90,
        "hundred" to 100
    )

    fun parseEnglishNumber(text: String): String {
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }
        var total = 0
        var current = 0
        var containsNumbers = false

        for (word in words) {
            val number = wordToNumber[word.lowercase()]
            if (number != null) {
                containsNumbers = true
                if (number >= 100) {
                    total += current * number
                    current = 0
                } else if (number >= 20) {
                    current += number
                } else {
                    current += number
                }
            }
        }

        total += current
        val numericValue = if (containsNumbers && total > 0) total else null

        return if (numericValue != null) {
            words.joinToString(" ") { word ->
                if (wordToNumber.containsKey(word.lowercase())) numericValue.toString() else word
            }.replace("$numericValue $numericValue", "$numericValue")
        } else {
            text
        }
    }
}
