/**
 * Copyright 2024 Charles Coughlin. All rights reserved.
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
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.camera.view.PreviewView
import chuckcoughlin.bertspeak.common.MessageType
import chuckcoughlin.bertspeak.databinding.FragmentFacesBinding
import chuckcoughlin.bertspeak.service.DispatchService
import chuckcoughlin.bertspeak.ui.facerec.FacialRecognitionView


/**
 * This fragment presents the front camera output and attempts to identify the primary face
 * association it with "the operator".
 */
class FacesFragment (pos:Int): BasicAssistantFragment(pos) {
    private val name: String
    private lateinit var cameraPreview: PreviewView
    private lateinit var analyzeButton: Button
    private lateinit var deleteButton: Button
    private lateinit var saveButton: Button

    // Inflate the view. It holds a fixed image of the robot
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.i(name, "onCreateView: ....")
        val binding = FragmentFacesBinding.inflate(inflater, container, false)
        cameraPreview = binding.cameraPreview
        analyzeButton = binding.facesAnalyzeButton
        analyzeButton.setOnClickListener { analyzeButtonClicked() }
        deleteButton = binding.facesDeleteButton
        deleteButton.setOnClickListener { deleteButtonClicked() }
        saveButton = binding.facesSaveButton
        saveButton.setOnClickListener { saveButtonClicked() }
        return binding.root
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

    //============================= Button Callbacks ================================
    //
    fun analyzeButtonClicked() {
        Log.i(name, "Analyze button clicked")

    }
    fun deleteButtonClicked() {
        Log.i(name, "Delete button clicked")

    }
    fun saveButtonClicked() {
        Log.i(name, "Save button clicked")

    }

    val CLSS = "FacesFragment"
    val CAPTURE_SIZE = 256

    init {
        name = CLSS
    }
}
