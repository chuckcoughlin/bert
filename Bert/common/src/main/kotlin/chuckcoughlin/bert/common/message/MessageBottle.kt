/**
 * Copyright 2022 Charles Coughlin. All rights reserved.
 * MIT License
 */
package chuckcoughlin.bert.common.message

import chuckcoughlin.bert.common.BottleConstants
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.*
import java.io.IOException
import java.io.Serializable
import java.util.logging.Logger

/**
 * This class represents the contents of requests and responses that are sent
 * across sockets between parts of the robot. it becomes serialized into JSON.
 *
 * There is no intrinsic difference between requests and
 * responses. We leave it to context to determine which is which.
 *
 * Leave members public to be accessible via Reflection. We use
 * fetch/assign instead of get/set for the shortcut methods that
 * access the properties to avoid confusion by the JSON mapper.
 */
data class MessageBottle (val type:RequestType) : Serializable {
    var properties : MutableMap<String, String> // Multiple properties for a single motor
    var jointValues : MutableMap<String, String> // A single property for multiple motors

    var id: Long = 0

    /**
     * Set the number of millisecs that the motion commanded by this
     * request is expected to last. Often this represents the required
     * time buffer between subsequent commands, especially those affecting
     * the same sub-chain.
     * @param period ~ msecs
     */
    var duration: Long = 0 // ~msecs
    var responderCount = 0 // Number of controllers/motors that have contributed

    /**
     * Increment the responder count by a specified value.
     * @param increment
     * @return the new count
     */
    fun incrementResponderCount(increment: Int): Int {
        responderCount = responderCount + increment
        return responderCount
    }

    /**
     * Increment the responder count by 1.
     * @return the new count
     */
    fun incrementResponderCount(): Int {
        responderCount = responderCount + 1
        return responderCount
    }

    fun getJointValue(joint: String, defaultValue: String): String {
        var result = defaultValue
        var value = jointValues[joint]
        if (value != null) result = value!!
        return result
    }

    fun setJointValue(joint: String, value: String) {
        jointValues[joint] = value
    }

    fun getProperty(key: String, defaultValue: String): String {
        var result = defaultValue
        var value = properties[key]
        if (value != null) result = value!!
        return result
    }

    fun setProperty(key: String, value: String) {
        properties[key] = value
    }

    /**
     * Use this method to clear the message properties. This conveniently
     * allows re-use of the message object when issuing a new command.
     */
    fun clear() {
        properties.clear()
    }

    /**
     * This is a convenience method retrieve an error message property.
     * The message should be suitable for direct display to the user.
     *
     * @return an error message. If there is no error message, return null.
     */
    fun fetchError(): String {
        return getProperty(BottleConstants.ERROR, BottleConstants.NO_ERROR)
    }

    /**
     * This is a convenience method to set a string suitable for audio
     * that indicates an error. If an error is present no further
     * processing is valid.
     *
     * @param msg a message suitable to be played for the user.
     */
    fun assignError(msg: String) {
        setProperty(BottleConstants.ERROR, msg)
    }

    /**
     * For a message that is a request, the request type should be set using
     * the setter supplied in this class.
     *
     * @return the RequestType. If not set, return NONE.
     */
    fun fetchRequestType(): RequestType {
        var type = RequestType.NONE
        val prop = getProperty(BottleConstants.TYPE, type.name)
        type = RequestType.valueOf(prop)
        return type
    }

    /**
     * For a message that is a request, use this method to set its type. This
     * is our way of enforcing a fixed vocabulary.
     *
     * @param type the type of request.
     */
    fun assignRequestType(type: RequestType) {
        setProperty(BottleConstants.TYPE, type.name)
    }

    /**
     * Set the number of seconds past the execution time of this message
     * that a subsequent message must wait before it can execute. This
     * most likely represents the length of time needed to complete the
     * commanded motion.
     * @param period ~ secs
     */
    fun assignSecondsDuration(period: Double) {
        duration = (period * 1000).toLong()
    }

    /**
     * Set the pronounceable text for a response
     * @param text a speech-compatible response
     */
    fun assignText(text: String) {
        setProperty(BottleConstants.TEXT, text)
    }

    /**
     * Convenience method to retrieve the ControllerType of the message source.
     *
     * @return a source name. If there is no identified source, return null.
     */
    fun fetchSource(): String {
        return getProperty(BottleConstants.SOURCE, BottleConstants.NO_SOURCE)
    }

    /**
     * Convenience method to set a string naming the message creator. Use
     * the controller type for this.
     *
     * @param source the name of the message creator.
     */
    fun assignSource(source: String) {
        setProperty(BottleConstants.SOURCE, source)
    }
    // =================================== JSON ======================================
    fun toJSON(): String {
        val mapper = jacksonObjectMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        var json = ""
        try {
            json = mapper.writeValueAsString(this)
            LOGGER.info(String.format("%s.toJSON: [%s]", CLSS, json))
        }
        catch (ex: Exception) {
            LOGGER.severe(String.format("%s.toJSON: Exception (%s)", CLSS, ex.localizedMessage))
        }
        return json
    }




    companion object {
        private const val serialVersionUID = 4356286171135500644L
        private const val CLSS = "MessageBottle"
        protected val LOGGER = Logger.getLogger(CLSS)

        // =================================== JSON ======================================
        fun fromJSON(json: String): MessageBottle? {
            var bottle: MessageBottle? = null
            val mapper = jacksonObjectMapper()
            try {
                bottle = mapper.readValue(json)
            }
            catch (jpe: JsonParseException) {
                LOGGER.severe(
                    String.format(
                        "%s.fromJSON: Parse exception (%s) from %s",
                        CLSS, jpe.getLocalizedMessage(), json
                    )
                )
            }
            catch (jme: JsonMappingException) {
                LOGGER.severe(
                    String.format(
                        "%s.fromJSON: Mapping exception (%s) from %s",
                        CLSS, jme.getLocalizedMessage(), json
                    )
                )
            } catch (ioe: IOException) {
                LOGGER.severe(String.format("%s.fromJSON: IO exception (%s)", CLSS, ioe.getLocalizedMessage()))
            }
            return bottle
        }
    }

    init {
        properties = HashMap()
        jointValues = HashMap()
    }
}