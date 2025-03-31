package com.example.miprimeraplicacion;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {
    private static final String TAG = "PhotoAdapter";
    private Context context;
    private List<String> selectedPhotosBase64;
    private OnPhotoDeleteListener listener;

    public interface OnPhotoDeleteListener {
        void onPhotoDelete(int position);
    }

    public PhotoAdapter(Context context, List<String> selectedPhotosBase64, OnPhotoDeleteListener listener) {
        this.context = context;
        this.selectedPhotosBase64 = selectedPhotosBase64;
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
        String base64Photo = selectedPhotosBase64.get(position);
        try {
            if (base64Photo != null && !base64Photo.isEmpty()) {
                // Limpiar la cadena base64
                base64Photo = base64Photo.replace("\n", "")
                        .replace("\r", "")
                        .replace(" ", "")
                        .replace("\t", "");

                byte[] decodedString = Base64.decode(base64Photo, Base64.NO_WRAP);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.photoImageView.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading photo: " + e.getMessage());
            holder.photoImageView.setImageResource(R.drawable.placeholder_image);
        }

        // Configurar botón de eliminar
        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhotoDelete(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return selectedPhotosBase64.size();
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