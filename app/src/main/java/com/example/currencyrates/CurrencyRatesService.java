package com.example.currencyrates;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.kevinsawicki.http.HttpRequest;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The android service that will handle the Currency Rates updating from the Revolut API. It
 * starts a {@see Timer} job, which schedules a API call at specific time in milliseconds. This
 * class also includes inner LocalBinder which will take care of service bindings,
 * {@see ScheduledUpdater} class is a {@see TimerTask}, which creates {@see RatesDownloader}, that
 * will do the async calls of the API. The {@see RatesDownloader} is actually {@see AsyncTask}
 * job, which uses {@see HttpRequest} and converts the data to a {@see Map} of {@see BigDecimal}
 * values by the currency code as key, and then calls
 * {@link CurrencyRatesService#broadcastCurrentRates(String)} to broadcast the result to every
 * entity, that's interested in the result.
 *
 * @author Nikola Georgiev
 * @version 1.0
 * @since 1.0.0
 */
public class CurrencyRatesService extends Service {

    public static final String TAG = CurrencyRatesService.class.getSimpleName();
    public static final int PERIOD = 1000;
    protected static final String URL_KEY = "api-url";
    protected static final String CURRENCY_RATES_KEY = "currency-rates";
    protected static final String BROADCAST_KEY = "currency-rates-service-broadcast-key";
    private static final String FAIL_SAFE_URL = "about:blank";

    private final IBinder binder = new LocalBinder();

    private Timer timer;
    private String url;

    /**
     * Return the communication channel to the service.  May return null if
     * clients can not bind to the service.  The returned
     * {@link IBinder} is usually for a complex interface
     * that has been <a href="{@docRoot}guide/components/aidl.html">described using
     * aidl</a>.
     *
     * <p><em>Note that unlike other application components, calls on to the
     * IBinder interface returned here may not happen on the main thread
     * of the process</em>.  More information about the main thread can be found in
     * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html">Processes and
     * Threads</a>.</p>
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return an IBinder through which clients can call on to the
     * service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        setJobUrl(intent);

        final ScheduledUpdater repeatTask = new ScheduledUpdater();
        this.timer = new Timer();
        this.timer.schedule(repeatTask, 0, PERIOD);

        return this.binder;
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        this.timer.cancel();
        super.onDestroy();
    }

    /**
     * Called when new clients have connected to the service, after it had
     * previously been notified that all had disconnected in its
     * {@link #onUnbind}.  This will only be called if the implementation
     * of {@link #onUnbind} was overridden to return true.
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     */
    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        setJobUrl(intent);
    }

    /**
     * Called when all clients have disconnected from a particular interface
     * published by the service.  The default implementation does nothing and
     * returns false.
     *
     * @param intent The Intent that was used to bind to this service,
     *               as given to {@link Context#bindService
     *               Context.bindService}.  Note that any extras that were included with
     *               the Intent at that point will <em>not</em> be seen here.
     * @return Return true if you would like to have the service's
     * {@link #onRebind} method later called when new clients bind to it.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        this.timer.cancel();
        return true;
    }

    /**
     * Broadcasts the result data to every entity, that's interested in the result. Each
     * {@see Activity} that is interested in the data, must subscribe for this broadcast events
     * by using the Intent key specified. It will convert the JSON string to a {@see Map} and
     * will be set as {@see Serializable} extra to the {@see Intent}.
     *
     * @param data {@see String} - The raw data as string, it suppose to be JSON object as string.
     * @see LocalBroadcastManager
     */
    private void broadcastCurrentRates(final String data) {

        final Map<String, BigDecimal> ratesMap = CurrencyUtils.convertData(data);
        final Intent intent = new Intent(BROADCAST_KEY);
        intent.putExtra(CURRENCY_RATES_KEY, (Serializable) ratesMap);

        LocalBroadcastManager.getInstance(super.getBaseContext()).sendBroadcast(intent);
    }

    /**
     * Service setter to set the API URL, which will be used to make the {@see HttpRequest}.
     *
     * @param intent {@see Intent} - The intent context that will be used to get the URL as
     *               attached {@see String} extra.
     */
    /* package-private */ void setJobUrl(final Intent intent) {

        if (intent != null) {
            final Bundle extras = intent.getExtras();
            if (extras != null) {
                this.url = extras.getString(URL_KEY, FAIL_SAFE_URL);
            }
        }
    }

    /**
     * The Scheduled job that will call the API every specific milliseconds, using the
     * {@see RateDownloader}. This job must be canceled once the Service has done it's job.
     *
     * @author Nikola Georgiev
     * @version 1.0
     * @since 1.0.0
     */
    private class ScheduledUpdater extends TimerTask {

        private RatesDownloader asyncTask;

        /**
         * The action to be performed by this timer task.
         */
        @Override
        public void run() {
            asyncTask = new RatesDownloader();
            asyncTask.execute(CurrencyRatesService.this.url);
        }

        /**
         * Cancels this timer task.  If the task has been scheduled for one-time
         * execution and has not yet run, or has not yet been scheduled, it will
         * never run.  If the task has been scheduled for repeated execution, it
         * will never run again.  (If the task is running when this call occurs,
         * the task will run to completion, but will never run again.)
         *
         * <p>Note that calling this method from within the <tt>run</tt> method of
         * a repeating timer task absolutely guarantees that the timer task will
         * not run again.
         *
         * <p>This method may be called repeatedly; the second and subsequent
         * calls have no effect.
         *
         * @return true if this task is scheduled for one-time execution and has
         * not yet run, or this task is scheduled for repeated execution.
         * Returns false if the task was scheduled for one-time execution
         * and has already run, or if the task was never scheduled, or if
         * the task was already cancelled.  (Loosely speaking, this method
         * returns <tt>true</tt> if it prevents one or more scheduled
         * executions from taking place.)
         */
        @Override
        public boolean cancel() {
            return !asyncTask.isCancelled() || asyncTask.cancel(true);
        }
    }

    /**
     * The async job, which will be performed in order to call the Revolut API and get the
     * Currency Rates. The raw data will be converted and broadcast to each subscriber. NOTE:
     * Once the job is done, the {@see AsyncTask} cannot be reused. To reused it, create new
     * instance of it.
     *
     * @author Nikola Georgiev
     * @version 1.0
     * @since 1.0.0
     */
    private class RatesDownloader extends AsyncTask<String, Long, String> {

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
        protected String doInBackground(String... urls) {
            try {
                final HttpRequest request = HttpRequest.get(urls[0]);
                //Accept all certificates
                request.trustAllCerts();
                String dataAsString = null;
                if (request.ok()) {
                    //Copy response to dataAsString
                    dataAsString = request.body();
                }
                return dataAsString;
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
         * @param data The result of the operation computed by {@link #doInBackground}.
         * @see #onPreExecute
         * @see #doInBackground
         * @see #onCancelled()
         */
        @Override
        protected void onPostExecute(String data) {
            if (data != null) {
                Log.i(TAG, "Data successfully downloaded from " + url);
                CurrencyRatesService.this.broadcastCurrentRates(data);
            } else
                Log.w(TAG, "Download failed for URL: " + url);
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     *
     * @author Nikola Georgiev
     * @version 1.0
     * @since 1.0.0
     */
    /* package-private */ class LocalBinder extends Binder {

        /* package-private */ CurrencyRatesService getService() {
            // Return this instance of LocalService so clients can call public methods
            return CurrencyRatesService.this;
        }
    }
}
