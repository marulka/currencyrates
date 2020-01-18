package com.example.currencyrates;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;

/**
 * @author Nikola Georgiev
 * @version 1.0
 * @since 1.0.0
 */
/* package-private */ final class FileUtils {

    /**
     * @param context
     * @param bitmap
     * @param fileName
     * @return
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
     * @param context
     * @param bitmap
     * @param fileName
     * @return
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
            e.printStackTrace();
            return null;
        }
        return Uri.fromFile(file);
    }

    /**
     * @param context
     * @param fileName
     * @return
     */
    /* package-private */
    static Uri getIconUri(final Context context, final String fileName) {

        final File tempFile = createTempFile(context, fileName);
        return (tempFile != null) ? Uri.fromFile(tempFile) : null;
    }

    /**
     * @param context
     * @param fileName
     * @return
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
                e.printStackTrace();
                return null;
            }
        }
        return new File(cacheStorageDir, fileName + ".png");
    }

    /**
     * @param context
     * @param fileName
     * @return
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
     * @return
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
