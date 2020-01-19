package com.example.currencyrates;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class that helps extracting common currency logics.
 *
 * @author Nikola Georgiev
 * @version 1.0
 * @since 1.0.0
 */
/* package-private */ final class CurrencyUtils {

    /* package-private */  static final String TAG = CurrencyUtils.class.getSimpleName();
    private static final String RATES_NODE = "rates";

    /**
     * Parses the JSON string raw data to a {@see JSONObject}, and converts it to a {@see Map}.
     *
     * @param data {@see String} - The raw data as string, it suppose to be JSON object as string.
     * @return {@see Map} - Returns map of {@see BigDecimal} values and the Currency Codes as
     * keys. It will return an empty map, in case the {@param data} is not a JSON string or
     * doesn't have the required data.
     */
    /* package-private */
    static Map<String, BigDecimal> convertData(final String data) {

        final JSONObject jsonData = parseJSONData(data);
        final Map<String, BigDecimal> ratesMap = new HashMap<>();
        if (jsonData != null && !jsonData.isNull(RATES_NODE)) {
            try {
                final JSONObject rates = jsonData.getJSONObject(RATES_NODE);
                if (rates != null && rates.length() > 0) {
                    final Iterator<String> keys = rates.keys();
                    while (keys.hasNext()) {
                        final String currency = keys.next();
                        final BigDecimal rate = new BigDecimal(rates.getString(currency));
                        ratesMap.put(currency, rate);
                    }
                }
            } catch (JSONException | NumberFormatException e) {
                Log.e(TAG, "An error occurred, while trying to convert data.", e);
            }
        }
        return ratesMap;
    }

    /**
     * Parses the JSON string raw data to a {@see JSONObject}.
     *
     * @param data {@see String} - The raw data as string, it suppose to be JSON object as string.
     * @return {@see JSONObject} - The JSON string raw data as {@see JSONObject}. In case an
     * error occurs during the parsing process, it will return Null Pointer.
     */
    /* package-private */
    static JSONObject parseJSONData(final String data) {

        try {
            return new JSONObject(data);
        } catch (JSONException je) {
            Log.e(TAG, "An error occurred, while trying to parse a string to JSON object.", je);
        }
        return null;
    }

    /**
     * This method formats a parsed {@see BigDecimal} value to a string according to the current
     * device locale.
     *
     * @param number {@see BigDecimal} - The decimal number that should be formatted.
     * @return {@see String} - The formatted decimal number as string.
     */
    /* package-private */
    static String formatBigDecimalAsString(final BigDecimal number) {

        if (number == null) return null;

        final NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        if (nf instanceof DecimalFormat) {
            final DecimalFormat formatter = (DecimalFormat) nf;
            formatter.setDecimalSeparatorAlwaysShown(true);
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);
            formatter.setRoundingMode(RoundingMode.HALF_UP);
            try {
                return formatter.format(number);
            } catch (NumberFormatException nfe) {
                Log.w(TAG, "The number input is wrong type.", nfe);
            }
        }
        return null;
    }
}
