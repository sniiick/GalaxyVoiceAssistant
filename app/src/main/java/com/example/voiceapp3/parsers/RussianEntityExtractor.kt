package com.example.voiceapp3.parsers

object RussianEntityExtractor {
    // Units
    private val degreePattern = Regex("град(?:ус(?:ов)?|усе|уса)?", RegexOption.IGNORE_CASE)
    private val percentPattern = Regex("процент(?:ов|а)?", RegexOption.IGNORE_CASE)

    // Actions
    private val increasePatterns = listOf(
        Regex("увелич(?:ить|иваю|ил|ила|ено|ение|ь)"),
        Regex("повыш(?:ать|аю|ал|ала|ение|ай)"),
        Regex("повыс(?:ь|ить)"),
        Regex("прибав(?:ить|лю|ил|ила|ь)"),
        Regex("добав(?:ить|лю|ил|ила|ь)"),
        Regex("усил(?:ить|иваю|ил|ила|ение|ь)"),
        Regex("ускор(?:ить|иваю|ил|ила|ение|ь)"),
        Regex("выше", RegexOption.IGNORE_CASE),
        Regex("больше", RegexOption.IGNORE_CASE),
        Regex("сильнее", RegexOption.IGNORE_CASE),
        Regex("теплее", RegexOption.IGNORE_CASE),
        Regex("горячее", RegexOption.IGNORE_CASE),
        Regex("жарче", RegexOption.IGNORE_CASE),
        Regex("громче", RegexOption.IGNORE_CASE),
    )

    private val decreasePatterns = listOf(
        Regex("уменьш(?:ить|аю|ил|ила|ение|и)"),
        Regex("пониж(?:ать|аю|ал|ала|ение)"),
        Regex("пониз(?:ить|ь)"),
        Regex("сниж(?:ить|аю|ал|ала|ение)"),
        Regex("сниз(?:ить|ь)"),
        Regex("убав(?:ить|лю|ил|ила|ь)"),
        Regex("ослаб(?:ить|иваю|ил|ила|ение|ь)"),
        Regex("замедл(?:ить|иваю|ил|ила|ение|ь)"),
        Regex("холоднее", RegexOption.IGNORE_CASE),
        Regex("прохладнее", RegexOption.IGNORE_CASE),
        Regex("ниже", RegexOption.IGNORE_CASE),
        Regex("меньше", RegexOption.IGNORE_CASE),
        Regex("слабее", RegexOption.IGNORE_CASE),
        Regex("тише", RegexOption.IGNORE_CASE),
    )

    private val setPatterns = listOf(
        Regex("установ(?:ить|лю|ил|ила|ь|и)"),
        Regex("зада(?:ть|ю|л|ла|й)"),
        Regex("выстав(?:ить|лю|ил|ила|ь|и)"),
        Regex("постав(?:ить|лю|ил|ила|ь)"),
        Regex("став(?:ить|лю|ил|ила|ь)"),
        Regex("смен(?:ить|ю|ил|ила|ь|и)"),
        Regex("сдел(?:ать|ай|аю)"),
        Regex("вкл(?:ючи|ючай|ючить)"),
        Regex("откр(?:ой|ыть|ывать)"),
    )

    private val unSetPatterns = listOf(
        Regex("убер(?:и|ать|ай)"),
        Regex("выкл(?:ючи|ючай|ючить)"),
        Regex("откл(?:ючи|ючай|ючить)"),
        Regex("отмен(?:а|и|яй|ять|ить)"),
        Regex("закр(?:ой|ыть|ывать)"),
    )

    // Special values
    private val maxPattern = Regex("макс(?:имум|имальн(?:ый|ая|ое|ые)|наибольш(?:ий|ая|ее|ие)|полн(?:ый|ая|ое|ые))", RegexOption.IGNORE_CASE)
    private val minPattern = Regex("мин(?:имум|имальн(?:ый|ая|ое|ые)|наименьш(?:ий|ая|ее|ие))", RegexOption.IGNORE_CASE)
    private val midPattern = Regex("средн(?:ий|яя|ее)|половин[ауеы]|наполовину?", RegexOption.IGNORE_CASE)

    fun extractEntities(text: String): Map<String, Any> {
        val entities = mutableMapOf<String, Any>()

        extractNumbers(text)?.let {
            entities["value"] = it
            entities["unit"] = "number"
        }
        entities.putAll(extractUnits(text))
        entities.putAll(extractActions(text))
        entities.putAll(handleSpecialValues(text, entities))

        return entities
    }

    private fun extractUnits(text: String): Map<String, Any> {
        return when {
            degreePattern.containsMatchIn(text) -> mapOf("unit" to "degree")
            percentPattern.containsMatchIn(text) -> mapOf("unit" to "percent")
            else -> emptyMap()
        }
    }

    private fun extractNumbers(text: String): Int? {
        return Regex("\\d+").find(text)?.value?.toIntOrNull()
    }

    private fun extractActions(text: String): Map<String, Any> {
        return when {
            increasePatterns.any { it.containsMatchIn(text) } -> mapOf("action" to "increase")
            decreasePatterns.any { it.containsMatchIn(text) } -> mapOf("action" to "decrease")
            setPatterns.any { it.containsMatchIn(text) } -> mapOf("action" to "set")
            unSetPatterns.any { it.containsMatchIn(text) } -> mapOf("action" to "unset")
            else -> emptyMap()
        }
    }

    private fun handleSpecialValues(text: String, currentEntities: Map<String, Any>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        when {
            maxPattern.containsMatchIn(text) -> {
                result["value"] = 100
                result["unit"] = "percent"
            }
            minPattern.containsMatchIn(text) -> {
                result["value"] = 1
                result["unit"] = "percent"
            }
            midPattern.containsMatchIn(text) -> {
                result["value"] = 50
                result["unit"] = "percent"
            }
        }

        return result.ifEmpty { currentEntities }
    }
}