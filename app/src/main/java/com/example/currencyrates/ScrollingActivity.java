package com.example.currencyrates;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.kevinsawicki.http.HttpRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * The main activity for this application.
 *
 * @author Nikola Georgiev
 * @version 1.0
 * @since 1.0.0
 */
public class ScrollingActivity extends AppCompatActivity implements ICurrencyRatesAppManager {

    public static final String TAG = ScrollingActivity.class.getSimpleName();
    public static final String REVOLUT_BASE_URL = "https://revolut.duckdns.org/latest?base=";
    public static final String COUNTRY_FLAGS_API_URL = "https://www.countryflags.io/";
    public static final String ICON_TYPE_AND_SIZE = "/shiny/64.png";

    private static final BigDecimal DEFAULT_MULTIPLIER = BigDecimal.ONE;
    private static final BigDecimal DEFAULT_QUANTITY = BigDecimal.ONE;

    private List<CurrencyModel> currenciesList = new LinkedList<>();
    private BroadcastReceiver dataReceiver;
    private ServiceConnection serviceConnection;
    private CurrencyRatesService currencyRatesService;
    private CurrenciesListAdapter listAdapter;
    private boolean isServiceBound;
    private boolean isListLoaded;
    private BigDecimal multiplier = DEFAULT_MULTIPLIER;
    private String baseCurrency = "EUR";
    private View listHeader;

    @BindView(R.id.list)
    /* package-private */ ListView listView;
    @BindView(R.id.progressBar)
    /* package-private */ ProgressBar progressBar;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // ********************* Implementing system callbacks - Starts Here ************************ //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Initializing main activity fields, as well as the data receiver, toolbar, list adapter, and list header.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        super.setContentView(R.layout.activity_scrolling);
        ButterKnife.bind(this);

        /* Register to receive broadcast messages. We are registering an observer (dataReceiver) to receive Intents  with actions named "currency-rates". */
        initDataReceiver();

        final Toolbar toolbar = super.findViewById(R.id.toolbar);
        super.setSupportActionBar(toolbar);

        this.listAdapter = new CurrenciesListAdapter(this, this.currenciesList);
        this.listAdapter.registerDataSetObserver(createListDataObserver());
        this.listView.setAdapter(this.listAdapter);
        this.listView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        this.listView.setItemsCanFocus(true);
    }

    /**
     * Called after {@link #onCreate} &mdash; or after {@link #onRestart} when
     * the activity had been stopped, but is now again being displayed to the
     * user.  It will be followed by {@link #onResume}.
     *
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onCreate
     * @see #onStop
     * @see #onResume
     */
    @Override
    protected void onStart() {
        super.onStart();
        /* Bind to CurrencyRatesService */
        initServiceConnection();
        startService();
    }

    /**
     * Called when you are no longer visible to the user.  You will next
     * receive either {@link #onRestart}, {@link #onDestroy}, or nothing,
     * depending on later user activity.
     *
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onRestart
     * @see #onResume
     * @see #onSaveInstanceState
     * @see #onDestroy
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (this.isServiceBound) {
            unbindService(this.serviceConnection);
        }
        this.isServiceBound = false;
    }

    /**
     * Initialize the contents of the Activity's standard options menu.  You
     * should place your menu items in to <var>menu</var>.
     *
     * <p>This is only called once, the first time the options menu is
     * displayed.  To update the menu every time it is displayed, see
     * {@link #onPrepareOptionsMenu}.
     *
     * <p>The default implementation populates the menu with standard system
     * menu items.  These are placed in the {@link Menu#CATEGORY_SYSTEM} group so that
     * they will be correctly ordered with application-defined menu items.
     * Deriving classes should always call through to the base implementation.
     *
     * <p>You can safely hold on to <var>menu</var> (and any items created
     * from it), making modifications to it as desired, until the next
     * time onCreateOptionsMenu() is called.
     *
     * <p>When you add items to the menu, you can implement the Activity's
     * {@link #onOptionsItemSelected} method to handle them there.
     *
     * @param menu The options menu in which you place your items.
     * @return You must return true for the menu to be displayed;
     * if you return false it will not be shown.
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     * The default implementation simply returns false to have the normal
     * processing happen (calling the item's Runnable or sending a message to
     * its Handler as appropriate).  You can use this method for any items
     * for which you would like to do processing without those other
     * facilities.
     *
     * <p>Derived classes should call through to the base class for it to
     * perform the default menu handling.</p>
     *
     * @param item The menu item that was selected.
     * @return boolean Return false to allow normal menu processing to
     * proceed, true to consume it here.
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            super.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Perform any final cleanup before an activity is destroyed.  This can
     * happen either because the activity is finishing (someone called
     * {@link #finish} on it, or because the system is temporarily destroying
     * this instance of the activity to save space.  You can distinguish
     * between these two scenarios with the {@link #isFinishing} method.
     *
     * <p><em>Note: do not count on this method being called as a place for
     * saving data! For example, if an activity is editing data in a content
     * provider, those edits should be committed in either {@link #onPause} or
     * {@link #onSaveInstanceState}, not here.</em> This method is usually implemented to
     * free resources like threads that are associated with an activity, so
     * that a destroyed activity does not leave such things around while the
     * rest of its application is still running.  There are situations where
     * the system will simply kill the activity's hosting process without
     * calling this method (or any others) in it, so it should not be used to
     * do things that are intended to remain around after the process goes
     * away.
     *
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onPause
     * @see #onStop
     * @see #finish
     * @see #isFinishing
     */
    @Override
    protected void onDestroy() {
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        /* Unregister the data receiver, because the activity is about to be closed. */
        localBroadcastManager.unregisterReceiver(this.dataReceiver);
        super.onDestroy();
    }

    /**
     * Changes the base currency with the currency at a specified position in the currency list.
     * Then restarts the service and scrolls to the 0 index of the {@see ListView}.
     *
     * @param currencyCode {@see String} - The currency code of clicked list item.
     */
    @Override
    public void changeBaseCurrency(final String currencyCode) {

        if (StringUtils.isNotEmpty(currencyCode)) {
            super.unbindService(this.serviceConnection);
            this.baseCurrency = currencyCode;
            this.multiplier = DEFAULT_QUANTITY;
            startService();
            this.listView.smoothScrollToPosition(0);
            replaceListHeader();
        }
    }

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
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

        if (v == null || StringUtils.isEmpty(v.getText())) return false;
        try {
            final BigDecimal input = new BigDecimal(v.getText().toString());
            if (BigDecimal.ZERO.compareTo(input) < 0) {
                multiplier = input;
                return true;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace(); // TODO
        }
        return false;
    }

    /**
     * Updated each time the list adapter has new data.
     */
    @Override
    public void onListDataLoaded() {
        if (this.progressBar != null && !this.isListLoaded) {
            this.progressBar.setVisibility(View.GONE);
        }
        if (!this.isListLoaded) {
            addListHeader();
            this.isListLoaded = true;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // *********************** Implementing system callbacks - Ends Here ************************ //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Initializing the {@see ServiceConnection}, implementing its callbacks, and binding the service.
     */
    private void initServiceConnection() {

        /* Defines callbacks for service binding, passed to bindService() */
        this.serviceConnection = new ServiceConnection() {


            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                /* We've bound to CurrencyRatesService, cast the IBinder and get CurrencyRatesService instance */
                final CurrencyRatesService.LocalBinder binder = (CurrencyRatesService.LocalBinder) service;
                currencyRatesService = binder.getService();
                isServiceBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                isServiceBound = false;
            }
        };
    }

    /**
     * Call this method in order to create {@see Intent} with the URL as extra, based on the base
     * currency, and start the service.
     */
    private void startService() {

        final Intent intent = new Intent(this, CurrencyRatesService.class);
        intent.putExtra(CurrencyRatesService.URL_KEY, REVOLUT_BASE_URL + this.baseCurrency);
        super.bindService(intent, this.serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Initializing the data receiver as {@see BroadcastReceiver} and implementing its callbacks.
     * When the data receiver receives a serialized data as extra, it parses it to a list of
     * {@see CurrenciesListAdapter.CurrencyModel} and calls the
     * {@link ScrollingActivity#updateListAdapterData(List)} method in order to update the list
     * using this data.
     */
    private void initDataReceiver() {

        // Our handler for received Intents. This will be called whenever an Intent with an action named "currency-rates" is broadcast.
        this.dataReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                // Get extra data included in the Intent
                final Serializable extra = intent.getSerializableExtra(CurrencyRatesService.CURRENCY_RATES_KEY);
                if (extra instanceof Map) {
                    final Map<String, BigDecimal> dataMap = (Map) extra;
                    final List<CurrencyModel> currencyRates = parseCurrencyRates(dataMap);
                    if (!currencyRates.isEmpty()) {
                        updateListAdapterData(currencyRates);
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(this.dataReceiver,
                new IntentFilter(CurrencyRatesService.BROADCAST_KEY));
    }

    /**
     * Creates implementation of anonymous class of type {@see DataSetObserver}. This instance
     * should be registered to the {@see ArrayAdapter} in order to be used as callback.
     *
     * @return {@see DataSetObserver} - An instance of implementation to a list adapter data
     * observer.
     */
    private DataSetObserver createListDataObserver() {
        return new DataSetObserver() {

            /**
             * This method is called when the entire data set has changed,
             * most likely through a call to {@link android.database.Cursor#requery()} on a
             * {@link android.database.Cursor}.
             */
            @Override
            public void onChanged() {
                onListDataLoaded();
                super.onChanged();
            }
        };
    }

    /**
     * Creates header view based on a specific inflated layout, loads data as
     * {@see AbstractMap.SimpleEntry}, and sets listeners to it.
     *
     * @return {@see View} - Returns header view.
     */
    private View createListHeader() {

        final LayoutInflater layoutInflater =
                (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View headerView = layoutInflater.inflate(R.layout.list_item, null, false);
        final CurrenciesListAdapter.ListItem listHeader =
                this.listAdapter.createListHeader(headerView, getFocusChangeListener());

        final Map.Entry<String, BigDecimal> baseCurrency =
                new AbstractMap.SimpleEntry<>(this.baseCurrency, DEFAULT_QUANTITY);
        listHeader.setValues(buildCurrencyModel(baseCurrency));
        headerView.setTag(listHeader);
        return headerView;
    }

    /**
     * Creates anonymous class of type {@see OnFocusChangeListener} to handle view focus change
     * events.
     *
     * @return {@see OnFocusChangeListener} - Returns implemented anonymous class of type
     * {@see OnFocusChangeListener}.
     */
    private View.OnFocusChangeListener getFocusChangeListener() {
        return new View.OnFocusChangeListener() {
            /**
             * Called when the focus state of a view has changed.
             *
             * @param v        The view whose state has changed.
             * @param hasFocus The new focus state of v.
             */
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && v instanceof EditText) {
                    final EditText editText = (EditText) v;
                    editText.requestFocus();
                    editText.selectAll();
                }
            }
        };
    }

    /**
     * This method converts a map with {@see BigDecimal} values to {@see ArrayList} of
     * {@see CurrenciesListAdapter.CurrencyModel} items.
     *
     * @param dataMap {@see Map} - A map of currency rates as {@see BigDecimal} using their
     *                currency codes as key.
     * @return {@see List} - Returns {@see ArrayList} of {@see CurrenciesListAdapter.CurrencyModel}.
     * In case te map is empty or has null pointer, this method will return an empty list.
     */
    private List<CurrencyModel> parseCurrencyRates(Map<String, BigDecimal> dataMap) {

        final List<CurrencyModel> currencies = new ArrayList<>();
        if (dataMap != null && !dataMap.isEmpty()) {
            for (final Map.Entry<String, BigDecimal> entry : dataMap.entrySet()) {
                currencies.add(buildCurrencyModel(entry));
            }
        }
        return currencies;
    }

    /**
     * Find the current header in the {@see ListView} and replaces it with newly created header
     * {@see View}.
     *
     * @see ScrollingActivity#createListHeader()
     */
    private void replaceListHeader() {

        /* Remove the old header */
        this.listView.removeHeaderView(this.listHeader);

        /* Add new header */
        addListHeader();

        /* Request focus to the new header */
        final Object tag = this.listHeader.getTag();
        if (tag instanceof CurrenciesListAdapter.ListItem) {
            final CurrenciesListAdapter.ListItem header = (CurrenciesListAdapter.ListItem) tag;
            header.requestFocus();
        }
    }

    /**
     * Adds new list header to the {@see ListView}. NOTE: remove the previous first, if any.
     */
    private void addListHeader() {
        final View newListHeader = createListHeader();
        this.listView.addHeaderView(newListHeader);
        this.listHeader = newListHeader;
    }

    /**
     * Updates the currencies list and loads it to the list adapter.
     *
     * @param currencies {@see List} - A list of {@see CurrenciesListAdapter.CurrencyModel} items.
     */
    private void updateListAdapterData(List<CurrencyModel> currencies) {
        this.currenciesList = currencies;
        if (this.listAdapter != null) {
            this.listAdapter.addAll(currenciesList);
        }
    }

    /**
     * Converts {@see Map.Entry} of {@see String} and {@see BigDecimal} pair to a
     * {@see CurrenciesListAdapter.CurrencyModel}. The map entry key is the currency code and the map
     * entry value is the currency rate.
     *
     * @param entry {@see Map.Entry} - A map entry of {@see String} and {@see BigDecimal} pair.
     * @return {@see CurrenciesListAdapter.CurrencyModel} - Returns currency model based on the map entry.
     */
    private CurrencyModel buildCurrencyModel(final Map.Entry<String, BigDecimal> entry) {

        Validate.notNull(entry, "Currency entry should NOT have Null Pointer. ");

        final String currencyCode = entry.getKey();
        final Uri iconUri = getIconUri(currencyCode);
        final String currencyName = getStringByResId(currencyCode);
        final BigDecimal currencyRate = this.multiplier.multiply(entry.getValue());
        return new CurrencyModel(iconUri, currencyCode, currencyName, currencyRate);
    }

    /**
     * Downloads country icon picture based on a 3 letter currency code. This currency code will
     * be converted to a 2 letter country code and used as parameter into the RESTful request to
     * the API.
     *
     * @param currencyCode {@see String} - A 3 letter currency code that will used to download an
     *                     icon for.
     * @return {@see Uri} - The image icon file {@see Uri} in the internal device memory.
     * @see ImageFileDownloader
     * @see FileUtils
     */
    private Uri getIconUri(String currencyCode) {

        final String iconUrl = buildCountryIconUrlByCurrencyCode(currencyCode);
        if (!FileUtils.isFileExisting(this, currencyCode)) {
            new ImageFileDownloader(iconUrl, currencyCode).execute();
        }
        return FileUtils.getIconUri(this, currencyCode);
    }

    /**
     * Gets currency name based on the 3 letter currency code. All the currency names are suppose
     * to be added into the String resources of this package and the string resource names should
     * be the currency code.
     *
     * @param currencyCode {@see String} - A 3 letter currency code that will used to get a
     *                     currency name for.
     * @return {@see String} - The currency name based on the 3 letter currency code.
     */
    private String getStringByResId(final String currencyCode) {

        final Resources res = super.getResources();
        final int nameId = res.getIdentifier(currencyCode, "string", super.getPackageName());
        final String currencyName = (nameId > 0) ? res.getString(nameId) : null;
        return StringUtils.isEmpty(currencyName) ? currencyCode : currencyName;
    }

    /**
     * Builds a country icon URL as string that will be used to download the picture.
     *
     * @param currencyCode {@see String} - A 3 letter currency code that will used to get a
     *                     country icon URL for.
     * @return {@see String} - The complete country icon URL.
     */
    private String buildCountryIconUrlByCurrencyCode(final String currencyCode) {

        if (StringUtils.isBlank(currencyCode) || currencyCode.length() < 2) {
            return null;
        }
        return StringUtils.join(COUNTRY_FLAGS_API_URL, currencyCode.substring(0, 2).toLowerCase(), ICON_TYPE_AND_SIZE);
    }

    /**
     * A class that extends the {@see AsyncTask} to provide ability to download file
     * asynchronously using {@see HttpRequest}. This {@see AsyncTask} downloads the image file
     * and converts it to {@see Bitmap} image, then stores it into the temporary internal app
     * storage.
     *
     * @author Nikola Georgiev
     * @version 1.0
     * @see FileUtils#saveTempBitmap(Context, Bitmap, String)
     * @since 1.0.0
     */
    /* package-private */ class ImageFileDownloader extends AsyncTask<String, Long, Bitmap> {

        private final String url;
        private final String fileName;

        /**
         * The main constructor of the class.
         *
         * @param url      {@see String} - The image URL, which will be downloaded.
         * @param fileName {@see String} - The image filename, which will be used to store the
         *                 image file.
         */
        /* package-private */ ImageFileDownloader(final String url, final String fileName) {
            this.url = url;
            this.fileName = fileName;
        }

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param urls The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected Bitmap doInBackground(String... urls) {

            try {
                final HttpRequest request = HttpRequest.get(this.url);
                //Accept all certificates
                request.trustAllCerts();
                InputStream inputStream = null;
                if (request.ok()) {
                    inputStream = request.buffer();
                }
                return BitmapFactory.decodeStream(inputStream);
            } catch (HttpRequest.HttpRequestException exception) {
                return null;
            }
        }

        /**
         * <p>Runs on the UI thread after {@link #doInBackground}. The
         * specified result is the value returned by {@link #doInBackground}.</p>
         *
         * <p>This method won't be invoked if the task was cancelled.</p>
         *
         * @param bitmap The result of the operation computed by {@link #doInBackground}.
         * @see #onPreExecute
         * @see #doInBackground
         * @see #onCancelled()
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                Log.i(TAG, "Downloaded file from URL: " + ImageFileDownloader.this.url);
                FileUtils.saveTempBitmap(ScrollingActivity.this, bitmap, ImageFileDownloader.this.fileName);
            } else
                Log.w(TAG, "Download failed.");
        }
    }
}
