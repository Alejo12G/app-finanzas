package com.finanzas.app.util;

import android.content.Context;
import android.content.res.Resources;
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
 * Sincroniza dinamicamente los tokens semanticos de color entre Modo Oscuro (por defecto) y Modo Claro.
 */
public final class UiHelper {

    // Inicializados por defecto con la Paleta Oficial Modo Oscuro Clasico
    public static int COLOR_PRIMARIO = 0xFF2B6CB0;
    public static int COLOR_SECUNDARIO = 0xFF94A3B8;
    public static int COLOR_EXITO = 0xFF38A169;
    public static int COLOR_PELIGRO = 0xFFE53E3E;
    public static int COLOR_ADVERTENCIA = 0xFFD69E2E;
    public static int COLOR_FONDO = 0xFF12161F;
    public static int COLOR_SUPERFICIE = 0xFF1A202C;
    public static int COLOR_TABLA_FILA_ALT = 0xFF222938;
    public static int COLOR_PRIMARIO_SUAVE = 0xFF222938;
    public static int COLOR_BORDE = 0xFF2D3748;
    public static int COLOR_TEXTO = 0xFFE2E8F0;
    public static int COLOR_TEXTO_SECUNDARIO = 0xFF94A3B8;
    public static int COLOR_TEXTO_DESHABILITADO = 0xFF64748B;
    public static int COLOR_TEXTO_INVERTIDO = 0xFFFFFFFF;
    public static int COLOR_EXITO_SUAVE = 0xFF1C3128;
    public static int COLOR_PELIGRO_SUAVE = 0xFF321C22;
    public static int COLOR_ADVERTENCIA_SUAVE = 0xFF332818;

    private static boolean darkModeActive = true;

    private UiHelper() {
    }

    /**
     * Sincroniza los colores activos desde res/values/colors.xml o res/values-night/colors.xml.
     */
    public static void syncThemeColors(Context context) {
        if (context == null) {
            return;
        }
        darkModeActive = ThemeManager.isDarkMode(context);
        COLOR_FONDO = getColor(context, R.color.colorBackgroundPrimary, darkModeActive ? 0xFF12161F : 0xFFF4F6F8);
        COLOR_SUPERFICIE = getColor(context, R.color.colorSurfaceCard, darkModeActive ? 0xFF1A202C : 0xFFFFFFFF);
        COLOR_TABLA_FILA_ALT = getColor(context, R.color.colorTableRowAlt, darkModeActive ? 0xFF222938 : 0xFFF4F6F8);
        COLOR_BORDE = getColor(context, R.color.colorBorderDivider, darkModeActive ? 0xFF2D3748 : 0xFFD9DEE5);
        COLOR_TEXTO = getColor(context, R.color.colorTextPrimary, darkModeActive ? 0xFFE2E8F0 : 0xFF1A202C);
        COLOR_TEXTO_SECUNDARIO = getColor(context, R.color.colorTextSecondary, darkModeActive ? 0xFF94A3B8 : 0xFF4A5568);
        COLOR_SECUNDARIO = COLOR_TEXTO_SECUNDARIO;
        COLOR_PRIMARIO = getColor(context, R.color.colorPrimaryAccent, darkModeActive ? 0xFF2B6CB0 : 0xFF1B2A4A);
        COLOR_EXITO = getColor(context, R.color.colorIncomeGreen, darkModeActive ? 0xFF38A169 : 0xFF2E7D32);
        COLOR_PELIGRO = getColor(context, R.color.colorExpenseRed, darkModeActive ? 0xFFE53E3E : 0xFFC62828);

        COLOR_ADVERTENCIA = getColor(context, R.color.color_advertencia, darkModeActive ? 0xFFD69E2E : 0xFFB26A00);
        COLOR_PRIMARIO_SUAVE = getColor(context, R.color.color_primario_suave, darkModeActive ? 0xFF222938 : 0xFFE8ECF3);
        COLOR_TEXTO_DESHABILITADO = getColor(context, R.color.color_texto_deshabilitado, darkModeActive ? 0xFF64748B : 0xFFA0AEC0);
        COLOR_TEXTO_INVERTIDO = getColor(context, R.color.color_texto_invertido, 0xFFFFFFFF);
        COLOR_EXITO_SUAVE = getColor(context, R.color.color_exito_suave, darkModeActive ? 0xFF1C3128 : 0xFFE8F5E9);
        COLOR_PELIGRO_SUAVE = getColor(context, R.color.color_peligro_suave, darkModeActive ? 0xFF321C22 : 0xFFFDECEA);
        COLOR_ADVERTENCIA_SUAVE = getColor(context, R.color.color_advertencia_suave, darkModeActive ? 0xFF332818 : 0xFFFFF4E5);
    }

    public static int getColor(Context context, int colorResId, int fallback) {
        if (context == null) {
            return fallback;
        }
        try {
            Resources res = context.getResources();
            return res.getColor(colorResId, context.getTheme());
        } catch (Exception e) {
            return fallback;
        }
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
        String clean = hex.trim();
        if (darkModeActive) {
            if ("#1B2A4A".equalsIgnoreCase(clean)) return COLOR_PRIMARIO;
            if ("#4A5568".equalsIgnoreCase(clean)) return COLOR_SECUNDARIO;
            if ("#2E7D32".equalsIgnoreCase(clean)) return COLOR_EXITO;
            if ("#C62828".equalsIgnoreCase(clean)) return COLOR_PELIGRO;
            if ("#B26A00".equalsIgnoreCase(clean)) return COLOR_ADVERTENCIA;
        }
        try {
            return Color.parseColor(clean);
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
