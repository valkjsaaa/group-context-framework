/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.face.facetracker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.widget.Toast;

import com.adefreitas.gcf.android.AndroidGroupContextManager;
import com.adefreitas.gcf.android.bluewave.BluewaveManager;
import com.adefreitas.gcf.android.bluewave.JSONContextParser;
import com.adefreitas.gcf.CommManager;
import com.adefreitas.gcf.Settings;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.CameraSourcePreview;
import com.google.android.gms.samples.vision.face.facetracker.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Activity for the face tracker app.  This app detects faces with the rear facing camera, and draws
 * overlay graphics to indicate the position, size, and ID of each face.
 */
public final class FaceTrackerActivity extends Activity {
    private static final String TAG = "FaceTracker";
    private static final String API_KEY = "3c37e71dc616a68515c0cead13531fa4";
    private static final String API_SECRET = "FinOk9wJ6u6lQ1lBQVOytSQFTsTOgVlQ";
    private static final String DEV_NAME = "Smart Sign - Hello";
    private static final String GROUP_ID = "5188a985698b5e68c7fc07593a02ad68";
    private static final CommManager.CommMode COMM_MODE  	 = CommManager.CommMode.MQTT;
    private static final String   IP_ADDRESS 	 = Settings.DEV_MQTT_IP;
    private static final int      PORT 	    	 = Settings.DEV_MQTT_PORT;
    public static Context context;
    private static HttpRequests sFacePPAPI = null;
    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private AndroidGroupContextManager mGCFManager;
    private BroadcastReceiver mGCFReceiver;
    private IntentFilter mGCFIntentFilter;
    private Set<String> mDeviceAround;
    private Map<String, String> mPersonAround;

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        context = this;

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        createFaceRecognizer();
        createContextManager();
    }

    /**
     * create Face Recognizer and link it to Surface View
     */
    public void createFaceRecognizer(){
        FaceDetector.Builder detectorBuilder = new FaceDetector.Builder(context);
        detectorBuilder.setClassificationType(FaceDetector.ALL_CLASSIFICATIONS);
        Detector<Pair<Face, Frame>> faceImageDetector = new Detector<Pair<Face, Frame>>() {
            FaceDetector faceDetector = new FaceDetector.Builder(context).build();

            @Override
            public SparseArray<Pair<Face, Frame>> detect(Frame frame) {
                SparseArray<Face> faceArray = faceDetector.detect(frame);
                SparseArray<Pair<Face, Frame>> faceImageArray = new SparseArray<>();
                for (int i = 0; i < faceArray.size(); i++) {
                    int key;
                    if ((key = faceArray.keyAt(i)) != -1) {
                        Face face = faceArray.get(key);
                        faceImageArray.append(key, new Pair<>(face, frame));
                    }
                }
                return faceImageArray;
            }
        };
        faceImageDetector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());

        mCameraSource = new CameraSource.Builder(context, faceImageDetector)
                .setRequestedPreviewSize(1280, 720)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * create Context Manager and Intent listener
     */
    public void createContextManager(){
        mGCFManager = new AndroidGroupContextManager(new ContextWrapper(context), DEV_NAME, false);

        mGCFManager.getBluewaveManager().setCredentials("IMPROMPTU", new String[]{"face", "device", "identity", "location", "time", "activity", "preferences", "snap-to-it"});

        mGCFManager.getBluewaveManager().setDiscoverable(true);
        mGCFManager.getBluewaveManager().startScan(30 * 1000);

        mDeviceAround = new HashSet<>();
        mPersonAround = new HashMap<>();

        if (sFacePPAPI == null) {
            sFacePPAPI = new HttpRequests(API_KEY, API_SECRET, false, false);
        }

        mGCFReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Set<String> newDeviceAround = new HashSet<>(Arrays.asList(mGCFManager.getBluewaveManager().getNearbyDevices(30)));
                Set<String> oldDeviceAround = new HashSet<>(mDeviceAround);
                Set<String> updatedDeviceAround = new HashSet<>(mDeviceAround);
                oldDeviceAround.removeAll(newDeviceAround);
                newDeviceAround.removeAll(mDeviceAround);
                for (final String deviceName : oldDeviceAround) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                removePerson(deviceName);
                            } catch (FaceppParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    updatedDeviceAround.remove(deviceName);
                }
                for (final String deviceName : newDeviceAround) {
                    JSONContextParser json = mGCFManager.getBluewaveManager().getContext(deviceName);
                    if (json != null) {
                        Log.i(TAG, "Device added: name: " + deviceName + "json: " + json.toString());
                        try {
                            if (json.getJSONObject("face") != null) {
                                final String name = json.getJSONObject("face").getString("name");
                                JSONArray urlJsonArray = json.getJSONObject("face").getJSONArray("pictures");
                                final String[] urlArray = new String[urlJsonArray.length()];
                                for (int i = 0; i < urlJsonArray.length(); i++) {
                                    String url = urlJsonArray.getString(i);
                                    urlArray[i] = url;
                                }
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            addPerson(deviceName, name, urlArray);
                                        } catch (FaceppParseException | JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                                updatedDeviceAround.add(deviceName);
                            }
                        } catch (JSONException | NullPointerException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.i(TAG, "Device added: name: " + deviceName + "json: null");
                    }
                }
                mDeviceAround = updatedDeviceAround;
            }
        };
        mGCFIntentFilter = new IntentFilter();
        mGCFIntentFilter.addAction(BluewaveManager.ACTION_OTHER_USER_CONTEXT_RECEIVED);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sFacePPAPI.groupRemovePerson(new PostParameters().setPersonId("all").setGroupId(GROUP_ID));
                } catch (FaceppParseException e) {
                    e.printStackTrace();
                }
                context.registerReceiver(mGCFReceiver, mGCFIntentFilter);
            }
        }).start();
    }

    public void addPerson(String deviceID, String personName, String[] personFaceUrl) throws FaceppParseException, JSONException {
        Log.i(TAG, "addPerson: deviceID: " + deviceID + " name: " + personName + " url: [" + personFaceUrl.toString() + "]");
        HttpRequests facepp = FaceTrackerActivity.sFacePPAPI;
        try {
            facepp.personDelete(new PostParameters().setPersonName(personName));
        } catch (FaceppParseException e) {
            e.printStackTrace();
        }
        ArrayList<String> faceIdList = new ArrayList<>();
        for (String url : personFaceUrl) {
            try {
                JSONObject result = facepp.detectionDetect(new PostParameters().setUrl(url));
                String faceId = result.getJSONArray("face").getJSONObject(0).getString("face_id");
                faceIdList.add(faceId);
                Log.i(TAG, "addPerson: name: " + personName + " faceID: " + faceId);
            } catch (FaceppParseException | JSONException e) {
                e.printStackTrace();
            }
        }
        String personId = facepp.personCreate(new PostParameters().setPersonName(personName).setFaceId(faceIdList).setGroupId(GROUP_ID)).getString("person_id");
        facepp.trainIdentify(new PostParameters().setGroupId(GROUP_ID));
        Log.i(TAG, "addPerson: personCreated! name: " + personName);
        mPersonAround.put(deviceID, personId);
        showToast("Person added: " + personName);
    }

    public void removePerson(String deviceID) throws FaceppParseException {
        HttpRequests facepp = FaceTrackerActivity.sFacePPAPI;
        facepp.personDelete(new PostParameters().setPersonId(mPersonAround.get(deviceID)).setGroupId(GROUP_ID));
        facepp.trainIdentify(new PostParameters().setGroupId(GROUP_ID));
    }

    /**
     * A {@link Handler} for showing {@link Toast}s.
     */
    private Handler mMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(context, (String) msg.obj, Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(String text) {
        // We show a Toast by sending request message to mMessageHandler. This makes sure that the
        // Toast is shown on the UI thread.
        Message message = Message.obtain();
        message.obj = text;
        mMessageHandler.sendMessage(message);
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraSource.release();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        try {
            mPreview.start(mCameraSource, mGraphicOverlay);
        } catch (IOException e) {
            Log.e(TAG, "Unable to start camera source.", e);
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Pair<Face, Frame>> {
        @Override
        public Tracker<Pair<Face, Frame>> create(Pair<Face, Frame> face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Pair<Face, Frame>> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;
        private int counter = 0;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        private Bitmap rotateBitmap(Bitmap source, float angle) {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        }

        private void recognize(Pair<Face, Frame> item) {
            Face face = item.first;
            Frame frame = item.second;
            int frameWidth = frame.getMetadata().getWidth();
            int width = (int) face.getHeight();
            int height = (int) face.getWidth();
            int left = (int) face.getPosition().y;
            int top = (int) face.getPosition().x;
            int right = left + width;
            int bottom = top + height;
            left = left > 0 ? left : 0;
            top = top > 0 ? top : 0;

            int[] facePixels = new int[width * height];
            byte[] framePixels = frame.getGrayscaleImageData().array();
            for (int i = left; i < right; i++) {
                for (int j = top; j < bottom; j++) {
                    int framePosition = i + j * frameWidth;
                    int facePosition = (i - left) + (j - top) * width;
                    int framePixel = (framePixels[framePosition]) & 0xFF;
                    facePixels[facePosition] = (0xFF000000) + (framePixel << 16) + (framePixel << 8) + (framePixel);
                }
            }
            Log.i(TAG, "createBitmap " + face.getPosition().x + " " + face.getPosition().y + " " + face.getWidth() + " " + face.getHeight());
            Bitmap faceBitmap = Bitmap.createBitmap(facePixels, width, height, Bitmap.Config.ARGB_8888);
            switch (frame.getMetadata().getRotation()) {
                case Frame.ROTATION_0:
                    break;
                case Frame.ROTATION_90:
                    faceBitmap = rotateBitmap(faceBitmap, 90);
                    break;
                case Frame.ROTATION_180:
                    faceBitmap = rotateBitmap(faceBitmap, 180);
                    break;
                case Frame.ROTATION_270:
                    faceBitmap = rotateBitmap(faceBitmap, 270);
                    break;
            }

            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                faceBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                File cacheDir = FaceTrackerActivity.context.getCacheDir();
                File faceFile = File.createTempFile("face", ".png", cacheDir);
                faceBitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(faceFile));
                try {
                    JSONObject faceDetection = FaceTrackerActivity.sFacePPAPI.detectionDetect(new PostParameters()
                            .setImg(faceFile));
                    Log.i(TAG, "detection: " + faceDetection.toString());
                    JSONObject result = FaceTrackerActivity.sFacePPAPI.recognitionIdentify(new PostParameters()
                            .setGroupId(GROUP_ID)
                            .setImg(faceFile)
                            .setMode("oneface"));
                    Log.i(TAG, "result get: " + result.toString());
                    JSONObject faceObj = result
                            .getJSONArray("face")
                            .getJSONObject(0)
                            .getJSONArray("candidate")
                            .getJSONObject(0);
                    double confidence = faceObj.getDouble("confidence");
                    String name = faceObj.getString("person_name");
                    Log.i(TAG, "face found: { name: " + name + ", confidence: " + confidence + "}");
                    if (confidence > 30.0) {
                        showToast("Hello " + name + "!");
                        mFaceGraphic.setmFaceName(name);
                    } else {
                        showToast("Are you " + name + "?\nI'm " + confidence + "% sure.");
                    }
                } catch (FaceppParseException | JSONException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, final Pair<Face, Frame> item) {
            mFaceGraphic.setId(faceId);
            if (((++counter) & 0xFFF) == 0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        recognize(item);
                    }
                }).start();
            }
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Pair<Face, Frame>> detectionResults, final Pair<Face, Frame> item) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(item.first);
            if ((++counter & 0xF) == 0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        recognize(item);
                    }
                }).start();
            }
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Pair<Face, Frame>> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }
}
