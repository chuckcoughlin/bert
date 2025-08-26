
import chuckcoughlin.bert.common.ai.chat.ChatMessage
import chuckcoughlin.bert.common.ai.chat.ChatRequest
import chuckcoughlin.bert.common.ai.chat.ChatResponse
import chuckcoughlin.bert.common.ai.chat.ChatRole
import chuckcoughlin.bert.common.message.BottleConstants
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.RobotModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.serialization.json.Json
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import java.util.logging.Logger


/**
 * Copyright 2025. Charles Coughlin
 * MIT License.
 *
 * Our Chat GPT interface is a simplified version of code by CJCrafter (Collin Barber)
 * See https://github.com/CJCrafter/ChatGPT-Java-API/blob/master/README.md
 */

/**
 * Our usage of the OpenAI API.
 */

object OpenAI {
	private val apiKey:String
	private val gson:Gson
	val model:String

	fun createChatRequest(msg: MessageBottle) : ChatRequest {
		val imsg = ChatMessage(ChatRole.DEVELOPER.name.lowercase(),INSTRUCTION)
		val cmsg = ChatMessage(ChatRole.USER.name.lowercase(),msg.text)
		val request = ChatRequest(model)
		request.addMessage(imsg)
		request.addMessage(cmsg)
		return request
	}

	fun createHttpRequest() : HttpClient {
		val client = HttpClient(CIO) {
			install(ContentNegotiation) {
				json(Json {
					prettyPrint = true
					isLenient = true
				})
			}
			expectSuccess = true
			engine {
				// this: CIOEngineConfig
				maxConnectionsCount = 100
				endpoint {
					// this: EndpointConfig
					maxConnectionsPerRoute = 20
					pipelineMaxSize = 20
					keepAliveTime = 5000
					connectTimeout = 5000
					connectAttempts = 5
				}
			}
		}
		return client
	}

	suspend fun executeRequest(client:HttpClient,req:ChatRequest): HttpResponse {
		val json = gson.toJson(req)
		if(DEBUG) LOGGER.info(String.format("%s.executeRequest: \n%s", CLSS, json))
		val response= client.post("https://api.openai.com/v1/responses") {
			headers {
				append(HttpHeaders.Accept, "application/json")
				append(HttpHeaders.Authorization, "Bearer "+apiKey)
				append(HttpHeaders.UserAgent, "ktor client")
			}
			contentType(ContentType.Application.Json)
			setBody(json)
		}
		return response
	}

	// Replace the syntax error with a response from OpenAI
	suspend fun updateRequestMessage(request:MessageBottle,response:HttpResponse) : MessageBottle {
		val body = response.bodyAsText()
		if(DEBUG) LOGGER.info(String.format("%s.updateRequestMessage: response = \n%s", CLSS, body))
		val chatRsp = gson.fromJson(body,ChatResponse::class.java)
		val msgRsp = request
		msgRsp.text = chatRsp.output[0].content[0].text
		msgRsp.error = BottleConstants.NO_ERROR
		return msgRsp
	}

	private val CLSS = "OpenAi"
	private val DEBUG : Boolean
	private val LOGGER = Logger.getLogger(CLSS)
	const val INSTRUCTION = "Answer like a robot"

	init {
		DEBUG = RobotModel.debug.contains(ConfigurationConstants.DEBUG_OPEN_AI)
		apiKey = System.getenv("CHATGPT_KEY")
		model = "gpt-3.5-turbo"
		gson = GsonBuilder().setPrettyPrinting().create()
	}
}