package com.example.voiceapp3.handlers

import com.example.voiceapp3.PredictionResult

fun PredictionResult.getInt(name: String): Int? = entities[name] as? Int
fun PredictionResult.getString(name: String): String? = entities[name] as? String
fun PredictionResult.getBoolean(name: String): Boolean? = entities[name] as? Boolean
fun PredictionResult.getAction(): String? = getString("action")
fun PredictionResult.getUnit(): String? = getString("unit")
fun PredictionResult.getValue(): Int? = getInt("value")