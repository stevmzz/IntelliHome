package com.example.miprimeraplicacion;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.fragment.app.DialogFragment;
import com.airbnb.lottie.LottieAnimationView;

public class SuccessAnimationDialog extends DialogFragment {
    private OnDialogDismissedListener dismissListener;

    public interface OnDialogDismissedListener {
        void onDialogDismissed();
    }

    public void setDismissListener(OnDialogDismissedListener listener) {
        this.dismissListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext(), R.style.FullScreenDialogStyle);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_success_animation);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        LottieAnimationView animationView = dialog.findViewById(R.id.animation_view);
        animationView.setAnimation(R.raw.check_animation);  // Usando el nuevo nombre del archivo
        animationView.playAnimation();

        new Handler().postDelayed(() -> {
            if (getActivity() != null && !getActivity().isFinishing()) {
                dialog.dismiss();
                if (dismissListener != null) {
                    dismissListener.onDialogDismissed();
                }
            }
        }, 2000);

        return dialog;
    }
}