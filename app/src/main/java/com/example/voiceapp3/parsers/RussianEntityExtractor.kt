package com.example.voiceapp3.parsers

object RussianEntityExtractor {
    // Units
    private val degreePattern = Regex("град(?:ус(?:ов)?|уса|усы)?", RegexOption.IGNORE_CASE)
    private val percentPattern = Regex("процент(?:ов|а|ы)?", RegexOption.IGNORE_CASE)

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
        Regex("ярче", RegexOption.IGNORE_CASE),
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
        Regex("темнее", RegexOption.IGNORE_CASE),
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
        Regex("опус(?:ти|кай|тить)"),
        Regex("запус(?:ти|кай|тить)|запуск"),
        Regex("заве(?:ди|сти)|заводи"),
    )

    private val unSetPatterns = listOf(
        Regex("убер(?:и|ать|ай)|убрать|убирать"),
        Regex("выкл(?:ючи|ючай|ючить|ючать)"),
        Regex("откл(?:ючи|ючай|ючить)|откл"),
        Regex("отмен(?:а|и|яй|ять|ить)"),
        Regex("закр(?:ой|ыть|ывать)"),
        Regex("под(?:ними|нимай|нимать|нять)"),
        Regex("загл(?:уши|ушить)|пога(?:си|сить)|заглохн(?:и|уть)"),
    )

    // Direction and Position
    private val right = Regex("прав(?:о|ый|ая|ое|ому|ой)|справ[оа]|пассажир(?:а|у|ское|ская|скому|ы|ского|ам|)", RegexOption.IGNORE_CASE)
    private val rightAll = Regex("правые|все.*правые|правые.*все|все.*справ[оа]|справ[оа].*все", RegexOption.IGNORE_CASE)
    private val left = Regex("лев(?:о|ый|ая|ое|ому|ой)|слев[ао]|водител(?:ь|я|ю|ем|ское|ская|скому|ской)|пилот(?:а|у|ом)", RegexOption.IGNORE_CASE)
    private val leftAll = Regex("левые|все.*левые|левые.*все|все.*слев[оа]|слев[оа].*все", RegexOption.IGNORE_CASE)

    private val front = Regex("передн(?:ий|яя|ее|ему|им|ей)|спереди", RegexOption.IGNORE_CASE)
    private val frontAll = Regex("передние|все.*передние|передние.*все|все.*спереди|спереди.*все", RegexOption.IGNORE_CASE)
    private val rear = Regex("задн(?:ий|яя|ее|ему|им|ей)|сзади", RegexOption.IGNORE_CASE)
    private val rearAll = Regex("задние|все.*задние|задние.*все|все.*сзади|сзади.*все", RegexOption.IGNORE_CASE)
    private val all = Regex("вс[её]|весь|всем|всех|всю", RegexOption.IGNORE_CASE)

    // Special values
    private val maxPattern = Regex("макс(?:имум|имальн)|наибольш|полн(?:ый|ая|ое|ые|остью)", RegexOption.IGNORE_CASE)
    private val minPattern = Regex("мин(?:имум|имальн)|наименьш", RegexOption.IGNORE_CASE)
    private val midPattern = Regex("средн(?:ий|яя|ее|юю|е)|середин|половин|наполовин", RegexOption.IGNORE_CASE)

    fun extractEntities(text: String): Map<String, Any> {
        val entities = mutableMapOf<String, Any>()

        extractNumbers(text)?.let {
            entities["value"] = it
            entities["unit"] = "number"
        }
        entities.putAll(extractUnits(text))
        entities.putAll(extractActions(text))
        entities.putAll(handleSpecialValues(text, entities))
        entities.putAll(extractDirectionPosition(text, entities))

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

    private fun extractDirectionPosition(text: String, currentEntities: Map<String, Any>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        // handle left right direction
        // Handle combined "all + position" cases first
        when {
            frontAll.containsMatchIn(text) -> {
                result["position"] = "front"
                result["direction"] = "both"
                return result.ifEmpty { currentEntities }
            }
            rearAll.containsMatchIn(text) -> {
                result["position"] = "rear"
                result["direction"] = "both"
                return result.ifEmpty { currentEntities }
            }
            leftAll.containsMatchIn(text) -> {
                result["position"] = "both"
                result["direction"] = "left"
                return result.ifEmpty { currentEntities }
            }
            rightAll.containsMatchIn(text) -> {
                result["position"] = "both"
                result["direction"] = "right"
                return result.ifEmpty { currentEntities }
            }
        }

        when {
            front.containsMatchIn(text) -> {
                result["position"] = "front"
            }
            rear.containsMatchIn(text) -> {
                result["position"] = "rear"
            }
        }

        when {
            right.containsMatchIn(text) -> {
                result["direction"] = "right"
            }
            left.containsMatchIn(text) -> {
                result["direction"] = "left"
            }
        }

        // finally handle all
        if (all.containsMatchIn(text)) {
            if (!result.containsKey("position")) result["position"] = "both"
            if (!result.containsKey("direction")) result["direction"] = "both"
        }

        return result.ifEmpty { currentEntities }
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