/**
 * Copyright 2022-2025. Charles Coughlin. All Rights Reserved.
 * MIT License.
 */
package chuckcoughlin.bert.speech.process

import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bert.common.message.MessageBottle
import chuckcoughlin.bert.common.message.RequestType
import chuckcoughlin.bert.common.model.ConfigurationConstants
import chuckcoughlin.bert.common.model.FaceDirection
import chuckcoughlin.bert.common.model.FacialDetails
import chuckcoughlin.bert.sql.db.Database
import chuckcoughlin.bert.sql.db.SQLConstants
import java.util.logging.Logger

/**
 * Handle tablet messages dealing with facial recognition.
 */
object FaceMessageHandler  {
    private var currentFace: FacialDetails
    private var currentName: String
    private var idIsPending: Boolean   // When true we have face details, no name

    /**
     * Associate the supplied name with the current facial details
     */
    fun associateNameWithFace(name:String):Boolean {
        var result = true
        if(!currentFace.landmarks.isEmpty()) {
            Database.createFace(name,currentFace)
            idIsPending = false
        }
        else {
            result = false
            LOGGER.warning(String.format("%s.associateNameWithFace: Tried to assign %s a face, but no details pending",CLSS,name))
        }
        return result
    }
    /**
     *  Send a list of known face names
     */
    fun getFaceNames() : MessageBottle {
        val msg = MessageBottle(RequestType.JSON)
        msg.jtype = JsonType.FACE_NAMES
        msg.text = Database.faceNamesToJSON()
        return msg
    }
    /**
     * The tablet has detected a face.
     * 1) If it matches a known face ,then send a greeting
     * 2) Otherwise save the details as pending and request
     *    an identifying name to go with it.
     *
     * @param response
     * @return a notification to the user.
     */
    fun handleDetails(details: FacialDetails) :MessageBottle {
        currentFace = details
        var faceId = Database.matchDetailsToFace(details)
        var msg = MessageBottle(RequestType.NOTIFICATION)
        if( faceId== SQLConstants.NO_FACE ) {
            val text = selectRandomText(whoPhrases)
            msg.text = text
            idIsPending = true
        }
        else {
            val name = Database.getFaceName(faceId)
            currentName = name
            val text = selectRandomText(helloPhrases)
            msg.text = String.format(text,name)
        }
        return msg
    }
    /**
     * The tablet has detected a face.
     * Turn the robot head and eyes in the direction of the face.
     *
     * @param response
     * @return a notification to the user.
     */
    fun handleDirection(direction: FaceDirection) :MessageBottle {
        var msg = MessageBottle(RequestType.NOTIFICATION)
        return msg
    }

    /**
     * Select a random phrase from the list.
     * @return the selected phrase.
     */
    private fun selectRandomText(phrases: Array<String>): String {
        val rand = Math.random()
        val index = (rand * phrases.size).toInt()
        return phrases[index]
    }

    // Phrases to choose from ... (randomly)
    private val helloPhrases = arrayOf(
            "Hi %s",
            "hello %s, I'm glad to see you",
            "%s, glad to see you",
            "%s, I'm listening"
    )
    private val whoPhrases = arrayOf(
            "Who are you",
            "What is your name",
            "Who is controlling me"
    )

    private val CLSS = "FaceMessageHandler"
    private val LOGGER = Logger.getLogger(CLSS)

    init {
        currentName = ConfigurationConstants.NO_NAME
        currentFace = FacialDetails()
        idIsPending = false
    }
}