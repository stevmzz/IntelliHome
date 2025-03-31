package com.example.miprimeraplicacion;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.List;

public class LocationAutocompleteHelper {
    private final Context context;
    private final AutoCompleteTextView autoCompleteTextView;
    private final LocationSelectionListener listener;
    private final LocationAdapter adapter;

    public interface LocationSelectionListener {
        void onLocationSelected(String location);
    }

    public LocationAutocompleteHelper(Context context, AutoCompleteTextView autoCompleteTextView,
                                      LocationSelectionListener listener) {
        this.context = context;
        this.autoCompleteTextView = autoCompleteTextView;
        this.listener = listener;
        this.adapter = new LocationAdapter(context);

        setupAutoComplete();
    }

    private void setupAutoComplete() {
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setThreshold(1);

        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLocation = (String) parent.getItemAtPosition(position);
            autoCompleteTextView.setText(selectedLocation);
            hideKeyboard();
            if (listener != null) {
                listener.onLocationSelected(selectedLocation);
            }
        });

        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                if (!text.isEmpty() && !adapter.isValidLocation(text)) {
                    autoCompleteTextView.setError("Seleccione una ubicación válida");
                } else {
                    autoCompleteTextView.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        autoCompleteTextView.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String text = autoCompleteTextView.getText().toString();
                if (!text.isEmpty() && !adapter.isValidLocation(text)) {
                    autoCompleteTextView.setText("");
                    if (listener != null) {
                        listener.onLocationSelected("");
                    }
                }
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(autoCompleteTextView.getWindowToken(), 0);
        }
    }

    public void clearLocation() {
        autoCompleteTextView.setText("");
    }

    public String getCurrentLocation() {
        return autoCompleteTextView.getText().toString();
    }

    private static class LocationAdapter extends ArrayAdapter<String> implements Filterable {
        private final List<String> allLocations;
        private List<String> filteredLocations;

        public LocationAdapter(Context context) {
            super(context, android.R.layout.simple_dropdown_item_1line);
            this.allLocations = CostaRicaLocations.getAllLocations();
            this.filteredLocations = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return filteredLocations.size();
        }

        @Override
        public String getItem(int position) {
            return filteredLocations.get(position);
        }

        public boolean isValidLocation(String location) {
            return allLocations.contains(location);
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    filteredLocations = constraint == null || constraint.length() == 0 ?
                            new ArrayList<>() :
                            CostaRicaLocations.searchLocations(constraint.toString());
                    results.values = filteredLocations;
                    results.count = filteredLocations.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    clear();
                    if (results.count > 0) {
                        addAll(filteredLocations);
                    }
                    notifyDataSetChanged();
                }
            };
        }
    }
}