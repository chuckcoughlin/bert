/**
 * Copyright 2025. Charles Coughlin
 * MIT License.
 *
 * Chat GPT interface is a simplified version of code by CJCrafter (Collin Barber)
 * @See https://github.com/CJCrafter/ChatGPT-Java-API/blob/master/README.md
 */
package chuckcoughlin.bert.common.ai.chat

import chuckcoughlin.bert.common.model.Solver.model

data class ChatRequest(val model:String) {
		val input: MutableList<ChatMessage>
		//val maxTokens: Int
		val stream: Boolean

	fun addMessage(cmsg:ChatMessage) {
		input.add(cmsg)
	}

	init {
		input = mutableListOf<ChatMessage>()
		//maxTokens = 500
		stream = false
	}
}