/**
 * Copyright 2019 Charles Coughlin. All rights reserved.
 *  (MIT License)
 */

package chuckcoughlin.bert.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;

import chuckcoughlin.bert.service.BluetoothHandler;
import chuckcoughlin.bert.service.FacilityState;
import chuckcoughlin.bert.service.TieredFacility;

/**
 * This class analyzes speech, converting it into text (lists of words).
 * These methods must be executed on the main application thread (UI thread).
 */

public class SpeechAnalyzer implements  RecognitionListener  {
    private static final String CLSS = "SpeechAnalyzer";
    private static final int END_OF_PHRASE_TIME = 2000; // Silence to indicate end-of-input
    private static final long INTER_PHRASE_TIME = 2000;  // Time to wait before considering next input
    private final Context context;
    private SpeechRecognizer sr = null;
    private final BluetoothHandler handler;
    private Intent recognizerIntent = null;

    public SpeechAnalyzer(BluetoothHandler h, Context c ) {
        this.context = c;
        this.handler  = h;
        if( handler==null ) Log.e(CLSS,"SpeechAnalyzer: ERROR: BluetoothHandler is null.");
    }

    public void start() {
        recognizerIntent = createRecognizerIntent();
        resetSpeechRecognizer();
        startListening();
    }

    public void shutdown() {
        if (sr != null) {
            sr.stopListening();
            sr.destroy();
        }
        sr = null;
    }

    // Delay before we start listening to avoid feedback loop
    // with spoken response.
    private void startListening() {
        if(sr!=null) {
            try {
                Thread.sleep(INTER_PHRASE_TIME);
            }
            catch(InterruptedException ignore) {}
            sr.startListening(recognizerIntent);
        }
    }
    private void resetSpeechRecognizer() {

        if(sr != null) {
            sr.destroy();
        }
        sr = SpeechRecognizer.createSpeechRecognizer(context);
        sr.setRecognitionListener(this);
    }
    // ========================================= RecognitionListener ============================
    public void onReadyForSpeech(Bundle params)  {
        Log.i(CLSS, "onReadyForSpeech");
    }
    public void onBeginningOfSpeech(){
        Log.i(CLSS, "onBeginningOfSpeech");
        handler.reportConnectionState(TieredFacility.VOICE, FacilityState.ACTIVE);
    }
    // Background level changed ...
    public void onRmsChanged(float rmsdB){
    }
    public void onBufferReceived(byte[] buffer)  {
        Log.i(CLSS, "onBufferReceived");
    }
    public void onEndOfSpeech()  {
        Log.i(CLSS, "onEndofSpeech");
    }
    public void onError(int error)  {
        String reason = null;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                reason = String.format("Audio recording error");
                break;
                // On the Android device, settings, go to BertSpeak and enable the microphone
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                reason = String.format("INSUFFICIENT PERMISSION - Enable microphone in application");
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                Log.i(CLSS,  String.format("SpeechRecognition: Error - no word match. Enunciate!"));
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                Log.d(CLSS,  String.format("SpeechRecognition: Error - speech timeout"));
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                reason =  String.format("Network error");
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                reason = String.format("Error - in client");
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                Log.i(CLSS, String.format("Error - recognition service is busy (started twice?)"));
                break;
            case SpeechRecognizer.ERROR_SERVER:
                reason = String.format("Error - in server");
                break;
            default:
                reason = String.format("ERROR (%d) ",error);
        }
        if( reason!=null) {
            Log.e(CLSS,  String.format("SpeechRecognizer: Error - %s",reason));
            handler.handleVoiceError(reason);
        }
        // Try again
        resetSpeechRecognizer();
        startListening();

    }
    public void onResults(Bundle results) {
        Log.i(CLSS, "onResults \n" + results);
        // Fill the list view with the strings the recognizer thought it could have heard, there should be 5, based on the call
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        //display results. The zeroth result is usually the space-separated one.
        assert matches != null;
        for (int i = 0; i < matches.size(); i++) {
            Log.i(CLSS, "result " + matches.get(i));
        }
        String text = matches.get(0);
        text = scrubText(text);
        handler.receiveSpokenText(text);
        handler.reportConnectionState(TieredFacility.VOICE, FacilityState.WAITING);
        startListening();   // Repeat forever
    }
    public void onPartialResults(Bundle partialResults) {
        Log.i(CLSS, "onPartialResults");
    }
    public void onEvent(int eventType, Bundle params) {
        Log.i(CLSS, "onEvent " + eventType);
    }

    private Intent createRecognizerIntent() {
        String locale = "us-UK";
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);  // Partials are always empty
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,END_OF_PHRASE_TIME);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        //Give a hint to the recognizer about what the user is going to say
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Max number of results. This is three attempts at deciphering, not a 3-word limit.
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 2);
        return intent;
    }

    /**
     * Perform any cleanup.
     * For now, replace ° with " degrees"
     * @param text
     * @return spiffy-clean text
     */
    private String scrubText(String text) {
        text = text.replace("°"," degrees");
        return text;
    }
}