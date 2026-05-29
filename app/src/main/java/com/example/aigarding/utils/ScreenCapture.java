package com.example.aigarding.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.util.Log;
import android.view.WindowManager;

import java.nio.ByteBuffer;

public class ScreenCapture {
    private static final String TAG = "ScreenCapture";
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private int screenWidth;
    private int screenHeight;
    private Bitmap latestScreenshot;

    public interface ScreenshotCallback {
        void onScreenshot(Bitmap bitmap);
        void onError(String error);
    }

    public void startCapture(Context context, MediaProjection projection) {
        this.mediaProjection = projection;
        Point size = getScreenSize(context);
        this.screenWidth = size.x;
        this.screenHeight = size.y;
        
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
        
        mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            screenWidth,
            screenHeight,
            1,
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(),
            null,
            null
        );
    }

    public void captureCrop(int left, int top, int right, int bottom, ScreenshotCallback callback) {
        Image image = null;
        try {
            image = imageReader.acquireLatestImage();
            if (image != null) {
                Bitmap fullScreen = imageToBitmap(image);
                
                int cropLeft = Math.max(0, Math.min(left, screenWidth));
                int cropTop = Math.max(0, Math.min(top, screenHeight));
                int cropWidth = Math.min(right - cropLeft, screenWidth - cropLeft);
                int cropHeight = Math.min(bottom - cropTop, screenHeight - cropTop);
                
                if (cropWidth > 0 && cropHeight > 0) {
                    Bitmap cropped = Bitmap.createBitmap(fullScreen, cropLeft, cropTop, cropWidth, cropHeight);
                    latestScreenshot = cropped;
                    callback.onScreenshot(cropped);
                } else {
                    callback.onError("Invalid crop area");
                }
            } else {
                callback.onError("No image available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Capture error", e);
            callback.onError(e.getMessage());
        } finally {
            if (image != null) {
                image.close();
            }
        }
    }

    public Bitmap getLatestScreenshot() {
        return latestScreenshot;
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * screenWidth;

        Bitmap bitmap = Bitmap.createBitmap(
            screenWidth + rowPadding / pixelStride,
            screenHeight,
            Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);
        
        return Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
    }

    public static Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        return size;
    }

    public void stopCapture() {
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
}
