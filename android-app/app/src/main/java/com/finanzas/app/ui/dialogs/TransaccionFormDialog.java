package com.finanzas.app.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.Categoria;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.model.Transaccion;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.DateUtils;
import com.finanzas.app.util.QuickExpenseParser;
import com.finanzas.app.util.UiHelper;
import com.finanzas.app.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dialogo de alta y edicion de transacciones (POST /transacciones y PUT /transacciones/{id}).
 */
public class TransaccionFormDialog {

    public interface OnSavedListener {
        void onSaved(Transaccion transaccion);
    }

    private static final String[] TIPOS = {"GASTO", "INGRESO"};
    private static final String[] NATURALEZAS = {"VARIABLE", "FIJO"};
    private static final String[] METODOS_PAGO = {"EFECTIVO", "DEBITO", "CREDITO", "TRANSFERENCIA", "OTRO"};

    public static void show(
            final Context context,
            final Transaccion existente,
            final QuickExpenseParser.ParsedExpense prefill,
            final List<Categoria> todasCategorias,
            final OnSavedListener listener) {

        final boolean esEdicion = (existente != null && existente.id > 0);
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiHelper.COLOR_SUPERFICIE);
        int pad = UiHelper.dp(context, 16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView titleTv = UiHelper.createSectionTitle(
                context,
                esEdicion ? "Editar transaccion" : (prefill != null ? "Confirmar transaccion rapida IA" : "Nueva transaccion")
        );
        root.addView(titleTv);

        final TextView globalErrorTv = UiHelper.createFieldErrorText(context);
        root.addView(globalErrorTv);

        // Tipo (GASTO / INGRESO)
        root.addView(UiHelper.createLabel(context, "Tipo de movimiento *"));
        final Spinner spTipo = new Spinner(context);
        spTipo.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, TIPOS));
        root.addView(spTipo);

        // Naturaleza (VARIABLE / FIJO)
        root.addView(UiHelper.createLabel(context, "Naturaleza *"));
        final Spinner spNaturaleza = new Spinner(context);
        spNaturaleza.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, NATURALEZAS));
        root.addView(spNaturaleza);

        // Monto
        root.addView(UiHelper.createLabel(context, "Monto (" + CurrencyUtils.getActiveCurrency() + ") *"));
        final EditText etMonto = UiHelper.createInput(context, "0.00");
        etMonto.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final TextView errMonto = UiHelper.createFieldErrorText(context);
        root.addView(etMonto);
        root.addView(errMonto);

        // Fecha
        root.addView(UiHelper.createLabel(context, "Fecha (YYYY-MM-DD) *"));
        LinearLayout fechaRow = new LinearLayout(context);
        fechaRow.setOrientation(LinearLayout.HORIZONTAL);
        fechaRow.setGravity(Gravity.CENTER_VERTICAL);
        final EditText etFecha = UiHelper.createInput(context, DateUtils.getTodayIso());
        LinearLayout.LayoutParams fechaLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        fechaLp.rightMargin = UiHelper.dp(context, 8);
        etFecha.setLayoutParams(fechaLp);
        fechaRow.addView(etFecha);

        LinearLayout btnPickDate = UiHelper.createActionButton(
                context,
                "fa-solid fa-calendar-days",
                "Calendario",
                R.drawable.bg_button_secondary,
                UiHelper.COLOR_SECUNDARIO
        );
        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateUtils.showDatePicker(context, etFecha);
            }
        });
        fechaRow.addView(btnPickDate);
        final TextView errFecha = UiHelper.createFieldErrorText(context);
        root.addView(fechaRow);
        root.addView(errFecha);

        // Descripcion
        root.addView(UiHelper.createLabel(context, "Descripcion *"));
        final EditText etDescripcion = UiHelper.createInput(context, "Ej. Pago de servicio o compra");
        final TextView errDescripcion = UiHelper.createFieldErrorText(context);
        root.addView(etDescripcion);
        root.addView(errDescripcion);

        // Categoria
        root.addView(UiHelper.createLabel(context, "Categoria *"));
        final Spinner spCategoria = new Spinner(context);
        final TextView errCategoria = UiHelper.createFieldErrorText(context);
        root.addView(spCategoria);
        root.addView(errCategoria);

        // Metodo de pago
        root.addView(UiHelper.createLabel(context, "Metodo de pago *"));
        final Spinner spMetodo = new Spinner(context);
        spMetodo.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, METODOS_PAGO));
        root.addView(spMetodo);

        // Comercio
        root.addView(UiHelper.createLabel(context, "Comercio / Entidad (opcional)"));
        final EditText etComercio = UiHelper.createInput(context, "Ej. Supermaxi, Empresa Electrica");
        final TextView errComercio = UiHelper.createFieldErrorText(context);
        root.addView(etComercio);
        root.addView(errComercio);

        // Notas
        root.addView(UiHelper.createLabel(context, "Notas adicionales (opcional)"));
        final EditText etNotas = UiHelper.createInput(context, "Observaciones");
        final TextView errNotas = UiHelper.createFieldErrorText(context);
        root.addView(etNotas);
        root.addView(errNotas);

        final List<Categoria> categoriasFiltradas = new ArrayList<>();
        final Runnable actualizarSpinnerCategorias = new Runnable() {
            @Override
            public void run() {
                String tipoSel = TIPOS[spTipo.getSelectedItemPosition()];
                categoriasFiltradas.clear();
                if (todasCategorias != null) {
                    for (Categoria c : todasCategorias) {
                        if (tipoSel.equals(c.tipo) && (c.activa || (esEdicion && existente.categoriaId == c.id))) {
                            categoriasFiltradas.add(c);
                        }
                    }
                }
                ArrayAdapter<Categoria> catAdapter = new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_spinner_dropdown_item,
                        categoriasFiltradas
                );
                spCategoria.setAdapter(catAdapter);

                int targetCatId = 0;
                if (esEdicion) {
                    targetCatId = existente.categoriaId;
                } else if (prefill != null && prefill.categoriaId > 0) {
                    targetCatId = prefill.categoriaId;
                }
                if (targetCatId > 0) {
                    for (int i = 0; i < categoriasFiltradas.size(); i++) {
                        if (categoriasFiltradas.get(i).id == targetCatId) {
                            spCategoria.setSelection(i);
                            break;
                        }
                    }
                }
            }
        };

        spTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                actualizarSpinnerCategorias.run();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Pre-llenar valores si es edicion o si viene de creacion rapida IA
        if (esEdicion) {
            spTipo.setSelection("INGRESO".equals(existente.tipo) ? 1 : 0);
            spNaturaleza.setSelection("FIJO".equals(existente.naturaleza) ? 1 : 0);
            etMonto.setText(CurrencyUtils.centavosToInputString(existente.montoCentavos));
            etFecha.setText(existente.fecha);
            etDescripcion.setText(existente.descripcion);
            for (int i = 0; i < METODOS_PAGO.length; i++) {
                if (METODOS_PAGO[i].equals(existente.metodoPago)) {
                    spMetodo.setSelection(i);
                    break;
                }
            }
            etComercio.setText(existente.comercio != null ? existente.comercio : "");
            etNotas.setText(existente.notas != null ? existente.notas : "");
        } else if (prefill != null) {
            spTipo.setSelection("INGRESO".equals(prefill.tipo) ? 1 : 0);
            spNaturaleza.setSelection("FIJO".equals(prefill.naturaleza) ? 1 : 0);
            if (prefill.montoCentavos > 0) {
                etMonto.setText(CurrencyUtils.centavosToInputString(prefill.montoCentavos));
            }
            etFecha.setText(prefill.fecha != null ? prefill.fecha : DateUtils.getTodayIso());
            etDescripcion.setText(prefill.descripcion != null ? prefill.descripcion : "");
            for (int i = 0; i < METODOS_PAGO.length; i++) {
                if (METODOS_PAGO[i].equals(prefill.metodoPago)) {
                    spMetodo.setSelection(i);
                    break;
                }
            }
            etComercio.setText(prefill.comercio != null ? prefill.comercio : "");
            etNotas.setText(prefill.notas != null ? prefill.notas : "");
        } else {
            etFecha.setText(DateUtils.getTodayIso());
        }
        actualizarSpinnerCategorias.run();

        // Botonera inferior
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

        final LinearLayout btnGuardar = UiHelper.createActionButton(
                context,
                "fa-solid fa-floppy-disk",
                "Guardar cambios",
                R.drawable.bg_button_primary,
                UiHelper.COLOR_TEXTO_INVERTIDO
        );

        btnRow.addView(btnCancelar);
        btnRow.addView(btnGuardar);
        root.addView(btnRow);

        builder.setView(scroll);
        final AlertDialog dialog = builder.create();

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Limpiar errores previos
                UiHelper.setFieldError(null, globalErrorTv, null);
                UiHelper.setFieldError(etMonto, errMonto, null);
                UiHelper.setFieldError(etFecha, errFecha, null);
                UiHelper.setFieldError(etDescripcion, errDescripcion, null);
                UiHelper.setFieldError(null, errCategoria, null);
                UiHelper.setFieldError(etComercio, errComercio, null);
                UiHelper.setFieldError(etNotas, errNotas, null);

                String tipo = TIPOS[spTipo.getSelectedItemPosition()];
                String naturaleza = NATURALEZAS[spNaturaleza.getSelectedItemPosition()];
                String metodo = METODOS_PAGO[spMetodo.getSelectedItemPosition()];
                long montoCentavos = CurrencyUtils.parseMontoToCentavos(etMonto.getText().toString());
                String fecha = etFecha.getText().toString().trim();
                String desc = etDescripcion.getText().toString().trim();
                String comercio = etComercio.getText().toString().trim();
                String notas = etNotas.getText().toString().trim();

                boolean valid = true;
                if (!ValidationUtils.isValidMontoCentavos(montoCentavos)) {
                    UiHelper.setFieldError(etMonto, errMonto, "Ingrese un monto mayor a 0.00.");
                    valid = false;
                }
                if (!ValidationUtils.isValidIsoDate(fecha)) {
                    UiHelper.setFieldError(etFecha, errFecha, "Formato requerido: YYYY-MM-DD.");
                    valid = false;
                }
                if (desc.isEmpty() || desc.length() > 200) {
                    UiHelper.setFieldError(etDescripcion, errDescripcion, "La descripcion debe tener entre 1 y 200 caracteres.");
                    valid = false;
                } else if (ValidationUtils.containsEmoji(desc)) {
                    UiHelper.setFieldError(etDescripcion, errDescripcion, "No se permiten emojis en el texto.");
                    valid = false;
                }
                if (comercio.length() > 120) {
                    UiHelper.setFieldError(etComercio, errComercio, "Maximo 120 caracteres.");
                    valid = false;
                } else if (ValidationUtils.containsEmoji(comercio)) {
                    UiHelper.setFieldError(etComercio, errComercio, "No se permiten emojis en el texto.");
                    valid = false;
                }
                if (notas.length() > 1000) {
                    UiHelper.setFieldError(etNotas, errNotas, "Maximo 1000 caracteres.");
                    valid = false;
                } else if (ValidationUtils.containsEmoji(notas)) {
                    UiHelper.setFieldError(etNotas, errNotas, "No se permiten emojis en el texto.");
                    valid = false;
                }

                if (categoriasFiltradas.isEmpty() || spCategoria.getSelectedItemPosition() < 0) {
                    UiHelper.setFieldError(null, errCategoria, "Seleccione una categoria activa.");
                    valid = false;
                }

                if (!valid) {
                    return;
                }

                int categoriaId = categoriasFiltradas.get(spCategoria.getSelectedItemPosition()).id;
                btnGuardar.setEnabled(false);

                ApiCallback<Transaccion> cb = new ApiCallback<Transaccion>() {
                    @Override
                    public void onSuccess(Transaccion data, PaginacionMeta meta) {
                        btnGuardar.setEnabled(true);
                        dialog.dismiss();
                        if (listener != null) {
                            listener.onSaved(data);
                        }
                    }

                    @Override
                    public void onError(ApiError error) {
                        btnGuardar.setEnabled(true);
                        UiHelper.setFieldError(null, globalErrorTv, error.mensaje);
                        Map<String, String> fieldMap = error.getMapaErroresPorCampo();
                        if (fieldMap.containsKey("monto_centavos")) {
                            UiHelper.setFieldError(etMonto, errMonto, fieldMap.get("monto_centavos"));
                        }
                        if (fieldMap.containsKey("fecha")) {
                            UiHelper.setFieldError(etFecha, errFecha, fieldMap.get("fecha"));
                        }
                        if (fieldMap.containsKey("descripcion")) {
                            UiHelper.setFieldError(etDescripcion, errDescripcion, fieldMap.get("descripcion"));
                        }
                        if (fieldMap.containsKey("categoria_id")) {
                            UiHelper.setFieldError(null, errCategoria, fieldMap.get("categoria_id"));
                        }
                        if (fieldMap.containsKey("comercio")) {
                            UiHelper.setFieldError(etComercio, errComercio, fieldMap.get("comercio"));
                        }
                        if (fieldMap.containsKey("notas")) {
                            UiHelper.setFieldError(etNotas, errNotas, fieldMap.get("notas"));
                        }
                    }
                };

                if (esEdicion) {
                    ApiClient.getInstance().actualizarTransaccion(
                            existente.id, tipo, naturaleza, montoCentavos, fecha, desc, categoriaId, metodo, comercio, notas, cb
                    );
                } else {
                    ApiClient.getInstance().crearTransaccion(
                            tipo, naturaleza, montoCentavos, fecha, desc, categoriaId, metodo, comercio, notas, cb
                    );
                }
            }
        });

        dialog.show();
    }
}
