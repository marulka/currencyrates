package com.example.currencyrates;

import android.widget.ImageView;

/**
 * @author Nikola Georgiev
 * @version 1.0
 * @since 1.0.0
 */
interface IFileDownloadHandler {

    /**
     * Sets icon {@see Bitmap} to specified {@see ImageView}.
     *
     * @param view {@see ImageView} - An instance to the image view where the Bitmap result will by added to.
     * @param url  {@see String} - The url of the image, which suppose to be downloaded and set to an {@see ImageView}.
     */
    void setIconByUrl(ImageView view, String url);
}
