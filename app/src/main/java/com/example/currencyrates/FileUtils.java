package com.example.currencyrates;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;

/**
 * File Utilities class that helps saving image files.
 *
 * @author Nikola Georgiev
 * @version 1.0
 * @since 1.0.0
 */
/* package-private */ final class FileUtils {

    /* package-private */ static final String TAG = FileUtils.class.getSimpleName();

    /**
     * Use this method to save a temporary image file, in case the internal device storage is
     * writable.
     *
     * @param context  {@see Context} - The base application context.
     * @param bitmap   {@see Bitmap} - An instance of the image bitmap that will be saved.
     * @param fileName {@see String} - The name of the file that will be saved.
     * @return {@see Uri} - The Uri path of the saved file. NULL - in case either some of the
     * context, bitmap, and fileName have Null Pointer.
     */
    /* package-private */
    static Uri saveTempBitmap(final Context context, final Bitmap bitmap, final String fileName) {

        if (context == null || bitmap == null || StringUtils.isEmpty(fileName)) {
            return null;
        }
        if (isInternalStorageWritable(context)) {
            return saveImage(context, bitmap, fileName);
        }
        return null;
    }

    /**
     * Use this method to save image file, in case the internal device storage is writable.
     *
     * @param context  {@see Context} - The base application context.
     * @param bitmap   {@see Bitmap} - An instance of the image bitmap that will be saved.
     * @param fileName {@see String} - The name of the file that will be saved.
     * @return {@see Uri} - The Uri path of the saved file. NULL - in case either some of the
     * context, bitmap, and fileName have Null Pointer.
     */
    private static Uri saveImage(final Context context, final Bitmap bitmap, final String fileName) {

        final File file = createTempFile(context, fileName);
        if (file == null)
            return null;

        try {
            final FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Cannot convert and save bitmap file with name: " + fileName, e);
            return null;
        }
        return Uri.fromFile(file);
    }

    /**
     * Gets icon Uri based on the file name of it's image.
     *
     * @param context  {@see Context} - The base application context.
     * @param fileName {@see String} - The name of the file that is saved.
     * @return {@see Uri} - The Uri path of the saved file. NULL - in case either some of the
     * context, and fileName have Null Pointer, or the file cannot be created.
     */
    /* package-private */
    static Uri getIconUri(final Context context, final String fileName) {

        final File tempFile = createTempFile(context, fileName);
        return (tempFile != null) ? Uri.fromFile(tempFile) : null;
    }

    /**
     * Creates temporary file in the cache directory "country_images", in case file cannot be
     * created it will return Null Pointer.
     *
     * @param context  {@see Context} - The base application context.
     * @param fileName {@see String} - The name of the file that is saved.
     * @return {@see Uri} - The Uri path of the saved file. NULL - in case either some of the
     * context, and fileName have Null Pointer, or the file cannot be created.
     */
    /* package-private */
    static File createTempFile(final Context context, final String fileName) {

        if (context == null || fileName == null)
            return null;

        final String cacheRoot = context.getFilesDir().getAbsolutePath();
        final File cacheStorageDir = new File(cacheRoot + "/country_images");
        if (!cacheStorageDir.exists() || !cacheStorageDir.isDirectory()) {
            try {
                if (!cacheStorageDir.mkdirs()) {
                    return null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Cannot create dir: " + cacheStorageDir, e);
                return null;
            }
        }
        return new File(cacheStorageDir, fileName + ".png");
    }

    /**
     * Checks whether a given file with name exists in the cache directory.
     *
     * @param context  {@see Context} - The base application context.
     * @param fileName {@see String} - The name of the file that is saved.
     * @return {@see boolean} - True - in case the file exists, False - in case the file does not
     * exist.
     */
    /* package-private */
    static boolean isFileExisting(final Context context, final String fileName) {
        final File tempFile = createTempFile(context, fileName);
        return tempFile != null && tempFile.exists();
    }

    /**
     * Checks if internal storage is available for read and write
     *
     * @param context
     * @return {@see boolean} - True - in case the internal storage of the device is writable,
     * False - in case has no permissions to write in the device internal storage.
     */
    /* package-private */
    static boolean isInternalStorageWritable(final Context context) {

        if (context == null)
            return false;

        // Create the storage directory if it does not exist
        final String cacheRoot = context.getFilesDir().getAbsolutePath();
        final File cacheStorageDir = new File(cacheRoot);
        if (cacheStorageDir.exists() && cacheStorageDir.isDirectory()) {
            return cacheStorageDir.canWrite();
        }
        return false;
    }
}
