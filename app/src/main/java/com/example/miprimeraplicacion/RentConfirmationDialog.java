package com.example.miprimeraplicacion;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * Diálogo de confirmación para alquilar una propiedad
 */
public class RentConfirmationDialog extends DialogFragment {
    private RentConfirmationListener listener;

    /**
     * Interface para comunicar la confirmación al activity
     */
    public interface RentConfirmationListener {
        void onRentConfirmed();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(), R.style.AlertDialogTheme);

        // Inflar el layout personalizado
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_rent_confirmation, null);

        // Configurar el diálogo
        builder.setView(view)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    if (listener != null) {
                        listener.onRentConfirmed();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    dialog.dismiss();
                });

        // Crear el diálogo
        AlertDialog dialog = builder.create();

        // Personalizar la apariencia
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        return dialog;
    }

    /**
     * Establece el listener para los eventos del diálogo
     * @param listener RentConfirmationListener para manejar la confirmación
     */
    public void setListener(RentConfirmationListener listener) {
        this.listener = listener;
    }

    /**
     * Muestra el diálogo con un estilo personalizado
     */
    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            // Personalizar los botones
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                    requireContext().getResources().getColor(R.color.colorPrimary));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    requireContext().getResources().getColor(R.color.colorPrimary));
        }
    }
}