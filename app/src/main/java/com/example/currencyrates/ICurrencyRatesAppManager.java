package com.example.currencyrates;

import android.view.KeyEvent;
import android.widget.TextView;

/**
 * Interface that will be used to control the currency rates service and list.
 *
 * @author Nikola Georgiev
 * @version 1.0
 * @since 1.0.0
 */
/* package-private */ interface ICurrencyRatesAppManager {

    /**
     * Changes the base currency with the currency at a specified position in the currency list.
     * Then restarts the service and scrolls to the 0 index of the {@see ListView}.
     *
     * @param currencyCode {@see String} - The currency code of clicked list item.
     */
    /* package-private */ void changeBaseCurrency(final String currencyCode);

    /**
     * Callback to handle text editor change events.
     *
     * @param v        The view that was clicked.
     * @param actionId Identifier of the action.  This will be either the
     *                 identifier you supplied, or {@link android.view.inputmethod.EditorInfo#IME_NULL
     *                 EditorInfo.IME_NULL} if being called due to the enter key
     *                 being pressed.
     * @param event    If triggered by an enter key, this is the event;
     */
    /* package-private */ boolean onEditorAction(TextView v, int actionId, KeyEvent event);

    /**
     * Updated each time the list adapter has new data.
     */
    /* package-private */ void onListDataLoaded();
}
