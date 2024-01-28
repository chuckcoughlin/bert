/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
 * (MIT License)
 * @see https://medium.com/swlh/how-to-create-a-simple-camera-app-using-android-camerax-library-7367778498e0
 */
package chuckcoughlin.bertspeak.tab

import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.databinding.FragmentFacesBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.ui.facerec.FacialRecognitionView
import java.io.File
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


/**
 * This fragment presents the front camera output and attempts to identify the primary face
 * association it with "the operator".
 */
class FacesFragment (pos:Int): BasicAssistantFragment(pos) {
    private val name: String
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraPreview: PreviewView
    private val imageCapture: ImageCapture
    private val callback: ImageCaptureCallback
    private lateinit var analyzeButton: Button
    private lateinit var deleteButton: Button
    private lateinit var saveButton: Button

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
        saveButton = binding.facesSaveButton
        saveButton.setOnClickListener { saveButtonClicked() }
        startCamera()
        return binding.root
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    /**
     *
     */
    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    private fun startCamera() {
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
                    requireActivity(), cameraSelector, preview)
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
    fun saveButtonClicked() {
        Log.i(name, "Save button clicked")

    }
    class ImageCaptureCallback(): ImageCapture.OnImageCapturedCallback() {

        //============================= Image Captured Callbacks ================================
        override fun onError(exc: ImageCaptureException) {
            Log.e(CLSS, "Photo capture failed: ${exc.message}", exc)
        }

        override fun onCaptureSuccess(image: ImageProxy) {
            Log.i(CLSS, "Image captured: ...")
        }
        val CLSS = "ImageCptureCallback"
    }

    val CLSS = "FacesFragment"
    val CAPTURE_SIZE = 256

    init {
        name = CLSS
        imageCapture = ImageCapture.Builder().build()
        callback = ImageCaptureCallback()
    }
}
