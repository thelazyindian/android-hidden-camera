package com.androidhiddencamera;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

/**
 * Created by Keval on 10-Nov-16.
 * This surface view works as the fake preview for the camera.
 *
 * @author {@link 'https://github.com/kevalpatel2106'}
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private HiddenCameraActivity mHiddenCameraActivity;

    private SurfaceHolder mHolder;
    private Camera mCamera;

    private boolean safeToTakePicture = false;

    CameraPreview(@NonNull Context context) {
        super(context);

        //validate the context
        if (context instanceof HiddenCameraActivity) {
            mHiddenCameraActivity = (HiddenCameraActivity) context;
        } else {
            throw new IllegalArgumentException("You must inherit HiddenCameraActivity in your parent class.");
        }

        //Set surface holder
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            // left blank for now
            mHiddenCameraActivity.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        safeToTakePicture = false;

        // Now that the size is known, set up the camera parameters and begin the preview.
        Camera.Parameters parameters = mCamera.getParameters();

        List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPictureSizes();
        parameters.setPreviewSize(previewSizes.get(previewSizes.size() - 1).width,
                previewSizes.get(previewSizes.size() - 1).height);
        requestLayout();
        mCamera.setParameters(parameters);

        mCamera.startPreview();
        safeToTakePicture = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Call stopPreview() to stop updating the preview surface.
        if (mCamera != null) {
            safeToTakePicture = false;
            mCamera.stopPreview();
        }
    }

    /**
     * Initialize the camera and start the preview of the camera.
     *
     * @param id camera id.
     */
    void startPreview(int id) {
        if (safeCameraOpen(id)) {
            if (mCamera != null) {
                requestLayout();

                try {
                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.startPreview();

                    safeToTakePicture = true;
                } catch (IOException e) {
                    e.printStackTrace();

                    mHiddenCameraActivity.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
                }
            }
        } else {
            mHiddenCameraActivity.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            stopPreviewAndFreeCamera();

            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e("CameraPreview", "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    boolean isSafeToTakePictureInternal() {
        return safeToTakePicture;
    }

    void takePictureInternal() {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                mHiddenCameraActivity.onImageCapture(BitmapFactory
                        .decodeByteArray(bytes, 0, bytes.length));
            }
        });
    }

    /**
     * When this function returns, mCamera will be null.
     */
    void stopPreviewAndFreeCamera() {
        safeToTakePicture = false;

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}