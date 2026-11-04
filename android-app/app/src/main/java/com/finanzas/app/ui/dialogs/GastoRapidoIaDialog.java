package com.finanzas.app.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.finanzas.app.R;
import com.finanzas.app.model.Categoria;
import com.finanzas.app.model.Transaccion;
import com.finanzas.app.util.QuickExpenseParser;
import com.finanzas.app.util.UiHelper;
import com.finanzas.app.util.ValidationUtils;

import java.util.List;

/**
 * Dialogo de "Creacion rapida de gasto por texto con IA".
 * Interpreta una frase en lenguaje natural (ej. "Pague 35.50 en Supermaxi hoy con debito")
 * y abre el formulario de transaccion pre-llenado para confirmacion inmediata.
 */
public class GastoRapidoIaDialog {

    public static void show(
            final Context context,
            final List<Categoria> categorias,
            final TransaccionFormDialog.OnSavedListener listener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiHelper.COLOR_SUPERFICIE);
        int pad = UiHelper.dp(context, 16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        root.addView(UiHelper.createSectionTitle(context, "Registro rapido por texto (IA)"));

        TextView helpTv = new TextView(context);
        helpTv.setText("Escriba el movimiento en lenguaje natural indicando monto, concepto, comercio o metodo de pago. El asistente extraera automaticamente los campos para su confirmacion.");
        helpTv.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        helpTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        root.addView(helpTv);

        root.addView(UiHelper.createLabel(context, "Texto del movimiento *"));
        final EditText etTexto = UiHelper.createInput(
                context,
                "Ej. Pague 28.50 en Supermaxi hoy con tarjeta de debito"
        );
        etTexto.setMinLines(3);
        etTexto.setGravity(Gravity.TOP | Gravity.START);
        final TextView errTexto = UiHelper.createFieldErrorText(context);
        root.addView(etTexto);
        root.addView(errTexto);

        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnRowLp.topMargin = UiHelper.dp(context, 16);
        btnRow.setLayoutParams(btnRowLp);

        LinearLayout btnCancelar = UiHelper.createActionButton(
                context,
                "fa-solid fa-xmark",
                "Cancelar",
                R.drawable.bg_button_secondary,
                UiHelper.COLOR_SECUNDARIO
        );
        LinearLayout.LayoutParams cancelLp = (LinearLayout.LayoutParams) btnCancelar.getLayoutParams();
        cancelLp.rightMargin = UiHelper.dp(context, 8);
        btnCancelar.setLayoutParams(cancelLp);

        LinearLayout btnProcesar = UiHelper.createActionButton(
                context,
                "fa-solid fa-wand-magic-sparkles",
                "Analizar y continuar",
                R.drawable.bg_button_primary,
                UiHelper.COLOR_TEXTO_INVERTIDO
        );

        btnRow.addView(btnCancelar);
        btnRow.addView(btnProcesar);
        root.addView(btnRow);

        builder.setView(scroll);
        final AlertDialog dialog = builder.create();

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnProcesar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiHelper.setFieldError(etTexto, errTexto, null);
                String raw = etTexto.getText().toString().trim();
                if (raw.length() < 3) {
                    UiHelper.setFieldError(etTexto, errTexto, "Escriba una descripcion valida del gasto o ingreso.");
                    return;
                }
                if (ValidationUtils.containsEmoji(raw)) {
                    UiHelper.setFieldError(etTexto, errTexto, "No se permiten emojis en el texto.");
                    return;
                }
                QuickExpenseParser.ParsedExpense parsed = QuickExpenseParser.parse(raw, categorias);
                dialog.dismiss();
                TransaccionFormDialog.show(context, null, parsed, categorias, new TransaccionFormDialog.OnSavedListener() {
                    @Override
                    public void onSaved(Transaccion transaccion) {
                        if (listener != null) {
                            listener.onSaved(transaccion);
                        }
                    }
                });
            }
        });

        dialog.show();
    }
}
