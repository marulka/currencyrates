package com.example.currencyrates;

import android.net.Uri;

import java.math.BigDecimal;

/**
 * A model object that will hold all the necessary information that will be shown on the GUI.
 *
 * @author Nikola Georgiev
 * @version 1.0
 * @since 1.0.0
 */
/* package-private */ final class CurrencyModel {

    private final Uri countryFlagIconUri;
    private final String currencyCode;
    private final String currencyName;
    private final BigDecimal currencyRate;

    /**
     * The main constructor of this class.
     *
     * @param countryFlagIconUri {@see Uri} - The country flag icon file Uri.
     * @param currencyCode       {@see String} - The 3 letter currency code that will used.
     * @param currencyName       {@see String} - The currency name, which will be shown.
     * @param currencyRate       {@see BigDecimal} - The currency rate of the currency pair
     *                           with the base currency.
     */
    /* package-private */ CurrencyModel(final Uri countryFlagIconUri, final String currencyCode,
                                        final String currencyName, final BigDecimal currencyRate) {
        this.countryFlagIconUri = countryFlagIconUri;
        this.currencyCode = currencyCode;
        this.currencyName = currencyName;
        this.currencyRate = currencyRate;
    }

    /* package-private */ Uri getCountryFlagIconUri() {
        return countryFlagIconUri;
    }

    /* package-private */ String getCurrencyCode() {
        return currencyCode;
    }

    /* package-private */ String getCurrencyName() {
        return currencyName;
    }

    /* package-private */ BigDecimal getCurrencyRate() {
        return currencyRate;
    }
}
