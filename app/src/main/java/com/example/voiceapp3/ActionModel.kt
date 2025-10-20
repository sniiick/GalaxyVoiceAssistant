package com.example.voiceapp3

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtUtil
import android.content.Context
import android.util.Log
import com.example.voiceapp3.parsers.EnglishEntityExtractor
import com.example.voiceapp3.parsers.EnglishNumberParser
import java.io.File
import java.io.FileOutputStream

class ActionModel(context: Context, modelPath: String) {
    private val TAG: String = "ActionModel"
    private var vocab: List<String>
    private var path: String = modelPath
    private var ortEnv: OrtEnvironment
    private var ortSession: OrtSession
    private val wordToId: Map<String, Int>
    private val labelEncoder = listOf(
        "ac_control", "change_ac_temp", "change_fan_speed",
        "open_app", "seat_massage", "trunk_control", "window_control"
    )

    init {
        vocab = context.assets.open("$path/vocab.txt").bufferedReader().useLines {
            it.toList()
        }
        wordToId = vocab.withIndex().associate { it.value to it.index }

        ortEnv = OrtEnvironment.getEnvironment()
        val modelDir = File(context.filesDir, path).apply {
            mkdirs()
        }
        val modelFile = File(modelDir, "model.onnx")

        try {
            if (!modelFile.exists()) {
                context.assets.open("model-cmd/model.onnx").use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            ortSession = ortEnv.createSession(modelFile.absolutePath).apply {
                Log.i(TAG, "Model loaded successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, """
            MODEL LOAD FAILED!
            - File exists: ${modelFile.exists()}
            - File path: ${modelFile.absolutePath}
            - File size: ${modelFile.length()} bytes
            - Assets: ${context.assets.list("model-cmd")?.joinToString()}
            """.trimIndent())
            throw e
        }

    }

    fun tokenize(text: String): TokenizerInput {
        val tokens = listOf("[CLS]") + text.split(" ") + listOf("[SEP]")
        val inputIds = tokens.map { wordToId[it] ?: wordToId["[UNK]"]!! }
        val attentionMask = List(inputIds.size) { 1 }

        return TokenizerInput(
            inputIds = inputIds.map { it.toLong() }.toTypedArray().toLongArray(),
            attentionMask = attentionMask.map { it.toLong() }.toTypedArray().toLongArray()
        )
    }
    fun predict(command: String): PredictionResult {
        try {
            val normalized = normalizeText(command)
            val entities = EnglishEntityExtractor.extractEntities(normalized)

            // 1. First try to determine intent by keywords
            val keywordIntent = detectIntentByKeywords(normalized)

            // 2. Only run model prediction if no clear keyword match
            val (intent, confidence) = keywordIntent?.let {
                Pair(it, 1.0f) // 100% confidence for keyword matches
            } ?: run {
                val inputs = tokenize(normalized)
                val inputIds = OrtUtil.reshape(inputs.inputIds, longArrayOf(1, inputs.inputIds.size.toLong()))
                val attentionMask = OrtUtil.reshape(inputs.attentionMask, longArrayOf(1, inputs.attentionMask.size.toLong()))

                val results = ortSession.run(
                    mapOf(
                        "input_ids" to OnnxTensor.createTensor(ortEnv, inputIds),
                        "attention_mask" to OnnxTensor.createTensor(ortEnv, attentionMask)
                    )
                )

                val logits = (results[0].value as Array<FloatArray>)[0]
                val intentId = logits.indices.maxByOrNull { logits[it] } ?: 0
                val conf = kotlin.math.exp(logits[intentId].toDouble()) / logits.sumOf {
                    kotlin.math.exp(it.toDouble())
                }
                Pair(labelEncoder[intentId], conf.toFloat())
            }

            return PredictionResult(
                text = command,
                normalizedText = normalized,
                intent = intent,
                confidence = confidence,
                entities = entities
            )
        } catch (e: Exception) {
            throw PredictionException("Prediction failed", e)
        }
    }

    private fun detectIntentByKeywords(text: String): String? {
        return when {
            Regex("degree(s)?|temperature|warm|hot|cold|cool").containsMatchIn(text) -> "change_ac_temp"
            Regex("fan|blow").containsMatchIn(text) -> "change_fan_speed"
            Regex("ventilation").containsMatchIn(text) -> "seat_ventilation"
            Regex("heating|heat").containsMatchIn(text) -> "seat_heat"
            Regex("massage").containsMatchIn(text) -> "seat_massage"
            Regex("charge|engine|motor").containsMatchIn(text) -> "fuel_charge"
            Regex("trunk|luggage").containsMatchIn(text) -> "trunk_control"
            Regex("brightness|screen|monitor|display|tablet").containsMatchIn(text) -> "screen_brightness"
            Regex("window|curtain|sunroof|panorama").containsMatchIn(text) -> "window_control"
            Regex("gas|fuel|fill|tank").containsMatchIn(text) -> "fuel_door_open"
            Regex("say|tell").containsMatchIn(text) -> "play_text"
            Regex("low beam|headlight(s)?|fog|parking light(s)?").containsMatchIn(text) -> "exterior_light_control"
            Regex("light").containsMatchIn(text) -> "light_control"
            Regex("mode|drive mode|driving|hybrid|electric|sport").containsMatchIn(text) -> "drive_mode"
            Regex("cat|fuck").containsMatchIn(text) -> "play_sound"
            else -> null
        }
    }

    private fun normalizeText(text: String): String {
        val normalizedText = EnglishNumberParser.parseEnglishNumber(text.trim())
        return normalizedText
    }

    fun cleanup() {
        ortSession.close()
        ortEnv.close()
    }

}

data class TokenizerInput(
    val inputIds: LongArray,
    val attentionMask: LongArray
)
data class PredictionResult(
    val text: String,
    val normalizedText: String,
    val intent: String,
    val confidence: Float,
    val entities: Map<String, Any>
)
class PredictionException(message: String, cause: Throwable?) : Exception(message, cause)
