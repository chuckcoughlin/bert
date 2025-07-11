/**
 * Copyright 2025. Charles Coughlin
 * MIT License.
 *
 * Our Chat GPT interface is a simplified version of code by CJCrafter (Collin Barber)
 * @See https://github.com/CJCrafter/ChatGPT-Java-API/blob/master/README.md
 */

import chuckcoughlin.bert.common.ai.chat.ChatRequest
import chuckcoughlin.bert.common.ai.completion.CompletionRequest
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.org.slf4j.internal.LoggerFactory
import org.jetbrains.annotations.Contract
import java.util.logging.FileHandler

/**
 * Our usage of the OpenAI API.
 */

object OpenAI {
	val apiKey:String

	init {
		apiKey = System.getenv("CHATGPT_KEY")
	}
}