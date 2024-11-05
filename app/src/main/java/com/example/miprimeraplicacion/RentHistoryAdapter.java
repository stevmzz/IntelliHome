package com.example.miprimeraplicacion;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class RentHistoryAdapter extends RecyclerView.Adapter<RentHistoryAdapter.ViewHolder> {
    private static final String TAG = "RentHistoryAdapter";
    private List<RentedProperty> properties;
    private final Context context;
    private final NumberFormat priceFormatter;

    public RentHistoryAdapter(Context context) {
        this.context = context;
        this.properties = new ArrayList<>();
        this.priceFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "CR"));
        this.priceFormatter.setMaximumFractionDigits(0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rented_property, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RentedProperty property = properties.get(position);

        // Establecer título
        holder.titleTextView.setText(property.getTitle());

        // Establecer descripción
        holder.descriptionTextView.setText(property.getDescription());

        // Establecer precio formateado
        String formattedPrice = priceFormatter.format(property.getPrice()) + " por noche";
        holder.priceTextView.setText(formattedPrice);
    }

    @Override
    public int getItemCount() {
        return properties.size();
    }

    public void updateProperties(List<RentedProperty> newProperties) {
        this.properties = new ArrayList<>(newProperties);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        TextView priceTextView;

        ViewHolder(View view) {
            super(view);
            titleTextView = view.findViewById(R.id.titleTextView);
            descriptionTextView = view.findViewById(R.id.descriptionTextView);
            priceTextView = view.findViewById(R.id.priceTextView);
        }
    }
}