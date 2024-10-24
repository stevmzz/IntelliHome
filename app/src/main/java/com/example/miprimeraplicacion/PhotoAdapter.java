package com.example.miprimeraplicacion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {
    private Context context;
    private List<String> photos;
    private OnPhotoDeleteListener listener;

    public interface OnPhotoDeleteListener {
        void onPhotoDelete(int position);
    }

    public PhotoAdapter(Context context, List<String> photos, OnPhotoDeleteListener listener) {
        this.context = context;
        this.photos = photos;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String base64Photo = photos.get(position);
        try {
            // Convertir base64 a bitmap
            byte[] decodedString = Base64.decode(base64Photo, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            holder.photoImageView.setImageBitmap(bitmap);

            // Configurar botón de eliminar
            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPhotoDelete(holder.getAdapterPosition());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            holder.photoImageView.setImageResource(R.drawable.placeholder_image);
        }
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photoImageView;
        ImageButton deleteButton;

        ViewHolder(View view) {
            super(view);
            photoImageView = view.findViewById(R.id.photoImageView);
            deleteButton = view.findViewById(R.id.deletePhotoButton);
        }
    }
}