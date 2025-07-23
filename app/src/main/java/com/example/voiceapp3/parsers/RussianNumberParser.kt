package com.example.voiceapp3.parsers

object RussianNumberParser {
    private val wordToNumber = mapOf(
        "один" to 1, "одна" to 1, "одно" to 1, "единица" to 1, "единицы" to 1, "единицу" to 1,
        "два" to 2, "две" to 2, "двойка" to 2, "двойки" to 2, "двойку" to 2,
        "три" to 3,
        "четыре" to 4,
        "пять" to 5,
        "шесть" to 6,
        "семь" to 7,
        "восемь" to 8,
        "девять" to 9,
        "десять" to 10,
        "одиннадцать" to 11,
        "двенадцать" to 12,
        "тринадцать" to 13,
        "четырнадцать" to 14,
        "пятнадцать" to 15,
        "шестнадцать" to 16,
        "семнадцать" to 17,
        "восемнадцать" to 18,
        "девятнадцать" to 19,
        "двадцать" to 20,
        "тридцать" to 30,
        "сорок" to 40,
        "пятьдесят" to 50,
        "шестьдесят" to 60,
        "семьдесят" to 70,
        "восемьдесят" to 80,
        "девяносто" to 90,
        "сто" to 100
    )

    fun parseRussianNumber(text: String): String {
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