/**
 * Copyright 2025. Charles Coughlin
 * MIT License.
 *
 * Chat GPT interface is a simplified version of code by CJCrafter (Collin Barber)
 * @See https://github.com/CJCrafter/ChatGPT-Java-API/blob/master/README.md
 */
package chuckcoughlin.bert.common.ai.chat

import com.google.gson.annotations.SerializedName

class ChatResponse() {
	var id:String
	@SerializedName("object")
	var obj:String
	var status:String
	var error:String?
	var model:String
	var user:String
	val output:List<ChatOutput>
	var totalTokens:Int

	init {
		id = "0"
		obj="undefined"
		status="initialized"
		error = null
		model = ""
		user = ""
		output = mutableListOf<ChatOutput>()
		totalTokens = 0
	}
}