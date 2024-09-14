/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.graphics.Rect
import android.util.Log
import chuckcoughlin.bert.common.message.JsonType
import chuckcoughlin.bertspeak.common.ContourTag
import chuckcoughlin.bertspeak.data.TextObserver
import chuckcoughlin.bertspeak.common.LandmarkTag
import chuckcoughlin.bertspeak.data.FacialDetectionDetails
import chuckcoughlin.bertspeak.data.NamedPoint
import chuckcoughlin.bertspeak.data.Point2D
import com.google.gson.Gson
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark

/**
 * Accept a new face structure from the FacesFragment, then handle all
 * communication from the robot concerning faaes.
 */
class FacesManager (service:DispatchService): CommunicationManager {
    private val dispatcher = service
    override val managerType = ManagerType.FACES
    override var managerState = ManagerState.OFF
    private var faceList: MutableList<String>    // Names of face owners
    private val faceObservers: MutableMap<String, TextObserver>

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
        val bb = face.boundingBox
        Log.i(CLSS, String.format("BoundingBox is %d x %d at %d,%d",bb.right-bb.left,bb.top-bb.bottom, bb.top,bb.left))
        val landmarks = face.allLandmarks
        for(landmark: FaceLandmark in landmarks) {
            Log.i(CLSS, String.format("Landmark type is %d at %2.2f,%2.2f",landmark.landmarkType,landmark.position.x,landmark.position.y))
        }

        // ------------------ Prepare Face to the Robot -----------------------
        val holder = FacialDetectionDetails()
        for(landmark in landmarks) {
            val norm = normalizePoint(bb, Point2D(landmark.position.x,landmark.position.y))
            val landmarkTag = LandmarkTag.tagForCode(landmark.landmarkType)
            val np = NamedPoint(landmarkTag.name,norm.x,norm.y)
            holder.addLandmark(np)
        }
        for(contour in contours) {
            val contourTag = ContourTag.tagForCode(contour.faceContourType)
            for(point in contour.points) {
                val norm = normalizePoint(bb, Point2D(point.x,point.y))
                holder.addContourPoint(contourTag.name, norm)
            }
        }
        val json = Gson().toJson(holder)
        dispatcher.reportJsonData(JsonType.FACE,json)
    }
    /**
     * When a new log observer is registered, send a link to this manager.
     * The observer can then initialize its list, if desired. The manager
     * reference should be cleared on "unregisterSettingsObserver".
     * @param observer
     */
    fun register(observer: TextObserver) {
        faceObservers[observer.name] = observer
        observer.resetList(faceList)
    }

    fun unregister(observer: TextObserver) {
        for( key in faceObservers.keys ) {
            if( !observer.equals(faceObservers.get(key)) ) {
                faceObservers.remove(key,observer)
                break
            }
        }
    }

    private fun initializeObservers() {
        for (observer in faceObservers.values) {
            observer.resetList(faceList)
        }
    }

    /** convert the coordinates of a point to reference the
     * bounding box instead of the entire image.
     */
    private fun normalizePoint( bb: Rect, p: Point2D) : Point2D {
        var x =  p.x - bb.right
        var y = p.y-bb.top
        x = x/bb.width()
        y = y/bb.height()
        return Point2D(x,y)
    }
    /**
     * Notify observers regarding receipt of a new current face
     */
    private fun notifyObservers(index:Int) {
        Log.i(CLSS, String.format("notifyObservers: %d", index))
        for (observer in faceObservers.values) {
            observer.selectItem(index)
        }
    }

    private val CLSS = "FacesManager"

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
        faceObservers = mutableMapOf<String, TextObserver>()
    }
}
