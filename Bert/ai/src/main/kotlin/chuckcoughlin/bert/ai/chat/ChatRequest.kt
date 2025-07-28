/**
 * Copyright 2025. Charles Coughlin
 * MIT License.
 *
 * Chat GPT interface is a simplified version of code by CJCrafter (Collin Barber)
 * @See https://github.com/CJCrafter/ChatGPT-Java-API/blob/master/README.md
 */
package chuckcoughlin.bert.common.ai.chat

data class ChatRequest(val model:String) {
		val input: MutableList<ChatMessage>
		val stream: Boolean

	fun addMessage(cmsg:ChatMessage) {
		input.add(cmsg)
	}

	init {
		input = mutableListOf<ChatMessage>()
		stream = false
	}
}