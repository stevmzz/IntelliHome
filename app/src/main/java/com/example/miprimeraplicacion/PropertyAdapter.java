package com.example.miprimeraplicacion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.ViewHolder> {
    private List<Property> properties;
    private Context context;
    private OnPropertyClickListener listener;
    private final NumberFormat priceFormatter;

    public interface OnPropertyClickListener {
        void onPropertyClick(Property property);
    }

    public PropertyAdapter(Context context, OnPropertyClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.properties = new ArrayList<>();
        this.priceFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "CR"));
        this.priceFormatter.setMaximumFractionDigits(0);
    }

    public void updateProperties(List<Property> newProperties) {
        this.properties = newProperties;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_property_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Property property = properties.get(position);

        // Título
        holder.titleTextView.setText(property.getTitle());

        // Precio
        String formattedPrice = priceFormatter.format(property.getPricePerNight());
        holder.priceTextView.setText(formattedPrice + " por noche");

        // Ubicación
        if (property.getLocation() != null && !property.getLocation().isEmpty()) {
            holder.locationTextView.setText(property.getLocation());
            holder.locationTextView.setVisibility(View.VISIBLE);
        } else {
            holder.locationTextView.setVisibility(View.GONE);
        }

        // Descripción
        if (property.getDescription() != null && !property.getDescription().isEmpty()) {
            holder.descriptionTextView.setText(property.getDescription());
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }

        // Manejo de fotos
        List<String> photoUrls = property.getPhotoUrls();
        if (photoUrls != null && !photoUrls.isEmpty()) {
            // Mostrar la primera foto
            String firstPhotoBase64 = photoUrls.get(0);
            try {
                byte[] decodedString = Base64.decode(firstPhotoBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.propertyImageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.propertyImageView.setImageResource(R.drawable.placeholder_home);
            }

            // Mostrar contador de fotos adicionales si hay más de una
            if (photoUrls.size() > 1) {
                holder.photoCountText.setVisibility(View.VISIBLE);
                holder.photoCountText.setText("+" + (photoUrls.size() - 1) + " fotos");
            } else {
                holder.photoCountText.setVisibility(View.GONE);
            }
        } else {
            holder.propertyImageView.setImageResource(R.drawable.placeholder_home);
            holder.photoCountText.setVisibility(View.GONE);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPropertyClick(property);
            }
        });
    }

    @Override
    public int getItemCount() {
        return properties.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImageView;
        TextView photoCountText;
        TextView titleTextView;
        TextView priceTextView;
        TextView locationTextView;
        TextView descriptionTextView;

        ViewHolder(View view) {
            super(view);
            propertyImageView = view.findViewById(R.id.propertyImageView);
            photoCountText = view.findViewById(R.id.photoCountText);
            titleTextView = view.findViewById(R.id.titleTextView);
            priceTextView = view.findViewById(R.id.priceTextView);
            locationTextView = view.findViewById(R.id.locationTextView);
            descriptionTextView = view.findViewById(R.id.descriptionTextView);
        }
    }
}