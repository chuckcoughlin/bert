package chuckcoughlin.bert.common.ai.chat


enum class ChatRole {
	/**
	 * ChatGPT has conversational memory. To remember the conversation, we need
	 * to map each message to who sent it. This enum stores the 3 possible users..
	 *
	 * SYSTEM should only send 1 message, and it should be the first message
	 * in the conversation. It set the context.
	 *
	 * @see <a href="https://github.com/f/awesome-chatgpt-prompts">System Message Examples/Ideas</a>
	 *
	 * USER is the human that is asking the questions. After a message from
	 * the user, you should lock the conversation until ASSISTANT replies to
	 * the user's message.
	 */
		SYSTEM,
		USER,
		ASSISTANT,
		DEVELOPER
	;

	companion object {
		/**
		 * Convert the enumeration to a text string for JSON conversion
		 */
		fun toText(user: ChatRole): String {
			var text=""
			when (user) {
				ChatRole.ASSISTANT -> text="assistant"
				ChatRole.DEVELOPER -> text="developer"
				ChatRole.SYSTEM -> text="system"
				ChatRole.USER -> text="user"
			}
			return text
		}
	}
}