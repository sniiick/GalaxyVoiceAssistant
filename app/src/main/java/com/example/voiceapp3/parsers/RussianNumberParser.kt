package com.example.voiceapp3.parsers

object RussianNumberParser {
    private val wordToNumber = mapOf(
        "один" to 1, "одна" to 1, "одно" to 1, "единица" to 1, "единицы" to 1, "единицу" to 1,
        "первый" to 1, "первое" to 1, "первая" to 1, "первую" to 1,
        "два" to 2, "две" to 2, "двойка" to 2, "двойки" to 2, "двойку" to 2,
        "второй" to 2, "второе" to 2, "вторая" to 2, "вторую" to 2,
        "три" to 3,
        "третья" to 3, "третью" to 3, "третий" to 3, "третье" to 3,
        "четыре" to 4,
        "четвертый" to 4, "четвертое" to 4, "четвертая" to 4, "четвертую" to 4,
        "четвёртый" to 4, "четвёртое" to 4, "четвёртая" to 4, "четвёртую" to 4,
        "пять" to 5,
        "пятый" to 5, "пятое" to 5, "пятая" to 5, "пятую" to 5,
        "шесть" to 6,
        "шестой" to 6, "шестое" to 6, "шестая" to 6, "шестую" to 6,
        "семь" to 7,
        "седьмой" to 7, "седьмое" to 7, "седьмая" to 7, "седьмую" to 7,
        "восемь" to 8,
        "восьмой" to 8, "восьмое" to 8, "восьмая" to 8, "восьмую" to 8,
        "девять" to 9,
        "девятый" to 9, "девятое" to 9, "девятая" to 9, "девятую" to 9,
        "десять" to 10, "десятка" to 10, "червонец" to 10,
        "десятый" to 10, "десятое" to 10, "десятая" to 10, "десятую" to 10,
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