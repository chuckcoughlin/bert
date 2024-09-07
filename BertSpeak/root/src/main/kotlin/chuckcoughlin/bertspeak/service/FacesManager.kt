/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 */
package chuckcoughlin.bertspeak.service

import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import chuckcoughlin.bert.common.util.FaceDataHolder
import chuckcoughlin.bert.common.util.NamedPoint
import chuckcoughlin.bertspeak.data.TextObserver
import chuckcoughlin.bertspeak.common.LandmarkTag
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
        val faceContour = face.getContour(FaceContour.FACE)
        if( faceContour!=null ) {
            for (point: PointF in faceContour.points) {
                Log.i(CLSS, String.format("    %.2f %.2f", point.x, point.y))
            }
        }
        else {
            Log.i(CLSS, "ERROR: No overall face countour")
        }
        val landmarks = face.allLandmarks
        for(landmark: FaceLandmark in landmarks) {
            Log.i(CLSS, String.format("Landmark type is %d at %2.2f,%2.2f",landmark.landmarkType,landmark.position.x,landmark.position.y))
        }

        // ------------------ Send Face to the Robot -----------------------
        val holder = FaceDataHolder()
        for(landmark in landmarks) {
            val norm = normalizePoint(bb,landmark.position)
            val landmarkTag = LandmarkTag.tagForCode(landmark.landmarkType)
            val np = NamedPoint(landmarkTag.name,norm.x,norm.y)
            holder.addLandmark(np)
        }
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
    private fun normalizePoint( bb: Rect, p: PointF) : PointF {
        var x =  p.x - bb.right
        var y = p.y-bb.top
        x = x/bb.width()
        y = y/bb.height()
        return PointF(x,y)
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
