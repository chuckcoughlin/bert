/**
 * Copyright 2024-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.graphics.Rect
import android.util.Log
import chuckcoughlin.bertspeak.common.ContourTag
import chuckcoughlin.bertspeak.common.LandmarkTag
import chuckcoughlin.bertspeak.data.FaceDirection
import chuckcoughlin.bertspeak.data.FacialDetails
import chuckcoughlin.bertspeak.data.JsonType
import chuckcoughlin.bertspeak.data.Point2D
import kotlin.math.PI
import com.google.gson.Gson
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.atan2

/**
 * Accept a new face structure from the FacesFragment, then handle all
 * communication from the robot concerning faaes.
 */
class FacesManager (service:DispatchService): CommunicationManager {
    private val dispatcher = service
    override val managerType = ManagerType.FACES
    override var managerState = ManagerState.OFF
    private var faceList: MutableList<String>    // Names of face owners

    override fun start() {
    }
    /**
     * Called when main activity is stopped. Clean up any resources.
     * To use again requires re-initialization.
     */
    override fun stop() {
    }

    /**
     * Inform the robot of the newly analyzed face.
     * We have found all landmark points and contours to be available.
     */
    fun reportFaceDetected(face:Face) {
        Log.i(CLSS, "======================= Got a FACE =======================")
        val contours = face.allContours
        Log.i(CLSS, String.format("Face has %d contours",contours.size))
        val landmarks = face.allLandmarks
        for(landmark: FaceLandmark in landmarks) {
            Log.i(CLSS, String.format("reportFaceDetected: landmark type is %d at %2.2f,%2.2f",landmark.landmarkType,landmark.position.x,landmark.position.y))
        }
        val facebox = face.boundingBox
        Log.i(CLSS, String.format("reportFaceDetected: boundingBox is %d x %d at %d,%d",facebox.right-facebox.left,facebox.top-facebox.bottom, facebox.top,facebox.left))
        // ------------------ Prepare Face for the Robot -----------------------
        val details = FacialDetails()
        for(landmark in landmarks) {
            val norm = normalizePoint(facebox, Point2D(landmark.position.x.toDouble(),
                                                       landmark.position.y.toDouble()))
            val landmarkTag = LandmarkTag.tagForCode(landmark.landmarkType)
            val p = Point2D(norm.x,norm.y)
            details.addLandmark(landmarkTag.name,p)
        }
        for(contour in contours) {
            val contourTag = ContourTag.tagForCode(contour.faceContourType)
            var index = 0
            for(point in contour.points) {
                val norm = normalizePoint(facebox, Point2D(point.x.toDouble(),point.y.toDouble()))
                details.addContourPoint(contourTag.name, index, norm)
                index = index+1
            }
        }


        var json = Gson().toJson(details)
        dispatcher.reportJsonData(JsonType.FACIAL_DETAILS,json)

        val direction = computeFaceDirection(facebox)
        json = Gson().toJson(direction)
        dispatcher.reportJsonData(JsonType.FACE_DIRECTION,json)
    }

    /**
     * Use the difference in size between screen and bounding box to
     * infer a distance from screen. Then use difference in centers
     * of screen and bounding box to infer a direction. Compare widths.
     */
    private fun computeFaceDirection(faceBox:Rect):FaceDirection {
        val faceDirection = FaceDirection()
        /* Compute purported distance to face */
        val screenWidth = dispatcher.screenWidth.toFloat()
        val boxWidth = faceBox.width().toFloat()
        val distToFace = (FOCAL_LENGTH * boxWidth)/screenWidth
        val screenCenterX = screenWidth/2f
        val boxCenterX = faceBox.centerX().toFloat()
        faceDirection.azimuth = atan2(screenCenterX-boxCenterX,distToFace)*PI/180

        val screenHeight = dispatcher.screenHeight.toFloat()
        val screenCenterY = -screenHeight/2f
        val boxCenterY = faceBox.centerY().toFloat()
        faceDirection.elevation = atan2(screenCenterY-boxCenterY,distToFace)*PI/180
        Log.i(CLSS, String.format("computeFaceDirection: direction is %2.0f x %2.0f",faceDirection.azimuth,faceDirection.elevation))

        return faceDirection
    }

    /** convert the coordinates of a point to reference the
     * bounding box instead of the entire image.
     */
    private fun normalizePoint( bb: Rect, p: Point2D) : Point2D {
        var x:Double =  p.x - bb.right
        var y:Double = p.y-bb.top
        x = x/bb.width()
        y = y/bb.height()
        return Point2D(x,y)
    }

    private val CLSS = "FacesManager"
    /* Approximate distance from the screen to the vanishing
     * point of the face. This is used in our calculation of
     * direction to the center of the face
     */
    private val FOCAL_LENGTH = 10000f  // Vanishing distance ~ mm

    /**
     * There should only be one text manager. owned by the dispatch service.
     * There are three queues:
     * 1) Spoken text, both requests and responses
     * 2) Logs
     * 3) Table (only the most recent)
     * When a subscriber first registers, the current queue to-date
     * is sent.
     */
    init {
        faceList     = mutableListOf<String>()
    }
}
