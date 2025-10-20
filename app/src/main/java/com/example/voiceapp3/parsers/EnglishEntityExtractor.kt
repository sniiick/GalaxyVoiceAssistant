package com.example.voiceapp3.parsers

object EnglishEntityExtractor {
    // Units
    private val degreePattern = Regex("degree(s)?", RegexOption.IGNORE_CASE)
    private val percentPattern = Regex("percent(age)?", RegexOption.IGNORE_CASE)

    // Actions
    private val increasePatterns = listOf(
        Regex("increase"),
        Regex("raise"),
        Regex("add"),
        Regex("strengthen"),
        Regex("accelerate"),
        Regex("higher"),
        Regex("more"),
        Regex("stronger"),
        Regex("warmer"),
        Regex("hotter"),
        Regex("louder"),
        Regex("brighter"),
    )

    private val decreasePatterns = listOf(
        Regex("decrease"),
        Regex("lower"),
        Regex("reduce"),
        Regex("weaken"),
        Regex("decelerate"),
        Regex("colder"),
        Regex("cooler"),
        Regex("below"),
        Regex("less"),
        Regex("weaker"),
        Regex("quieter"),
        Regex("dimmer"),
    )

    private val setPatterns = listOf(
        Regex("set"),
        Regex("assign"),
        Regex("put"),
        Regex("change"),
        Regex("make"),
        Regex("turn on"),
        Regex("open"),
        Regex("lower"),
        Regex("launch"),
        Regex("start"),
    )

    private val unSetPatterns = listOf(
        Regex("remove"),
        Regex("turn off"),
        Regex("disable"),
        Regex("cancel"),
        Regex("close"),
        Regex("raise"),
        Regex("shut down"),
    )

    // Direction and Position
    private val right = Regex("right|passenger", RegexOption.IGNORE_CASE)
    private val rightAll = Regex("all right|right all", RegexOption.IGNORE_CASE)
    private val left = Regex("left|driver|pilot", RegexOption.IGNORE_CASE)
    private val leftAll = Regex("all left|left all", RegexOption.IGNORE_CASE)

    private val front = Regex("front", RegexOption.IGNORE_CASE)
    private val frontAll = Regex("front all|all front", RegexOption.IGNORE_CASE)
    private val rear = Regex("rear|back", RegexOption.IGNORE_CASE)
    private val rearAll = Regex("rear all|all rear|back all|all back", RegexOption.IGNORE_CASE)
    private val all = Regex("all|every", RegexOption.IGNORE_CASE)

    // Special values
    private val maxPattern = Regex("max(imum)?|full", RegexOption.IGNORE_CASE)
    private val minPattern = Regex("min(imum)?", RegexOption.IGNORE_CASE)
    private val midPattern = Regex("middle|half", RegexOption.IGNORE_CASE)

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
