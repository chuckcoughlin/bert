/**
 * Copyright 2025. Charles Coughlin
 * MIT License.
 *
 * Our Chat GPT interface is a simplified version of code by CJCrafter (Collin Barber)
 * @See https://github.com/CJCrafter/ChatGPT-Java-API/blob/master/README.md
 */

/**
 * Our usage of the OpenAI API.
 */

object OpenAI {
	val apiKey:String



	init {
		apiKey = System.getenv("CHATGPT_KEY")
	}
}