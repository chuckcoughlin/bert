/**
 * Copyright 2025. Charles Coughlin
 * MIT License.
 *
 * Chat GPT interface is a simplified version of code by CJCrafter (Collin Barber)
 * @See https://github.com/CJCrafter/ChatGPT-Java-API/blob/master/README.md
 */
package chuckcoughlin.bert.common.ai.chat

class ChatMessage {
	/**
	 * ChatGPT's biggest innovation is its conversation memory. To remember the
	 * conversation, we need to map each message to who sent it. This data class
	 * wraps a message with the user who sent the message.
	 *
	 * @property role The user who sent this message.
	 * @property content The string content of the message.
	 * @see ChatUser
	 */
	data class ChatMessage (
			var role: ChatUser,
			var content: String?, // JsonInclude here is important for tools
			var toolCalls: List<ToolCall>? = null,
			var toolCallId: String? = null,
	) {
		init {
			if (role == ChatUser.TOOL)
				requireNotNull(toolCallId) { "toolCallId must be set when role is TOOL" }
			if (role != ChatUser.ASSISTANT)
				require(toolCalls == null) { "Only ChatUser.ASSISTANT can make toolCalls" }
		}

		/**
		 * Returns true if this message has tool calls.
		 */
		fun hasToolCalls() = toolCalls != null

		companion object {

			/**
			 * Returns a new [ChatMessage] using [ChatUser.SYSTEM].
			 */
			@JvmStatic
			fun String.toSystemMessage(): ChatMessage {
				return ChatMessage(ChatUser.SYSTEM, this)
			}

			/**
			 * Returns a new [ChatMessage] using [ChatUser.USER].
			 */
			@JvmStatic
			fun String.toUserMessage(): ChatMessage {
				return ChatMessage(ChatUser.USER, this)
			}

			/**
			 * Returns a new [ChatMessage] using [ChatUser.ASSISTANT].
			 */
			@JvmStatic
			fun String.toAssistantMessage(): ChatMessage {
				return ChatMessage(ChatUser.ASSISTANT, this)
			}

			/**
			 * Returns a new [ChatMessage] using [ChatUser.TOOL].
			 */
			@JvmStatic
			fun String.toToolMessage(toolCallId: String): ChatMessage {
				return ChatMessage(ChatUser.TOOL, this, toolCallId = toolCallId)
			}
		}
}