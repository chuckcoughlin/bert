/**
 * Copyright 2025. Charles Coughlin
 * MIT License.
 *
 * Chat GPT interface is a simplified version of code by CJCrafter (Collin Barber)
 * @See https://github.com/CJCrafter/ChatGPT-Java-API/blob/master/README.md
 */
package chuckcoughlin.bert.common.ai.chat

import chuckcoughlin.bert.common.model.Solver.model

data class ChatRequest(val text:String,val user:ChatUser) {
		var messages: MutableList<String>
		val maxTokens: Int
		var model:     String


	init {
		messages = mutableListOf<String>()
		messages.add(text)
		maxTokens = 3
		model = "gpt-3.5-turbo"
	}
}