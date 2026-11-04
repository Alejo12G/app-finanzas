package com.finanzas.app.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.finanzas.app.R;

/**
 * Constructor de componentes visuales sobrios y bancarios segun UI_DESIGN.md.
 */
public final class UiHelper {

    public static final int COLOR_PRIMARIO = 0xFF1B2A4A;
    public static final int COLOR_SECUNDARIO = 0xFF4A5568;
    public static final int COLOR_EXITO = 0xFF2E7D32;
    public static final int COLOR_PELIGRO = 0xFFC62828;
    public static final int COLOR_ADVERTENCIA = 0xFFB26A00;
    public static final int COLOR_FONDO = 0xFFF4F6F8;
    public static final int COLOR_SUPERFICIE = 0xFFFFFFFF;
    public static final int COLOR_PRIMARIO_SUAVE = 0xFFE8ECF3;
    public static final int COLOR_BORDE = 0xFFD9DEE5;
    public static final int COLOR_TEXTO = 0xFF1A202C;
    public static final int COLOR_TEXTO_SECUNDARIO = 0xFF4A5568;
    public static final int COLOR_TEXTO_DESHABILITADO = 0xFFA0AEC0;
    public static final int COLOR_TEXTO_INVERTIDO = 0xFFFFFFFF;
    public static final int COLOR_EXITO_SUAVE = 0xFFE8F5E9;
    public static final int COLOR_PELIGRO_SUAVE = 0xFFFDECEA;
    public static final int COLOR_ADVERTENCIA_SUAVE = 0xFFFFF4E5;

    private UiHelper() {
    }

    public static int dp(Context context, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    public static int parseColorSafe(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Color.parseColor(hex.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    public static GradientDrawable createRoundedBox(Context context, int fillColor, int strokeColor, int strokeWidthDp, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(context, radiusDp));
        if (strokeWidthDp > 0) {
            gd.setStroke(dp(context, strokeWidthDp), strokeColor);
        }
        return gd;
    }

    public static LinearLayout createCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(context, 16);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dp(context, 16);
        card.setLayoutParams(lp);
        return card;
    }

    public static TextView createSectionTitle(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(COLOR_PRIMARIO);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = dp(context, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    public static TextView createLabel(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(COLOR_TEXTO_SECUNDARIO);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(context, 8);
        lp.bottomMargin = dp(context, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    public static EditText createInput(Context context, String hint) {
        EditText et = new EditText(context);
        et.setHint(hint);
        et.setHintTextColor(COLOR_TEXTO_DESHABILITADO);
        et.setTextColor(COLOR_TEXTO);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        et.setBackgroundResource(R.drawable.bg_input);
        int padH = dp(context, 12);
        int padV = dp(context, 10);
        et.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        et.setLayoutParams(lp);
        return et;
    }

    public static TextView createFieldErrorText(Context context) {
        TextView tv = new TextView(context);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTextColor(COLOR_PELIGRO);
        tv.setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.topMargin = dp(context, 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    public static void setFieldError(EditText input, TextView errorTv, String message) {
        if (message == null || message.trim().isEmpty()) {
            if (input != null) {
                input.setBackgroundResource(R.drawable.bg_input);
            }
            if (errorTv != null) {
                errorTv.setText("");
                errorTv.setVisibility(View.GONE);
            }
        } else {
            if (input != null) {
                input.setBackgroundResource(R.drawable.bg_input_error);
            }
            if (errorTv != null) {
                errorTv.setText(message);
                errorTv.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Crea un boton de 48dp de alto con icono FontAwesome a la izquierda + texto (UI_DESIGN.md Seccion 4).
     */
    public static LinearLayout createActionButton(
            Context context,
            String iconClass,
            String label,
            int bgDrawableRes,
            int textColor) {
        LinearLayout btn = new LinearLayout(context);
        btn.setOrientation(LinearLayout.HORIZONTAL);
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundResource(bgDrawableRes);
        btn.setClickable(true);
        btn.setFocusable(true);
        int padH = dp(context, 14);
        btn.setPadding(padH, 0, padH, 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(context, 48)
        );
        btn.setLayoutParams(lp);

        if (iconClass != null && !iconClass.isEmpty()) {
            TextView iconTv = new TextView(context);
            FaIcon.apply(iconTv, iconClass);
            iconTv.setTextColor(textColor);
            iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            iconTv.setContentDescription(null);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            if (label != null && !label.isEmpty()) {
                iconLp.rightMargin = dp(context, 8);
            }
            iconTv.setLayoutParams(iconLp);
            btn.addView(iconTv);
        }

        if (label != null && !label.isEmpty()) {
            TextView textTv = new TextView(context);
            textTv.setText(label);
            textTv.setTextColor(textColor);
            textTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            textTv.setTypeface(Typeface.DEFAULT_BOLD);
            btn.addView(textTv);
        }

        return btn;
    }

    public static void applyTabularNums(TextView tv) {
        if (tv != null) {
            tv.setFontFeatureSettings("tnum");
        }
    }
}
