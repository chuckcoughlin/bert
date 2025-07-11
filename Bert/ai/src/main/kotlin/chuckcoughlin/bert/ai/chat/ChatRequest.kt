/**
 * Copyright 2025. Charles Coughlin
 * MIT License.
 *
 * Chat GPT interface is a simplified version of code by CJCrafter (Collin Barber)
 * @See https://github.com/CJCrafter/ChatGPT-Java-API/blob/master/README.md
 */
package chuckcoughlin.bert.common.ai.chat

data class ChatRequest (
		var messages: MutableList<ChatMessage>,
		var model: String
		var frequencyPenalty: Float? = null,
		var logitBias: MutableMap<String, Float>? = null,
		var maxTokens: Int? = null,
		var n: Int? = null,
		var presencePenalty: Float? = null,
		var responseFormat: ChatResponseFormat? = null,
		var seed: Int? = null,
		var stop: String? = null,
		var temperature: Float? = null,
		var topP: Float? = null,
		var tools: MutableList<Tool>? = null
		var toolChoice: ToolChoice? = null
		var user: String? = null



	init {
		messages = mutableListOf<ChatMessage>()
		model = "gpt-3.5-turbo"
		toolChoice = ToolChoice.None
	}
}