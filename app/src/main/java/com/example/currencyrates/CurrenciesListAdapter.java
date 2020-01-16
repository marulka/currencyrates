package com.example.currencyrates;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * The list adapter based on the {@see ArrayAdapter}, which is used for the {@see ListView}. This
 * class also contains the {@see CurrencyModel}, which will be used to hold the list item data,
 * and the {@see ListItem} view holder class, which contains each list item view instances in
 * order ButterKnife to maintain better the memory allocation.
 *
 * @author Nikola Georgiev
 * @version 1.0
 * @since 1.0.0
 */
/* package-private */ class CurrenciesListAdapter extends ArrayAdapter<CurrencyModel> {

    private final LayoutInflater inflater;
    private SparseArray<ListItem> listItems;

    private List<CurrencyModel> currencyRates;

    /**
     * Constructor. This constructor will result in the underlying data collection being
     * immutable, so methods such as {@link #clear()} will throw an exception.
     *
     * @param context The current context.
     * @param rates   The rates to represent in the ListView.
     */
    /* package-private */ CurrenciesListAdapter(@NonNull Activity context, @NonNull List<CurrencyModel> rates) {
        super(context, 0, rates);
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.listItems = new SparseArray<>();
    }

    /**
     * A Callback that will be called every time the {@see ListView} needs the specific item
     * {@see CurrencyModel} at specific position.
     *
     * @param position {@see int} - The specific position at the list.
     * @return {@see CurrencyModel} - The currency information to be shown as a model object.
     */
    @Nullable
    @Override
    public CurrencyModel getItem(int position) {
        return (this.currencyRates != null && !this.currencyRates.isEmpty()) ? this.currencyRates.get(position) : null;
    }

    /**
     * A Callback, that will be called every time the {@see ListView} needs to get the list item
     * {@see View} at specific list position. Implemented logic to create {@see ListItem} view if
     * not present at the specified position. An instance to the item {@see ListItem} will
     * be added to the view tag (see {@link View#getTag()}).
     *
     * @param position    {@see int} - The position of the required view in the list.
     * @param convertView {@see View} - An instance of the view, if already created.
     * @param parent      {@see ViewGroup} - The parent view or view group, if any.
     * @return {@see View} - The view at the specified list position.
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final ListItem item;
        final CurrencyModel currencyModel = getItem(position);
        if (convertView != null) {
            item = (ListItem) convertView.getTag();
        } else {
            convertView = this.inflater.inflate(R.layout.list_item, parent, false);
            item = new ListItem(convertView, v -> CurrenciesListAdapter.this.onClickCallback(position),
                    (v, hasFocus) -> {
                        if (hasFocus) {
                            CurrenciesListAdapter.this.onClickCallback(position);
                        }
                    });
            this.listItems.put(position, item);
        }
        item.setValues(currencyModel);
        convertView.setTag(item);
        return convertView;
    }

    /**
     * A Callback, that will be called by the {@see ListView} every time it needs the full list
     * items count.
     *
     * @return {@see int} - The whole list items count.
     */
    @Override
    public int getCount() {
        return (this.currencyRates != null) ?
                this.currencyRates.size() : 0;
    }

    /**
     * Adds the specified Collection at the end of the array.
     *
     * @param items The Collection to add at the end of the array.
     * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
     *                                       is not supported by this list
     * @throws ClassCastException            if the class of an element of the specified
     *                                       collection prevents it from being added to this list
     * @throws NullPointerException          if the specified collection contains one
     *                                       or more null elements and this list does not permit null
     *                                       elements, or if the specified collection is null
     * @throws IllegalArgumentException      if some property of an element of the
     *                                       specified collection prevents it from being added to this list
     */
    @Override
    public void addAll(@NonNull Collection<? extends CurrencyModel> items) {
        if (items instanceof List) {
            this.currencyRates = (List<CurrencyModel>) items;
        }
        super.addAll(items);
    }

    /**
     * Changes the base currency according to the clicked list item.
     *
     * @param position {@see int} - The item position in the list.
     */
    private void onClickCallback(final int position) {

        final Context context = getContext();
        final CurrencyModel item = getItemByPosition(position);
        if (context instanceof ICurrencyRatesAppManager && item != null) {
            final ICurrencyRatesAppManager serviceManager = (ICurrencyRatesAppManager) context;
            final String currencyCode = item.getCurrencyCode();
            serviceManager.changeBaseCurrency(currencyCode);
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
    private boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {

        final Context context = getContext();
        if (context instanceof ICurrencyRatesAppManager) {
            final ICurrencyRatesAppManager serviceManager = (ICurrencyRatesAppManager) context;
            serviceManager.onEditorAction(v, actionId, event);
            return true;
        }
        return false;
    }

    /**
     * Gets the model of an item with specific position in the list.
     *
     * @param position {@see int} - The item position in the list.
     * @return {@see CurrencyModel} - The item model at specific position.
     */
    private CurrencyModel getItemByPosition(int position) {

        final ListItem item = (this.listItems != null && this.listItems.size() != 0) ?
                this.listItems.get(position) : null;
        return (item != null) ? item.getValues() : null;
    }

    /**
     * Creates new instance of {@see ListHeader} and adds local editor action listener
     * implementation.
     *
     * @param headerView          {@see View} - The list header view.
     * @param focusChangeListener
     * @return {@see ListHeader} - The new list header instance.
     */
    /* package-private */ ListHeader createListHeader(final View headerView,
                                                      final View.OnFocusChangeListener focusChangeListener) {
        return new ListHeader(headerView, this::onEditorAction, focusChangeListener);
    }

    /**
     * A {@see View} holder class, which wraps all the list header view instances.
     *
     * @author Nikola Georgiev
     * @version 1.0
     * @since 1.0.0
     */
    /* package-private */ static class ListHeader extends ListItem {

        /**
         * The main constructor of this class.
         *
         * @param view                 {@see View} - The parent view of this view wrapper.
         * @param editorActionListener
         * @param focusChangeListener
         */
        /* package-private */ ListHeader(final View view,
                                         final TextView.OnEditorActionListener editorActionListener,
                                         final View.OnFocusChangeListener focusChangeListener) {
            super(view);
            super.onEditorActionListener = editorActionListener;
            super.onFocusChangeListener = focusChangeListener;
        }
    }

    /**
     * A {@see View} holder class, which wraps all the list item view instances.
     *
     * @author Nikola Georgiev
     * @version 1.0
     * @since 1.0.0
     */
    /* package-private */ static class ListItem {

        @BindView(R.id.listItemIcon)
        /* package-private */ ImageView icon;
        @BindView(R.id.listItemCurrencyCode)
        /* package-private */ TextView fieldCode;
        @BindView(R.id.listItemCurrencyName)
        /* package-private */ TextView fieldName;
        @BindView(R.id.listItemCurrencyRate)
        /* package-private */ EditText fieldRate;

        private CurrencyModel currencyModel;
        private View.OnClickListener onClickListener;
        private View.OnFocusChangeListener onFocusChangeListener;
        private TextView.OnEditorActionListener onEditorActionListener;

        /**
         * The main constructor of this class.
         *
         * @param view {@see View} - The parent view of this view wrapper.
         */
        /* package-private */ ListItem(final View view) {
            ButterKnife.bind(ListItem.this, view);
        }

        /**
         * Overloaded constructor of this class.
         *
         * @param view          {@see View} - The parent view of this view wrapper.
         * @param clickListener
         * @param focusListener
         */
        /* package-private */ ListItem(final View view, final View.OnClickListener clickListener,
                                       final View.OnFocusChangeListener focusListener) {
            this(view);
            this.onClickListener = clickListener;
            this.onFocusChangeListener = focusListener;
        }

        /* package-private */ void setValues(final CurrencyModel currencyModel) {
            if (currencyModel != null) {
                setIcon(currencyModel.getCountryFlagIconUri());
                setFieldCode(currencyModel.getCurrencyCode());
                setFieldName(currencyModel.getCurrencyName());
                setFieldRate(currencyModel.getCurrencyRate());
                this.currencyModel = currencyModel;
            }
        }

        /* package-private */ CurrencyModel getValues() {
            return this.currencyModel;
        }

        private void setIcon(final Uri iconUri) {
            if (this.icon != null) {
                this.icon.setImageURI(iconUri);
                this.icon.setOnClickListener(this.onClickListener);
            }
        }

        private void setFieldCode(final String fieldCode) {
            if (this.fieldCode != null) {
                this.fieldCode.setText(fieldCode);
                this.fieldCode.setOnClickListener(this.onClickListener);
            }
        }

        private void setFieldName(final String fieldName) {
            if (this.fieldName != null) {
                this.fieldName.setText(fieldName);
                this.fieldName.setOnClickListener(this.onClickListener);
            }
        }

        private void setFieldRate(final BigDecimal fieldRate) {
            if (this.fieldRate != null) {
                this.fieldRate.setText(new DecimalFormat("#,##0.00").format(fieldRate)); // TODO
                this.fieldRate.setOnEditorActionListener(this.onEditorActionListener);
                this.fieldRate.setOnFocusChangeListener(this.onFocusChangeListener);
            }
        }

        /**
         * Request focus to the field EditorText for the currency rate.
         */
        /* package-private */ void requestFocus() {
            if (this.fieldRate != null) {
                this.fieldRate.requestFocus();
            }
        }
    }
}
