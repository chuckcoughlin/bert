/**
 * Copyright 2024-2025 Charles Coughlin. All rights reserved.
 * (MIT License)
 * @see https://medium.com/swlh/how-to-create-a-simple-camera-app-using-android-camerax-library-7367778498e0
 */
package chuckcoughlin.bertspeak.tab

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import chuckcoughlin.bertspeak.common.DispatchConstants
import chuckcoughlin.bertspeak.data.JsonObserver
import chuckcoughlin.bertspeak.data.JsonType
import chuckcoughlin.bertspeak.data.JsonType.FACE_NAMES
import chuckcoughlin.bertspeak.data.JsonType.LINK_LOCATIONS
import chuckcoughlin.bertspeak.data.StatusData
import chuckcoughlin.bertspeak.data.StatusObserver
import chuckcoughlin.bertspeak.databinding.FragmentFacesBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.service.DispatchService.Companion.CLSS
import chuckcoughlin.bertspeak.service.ManagerState
import chuckcoughlin.bertspeak.service.ManagerType
import chuckcoughlin.bertspeak.ui.adapter.TextListAdapter
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * This fragment presents the front camera output and attempts to identify the primary face
 * association it with "the operator".
 */
class FacesFragment (pos:Int): BasicAssistantFragment(pos), JsonObserver, StatusObserver {
    override val name: String

    private val detector: FaceDetector
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraPreview: PreviewView
    private lateinit var adapter: TextListAdapter
    private val imageCapture: ImageCapture
    private val callback: ImageCaptureCallback
    private val options:FaceDetectorOptions
    private val faceNames: MutableList<String>
    private lateinit var analyzeButton: Button
    private lateinit var deleteButton: Button
    private val gson: Gson

    // Inflate the view. It holds a fixed image of the robot
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.i(name, "onCreateView: ....")
        cameraExecutor = Executors.newSingleThreadExecutor()
        val binding = FragmentFacesBinding.inflate(inflater, container, false)
        cameraPreview = binding.cameraPreview
        analyzeButton = binding.facesAnalyzeButton
        analyzeButton.setOnClickListener { analyzeButtonClicked() }
        deleteButton = binding.facesDeleteButton
        deleteButton.setOnClickListener { deleteButtonClicked() }
        val txtarray = faceNames.toTypedArray()
        adapter = TextListAdapter(requireContext(),txtarray)
        var namesListView = binding.facesRecyclerView   // RecyclerView
        namesListView.setAdapter(adapter)
        return binding.root
    }


    override fun onDestroy() {
        super.onDestroy()
    }
    /**
     *
     */
    override fun onStart() {
        super.onStart()
        startCamera()
        DispatchService.registerForStatus(this)
        DispatchService.registerForJson(this)
    }

    override fun onStop() {
        super.onStop()
        DispatchService.unregisterForStatus(this)
        DispatchService.unregisterForJson(this)
        cameraExecutor.shutdown()
    }

    private fun startCamera()  {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Preview this, cameraSelector, preview, imageCapture)
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }

            // Use front camera by default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    requireActivity(), cameraSelector, preview, imageCapture)
            }
            catch (iae: IllegalArgumentException) {
                Log.e(CLSS, String.format("Camera not available (%s)",iae.localizedMessage))
            }
            catch (ise: IllegalStateException) {
                Log.e(CLSS, String.format("Lens validation failed (%s)",ise.localizedMessage))
            }
            catch (exc: Exception) {
                Log.e(CLSS, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Take a photo keeping the image in memory for analysis
    private fun takePicture() {
        // Set up image capture listener, which is triggered once photo has
        // been taken
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),callback);
    }

    //============================= Button Callbacks ================================
    //
    fun analyzeButtonClicked() {
        Log.i(name, "Analyze button clicked")
        takePicture()

    }
    fun deleteButtonClicked() {
        Log.i(name, "Delete button clicked")

    }
    //========================= JsonObserver =====================================
    override fun resetItem(map:Map<JsonType,String>) {
        var json = map.get(FACE_NAMES)
        adapter.clear()
        if( json!=null && !json.isEmpty() ) {
            val list = gson.fromJson(json, MutableList::class.java) as MutableList<*>
            val stringList = mutableListOf<String>()
            for(data in list) {
                stringList.add(data.toString())
            }
            adapter.addAll(stringList)
        }
    }

    override fun updateItem(type:JsonType,json:String) {
        adapter.clear()
        if( type==JsonType.FACE_NAMES ) {
            if( !json.isEmpty()) {
                val list = gson.fromJson(json, MutableList::class.java) as MutableList<*>
                val stringList = mutableListOf<String>()
                for(data in list) {
                    stringList.add(data.toString())
                }
                adapter.addAll(stringList)
            }
        }
    }
    // ===================== StatusDataObserver =====================
    override fun resetStatus(list: List<StatusData>) {
        for (ddata in list) {
            updateStatus(ddata)
        }
    }

    /**
     * When the SocketManager comes online, request a list of known faces.
     */
    override fun updateStatus(data: StatusData) {

        if (data.action.equals(DispatchConstants.ACTION_MANAGER_STATE)) {
            if( data.type== ManagerType.SOCKET && data.state== ManagerState.ACTIVE ) {
                Log.i(name, String.format("updateStatus (%s):%s = %s",data.action,data.type,data.state))
                DispatchService.sendJsonRequest(FACE_NAMES)
            }
        }
    }
    class ImageCaptureCallback(detect: FaceDetector): ImageCapture.OnImageCapturedCallback() {
        val detector = detect

        //============================= Image Captured Callbacks ============================
        override fun onError(exc: ImageCaptureException) {
            Log.e(CLSS, "Photo capture failed: ${exc.message}", exc)
        }

        @androidx.camera.core.ExperimentalGetImage
        override fun onCaptureSuccess(imageProxy: ImageProxy) {
            Log.i(CLSS, "Image captured: ...")
            val mediaImage = imageProxy.image
            if(mediaImage!=null) {
                val image = InputImage.fromMediaImage(mediaImage,imageProxy.imageInfo.rotationDegrees)
                val result = detector.process(image)
                    .addOnSuccessListener { faces ->
                        // Task completed successfully
                        Log.i(CLSS, String.format("%d faces detected",faces.size))
                        if(faces.size>0 ) {
                            DispatchService.reportFaceDetected(faces[0])
                        }
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        Log.i(CLSS, String.format("Faces detection failed (%s)",e.localizedMessage))
                    }
                if( result.isSuccessful ) {
                    Log.i(CLSS, "Image captured: ... SUCCESS")
                }
            }
        }
        val CLSS = "ImageCaptureCallback"
    }

    val CLSS = "FacesFragment"
    val CAPTURE_SIZE = 256

    init {
        name = CLSS
        imageCapture = ImageCapture.Builder().build()
        options = FaceDetectorOptions.Builder()
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
        detector = FaceDetection.getClient(options)
        callback = ImageCaptureCallback(detector)
        faceNames = ArrayList<String>()
        gson = Gson()
    }
}
