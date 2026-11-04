package com.finanzas.app.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import com.finanzas.app.model.ArticuloCompra;
import com.finanzas.app.model.Categoria;
import com.finanzas.app.model.Compra;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.DateUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;
import com.finanzas.app.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dialogo de creacion y edicion de compras con desglose de articulos (POST /compras y PUT /compras/{id}).
 * Calcula en vivo cada subtotal_centavos = redondeo_mitad_hacia_arriba(cantidad * precio_unitario_centavos)
 * y el monto_centavos total (API_CONTRACT.md Seccion 4.3 y 7.4).
 */
public class CompraFormDialog {

    public interface OnCompraSavedListener {
        void onSaved(Compra compra);
    }

    private static final String[] NATURALEZAS = {"VARIABLE", "FIJO"};
    private static final String[] METODOS_PAGO = {"DEBITO", "EFECTIVO", "CREDITO", "TRANSFERENCIA", "OTRO"};
    private static final String[] UNIDADES = {"UNIDAD", "KG", "G", "L", "ML", "PAQUETE", "OTRO"};

    private static class ArticuloRowHolder {
        LinearLayout container;
        EditText etNombre;
        EditText etCantidad;
        Spinner spUnidad;
        EditText etPrecioUnitario;
        TextView tvSubtotal;
        TextView tvRowError;
        long subtotalCentavos;
    }

    public static void show(
            final Context context,
            final Compra existente,
            final List<Categoria> todasCategorias,
            final OnCompraSavedListener listener) {

        final boolean esEdicion = (existente != null && existente.id > 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiHelper.COLOR_SUPERFICIE);
        int pad = UiHelper.dp(context, 16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        root.addView(UiHelper.createSectionTitle(context, esEdicion ? "Editar compra con desglose" : "Nueva compra con desglose"));

        final TextView globalErrorTv = UiHelper.createFieldErrorText(context);
        root.addView(globalErrorTv);

        // Comercio *
        root.addView(UiHelper.createLabel(context, "Comercio / Establecimiento *"));
        final EditText etComercio = UiHelper.createInput(context, "Ej. Supermaxi, Mi Comisariato");
        final TextView errComercio = UiHelper.createFieldErrorText(context);
        root.addView(etComercio);
        root.addView(errComercio);

        // Fecha *
        root.addView(UiHelper.createLabel(context, "Fecha (YYYY-MM-DD) *"));
        LinearLayout fechaRow = new LinearLayout(context);
        fechaRow.setOrientation(LinearLayout.HORIZONTAL);
        fechaRow.setGravity(Gravity.CENTER_VERTICAL);
        final EditText etFecha = UiHelper.createInput(context, DateUtils.getTodayIso());
        LinearLayout.LayoutParams fechaLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        fechaLp.rightMargin = UiHelper.dp(context, 8);
        etFecha.setLayoutParams(fechaLp);
        fechaRow.addView(etFecha);

        LinearLayout btnCal = UiHelper.createActionButton(
                context,
                "fa-solid fa-calendar-days",
                "Calendario",
                R.drawable.bg_button_secondary,
                UiHelper.COLOR_SECUNDARIO
        );
        btnCal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateUtils.showDatePicker(context, etFecha);
            }
        });
        fechaRow.addView(btnCal);
        final TextView errFecha = UiHelper.createFieldErrorText(context);
        root.addView(fechaRow);
        root.addView(errFecha);

        // Categoria (solo tipo GASTO)
        root.addView(UiHelper.createLabel(context, "Categoria de gasto *"));
        final Spinner spCategoria = new Spinner(context);
        final List<Categoria> categoriasGasto = new ArrayList<>();
        if (todasCategorias != null) {
            for (Categoria c : todasCategorias) {
                if ("GASTO".equals(c.tipo) && (c.activa || (esEdicion && existente.categoriaId == c.id))) {
                    categoriasGasto.add(c);
                }
            }
        }
        spCategoria.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, categoriasGasto));
        final TextView errCategoria = UiHelper.createFieldErrorText(context);
        root.addView(spCategoria);
        root.addView(errCategoria);

        // Metodo de pago y Naturaleza
        root.addView(UiHelper.createLabel(context, "Metodo de pago *"));
        final Spinner spMetodo = new Spinner(context);
        spMetodo.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, METODOS_PAGO));
        root.addView(spMetodo);

        root.addView(UiHelper.createLabel(context, "Naturaleza"));
        final Spinner spNaturaleza = new Spinner(context);
        spNaturaleza.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, NATURALEZAS));
        root.addView(spNaturaleza);

        // Descripcion opcional
        root.addView(UiHelper.createLabel(context, "Descripcion (opcional)"));
        final EditText etDescripcion = UiHelper.createInput(context, "Opcional (por defecto: Compra en <comercio>)");
        final TextView errDescripcion = UiHelper.createFieldErrorText(context);
        root.addView(etDescripcion);
        root.addView(errDescripcion);

        // Notas opcional
        root.addView(UiHelper.createLabel(context, "Notas (opcional)"));
        final EditText etNotas = UiHelper.createInput(context, "Observaciones del ticket");
        final TextView errNotas = UiHelper.createFieldErrorText(context);
        root.addView(etNotas);
        root.addView(errNotas);

        // Encabezado de articulos
        LinearLayout artHeader = new LinearLayout(context);
        artHeader.setOrientation(LinearLayout.HORIZONTAL);
        artHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams artHeaderLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        artHeaderLp.topMargin = UiHelper.dp(context, 16);
        artHeaderLp.bottomMargin = UiHelper.dp(context, 8);
        artHeader.setLayoutParams(artHeaderLp);

        TextView tvArtTitle = new TextView(context);
        tvArtTitle.setText("Desglose de articulos *");
        tvArtTitle.setTextColor(UiHelper.COLOR_PRIMARIO);
        tvArtTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvArtTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvArtTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        artHeader.addView(tvArtTitle);

        LinearLayout btnAddItem = UiHelper.createActionButton(
                context,
                "fa-solid fa-square-plus",
                "Agregar articulo",
                R.drawable.bg_button_secondary,
                UiHelper.COLOR_PRIMARIO
        );
        artHeader.addView(btnAddItem);
        root.addView(artHeader);

        final TextView errArticulos = UiHelper.createFieldErrorText(context);
        root.addView(errArticulos);

        final LinearLayout itemsContainer = new LinearLayout(context);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(itemsContainer);

        // Barra de vista previa del total en vivo
        LinearLayout totalBox = new LinearLayout(context);
        totalBox.setOrientation(LinearLayout.HORIZONTAL);
        totalBox.setGravity(Gravity.CENTER_VERTICAL);
        totalBox.setBackgroundResource(R.drawable.bg_badge_primary);
        int pad12 = UiHelper.dp(context, 12);
        totalBox.setPadding(pad12, pad12, pad12, pad12);
        LinearLayout.LayoutParams totalLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        totalLp.topMargin = UiHelper.dp(context, 12);
        totalBox.setLayoutParams(totalLp);

        TextView tvTotalLabel = new TextView(context);
        tvTotalLabel.setText("Total calculado del ticket:");
        tvTotalLabel.setTextColor(UiHelper.COLOR_PRIMARIO);
        tvTotalLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvTotalLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvTotalLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView tvTotalValue = new TextView(context);
        tvTotalValue.setText(CurrencyUtils.formatConMoneda(0L));
        tvTotalValue.setTextColor(UiHelper.COLOR_PRIMARIO);
        tvTotalValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvTotalValue.setTypeface(Typeface.DEFAULT_BOLD);
        UiHelper.applyTabularNums(tvTotalValue);

        totalBox.addView(tvTotalLabel);
        totalBox.addView(tvTotalValue);
        root.addView(totalBox);

        final List<ArticuloRowHolder> rows = new ArrayList<>();

        final Runnable recalcularTotalGeneral = new Runnable() {
            @Override
            public void run() {
                long suma = 0L;
                for (ArticuloRowHolder holder : rows) {
                    double cant = 0.0;
                    try {
                        cant = Double.parseDouble(holder.etCantidad.getText().toString().trim().replace(",", "."));
                    } catch (Exception ignored) {
                    }
                    long precioUnit = CurrencyUtils.parseMontoToCentavos(holder.etPrecioUnitario.getText().toString());
                    if (precioUnit < 0) precioUnit = 0L;
                    ArticuloCompra temp = new ArticuloCompra();
                    temp.cantidad = cant > 0 ? cant : 0.0;
                    temp.precioUnitarioCentavos = precioUnit;
                    holder.subtotalCentavos = temp.calcularSubtotalCentavos();
                    holder.tvSubtotal.setText("Subtotal: " + CurrencyUtils.formatConMoneda(holder.subtotalCentavos));
                    suma += holder.subtotalCentavos;
                }
                tvTotalValue.setText(CurrencyUtils.formatConMoneda(suma));
            }
        };

        final View.OnClickListener addRowAction = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addArticuloRow(context, itemsContainer, rows, null, recalcularTotalGeneral);
            }
        };
        btnAddItem.setOnClickListener(addRowAction);

        // Pre-llenar datos
        if (esEdicion) {
            etComercio.setText(existente.comercio != null ? existente.comercio : "");
            etFecha.setText(existente.fecha);
            etDescripcion.setText(existente.descripcion != null ? existente.descripcion : "");
            etNotas.setText(existente.notas != null ? existente.notas : "");
            spNaturaleza.setSelection("FIJO".equals(existente.naturaleza) ? 1 : 0);
            for (int i = 0; i < METODOS_PAGO.length; i++) {
                if (METODOS_PAGO[i].equals(existente.metodoPago)) {
                    spMetodo.setSelection(i);
                    break;
                }
            }
            for (int i = 0; i < categoriasGasto.size(); i++) {
                if (categoriasGasto.get(i).id == existente.categoriaId) {
                    spCategoria.setSelection(i);
                    break;
                }
            }
            if (existente.articulos != null && !existente.articulos.isEmpty()) {
                for (ArticuloCompra art : existente.articulos) {
                    addArticuloRow(context, itemsContainer, rows, art, recalcularTotalGeneral);
                }
            } else {
                addArticuloRow(context, itemsContainer, rows, null, recalcularTotalGeneral);
            }
        } else {
            etFecha.setText(DateUtils.getTodayIso());
            // Seleccionar Supermercado (id 11) por defecto si existe
            for (int i = 0; i < categoriasGasto.size(); i++) {
                if (categoriasGasto.get(i).id == 11) {
                    spCategoria.setSelection(i);
                    break;
                }
            }
            addArticuloRow(context, itemsContainer, rows, null, recalcularTotalGeneral);
        }
        recalcularTotalGeneral.run();

        // Botones inferiores
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
                "Guardar compra",
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
                UiHelper.setFieldError(null, globalErrorTv, null);
                UiHelper.setFieldError(etComercio, errComercio, null);
                UiHelper.setFieldError(etFecha, errFecha, null);
                UiHelper.setFieldError(etDescripcion, errDescripcion, null);
                UiHelper.setFieldError(etNotas, errNotas, null);
                UiHelper.setFieldError(null, errCategoria, null);
                UiHelper.setFieldError(null, errArticulos, null);

                String comercio = etComercio.getText().toString().trim();
                String fecha = etFecha.getText().toString().trim();
                String desc = etDescripcion.getText().toString().trim();
                String notas = etNotas.getText().toString().trim();
                String metodo = METODOS_PAGO[spMetodo.getSelectedItemPosition()];
                String naturaleza = NATURALEZAS[spNaturaleza.getSelectedItemPosition()];

                boolean valid = true;
                if (comercio.isEmpty() || comercio.length() > 120) {
                    UiHelper.setFieldError(etComercio, errComercio, "El comercio es obligatorio (1 a 120 caracteres).");
                    valid = false;
                } else if (ValidationUtils.containsEmoji(comercio)) {
                    UiHelper.setFieldError(etComercio, errComercio, "No se permiten emojis.");
                    valid = false;
                }

                if (!ValidationUtils.isValidIsoDate(fecha)) {
                    UiHelper.setFieldError(etFecha, errFecha, "Formato requerido: YYYY-MM-DD.");
                    valid = false;
                }

                if (desc.length() > 200) {
                    UiHelper.setFieldError(etDescripcion, errDescripcion, "Maximo 200 caracteres.");
                    valid = false;
                } else if (ValidationUtils.containsEmoji(desc)) {
                    UiHelper.setFieldError(etDescripcion, errDescripcion, "No se permiten emojis.");
                    valid = false;
                }

                if (notas.length() > 1000) {
                    UiHelper.setFieldError(etNotas, errNotas, "Maximo 1000 caracteres.");
                    valid = false;
                } else if (ValidationUtils.containsEmoji(notas)) {
                    UiHelper.setFieldError(etNotas, errNotas, "No se permiten emojis.");
                    valid = false;
                }

                if (categoriasGasto.isEmpty() || spCategoria.getSelectedItemPosition() < 0) {
                    UiHelper.setFieldError(null, errCategoria, "Seleccione una categoria de gasto.");
                    valid = false;
                }

                if (rows.isEmpty() || rows.size() > 200) {
                    UiHelper.setFieldError(null, errArticulos, "Debe incluir entre 1 y 200 articulos.");
                    valid = false;
                }

                List<ArticuloCompra> listaArticulos = new ArrayList<>();
                long sumaTotal = 0L;

                for (int i = 0; i < rows.size(); i++) {
                    ArticuloRowHolder h = rows.get(i);
                    UiHelper.setFieldError(null, h.tvRowError, null);
                    String artName = h.etNombre.getText().toString().trim();
                    String cantStr = h.etCantidad.getText().toString().trim().replace(",", ".");
                    String precioStr = h.etPrecioUnitario.getText().toString().trim();

                    double cant = -1.0;
                    try {
                        cant = Double.parseDouble(cantStr);
                    } catch (Exception ignored) {
                    }
                    long precioCent = CurrencyUtils.parseMontoToCentavos(precioStr);

                    if (artName.isEmpty() || artName.length() > 120) {
                        UiHelper.setFieldError(h.etNombre, h.tvRowError, "Nombre de articulo obligatorio (max 120 caracteres).");
                        valid = false;
                        continue;
                    }
                    if (ValidationUtils.containsEmoji(artName)) {
                        UiHelper.setFieldError(h.etNombre, h.tvRowError, "No se permiten emojis en el nombre del articulo.");
                        valid = false;
                        continue;
                    }
                    if (!ValidationUtils.isValidCantidad(cant)) {
                        UiHelper.setFieldError(h.etCantidad, h.tvRowError, "Cantidad debe ser mayor a 0 y hasta 99999.");
                        valid = false;
                        continue;
                    }
                    if (!ValidationUtils.isValidPrecioUnitarioCentavos(precioCent)) {
                        UiHelper.setFieldError(h.etPrecioUnitario, h.tvRowError, "Precio unitario invalido.");
                        valid = false;
                        continue;
                    }

                    ArticuloCompra a = new ArticuloCompra();
                    a.articulo = artName;
                    a.cantidad = cant;
                    a.unidad = UNIDADES[h.spUnidad.getSelectedItemPosition()];
                    a.precioUnitarioCentavos = precioCent;
                    a.orden = i;
                    sumaTotal += a.calcularSubtotalCentavos();
                    listaArticulos.add(a);
                }

                if (valid && sumaTotal <= 0L) {
                    UiHelper.setFieldError(null, errArticulos, "El total de la compra debe ser mayor que 0.00.");
                    valid = false;
                }

                if (!valid) {
                    return;
                }

                int catId = categoriasGasto.get(spCategoria.getSelectedItemPosition()).id;
                btnGuardar.setEnabled(false);

                ApiCallback<Compra> cb = new ApiCallback<Compra>() {
                    @Override
                    public void onSuccess(Compra data, PaginacionMeta meta) {
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
                        Map<String, String> map = error.getMapaErroresPorCampo();
                        if (map.containsKey("comercio")) UiHelper.setFieldError(etComercio, errComercio, map.get("comercio"));
                        if (map.containsKey("fecha")) UiHelper.setFieldError(etFecha, errFecha, map.get("fecha"));
                        if (map.containsKey("descripcion")) UiHelper.setFieldError(etDescripcion, errDescripcion, map.get("descripcion"));
                        if (map.containsKey("categoria_id")) UiHelper.setFieldError(null, errCategoria, map.get("categoria_id"));
                        if (map.containsKey("articulos")) UiHelper.setFieldError(null, errArticulos, map.get("articulos"));

                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            String key = entry.getKey();
                            if (key != null && key.startsWith("articulos[")) {
                                try {
                                    int closeIdx = key.indexOf(']');
                                    int idx = Integer.parseInt(key.substring(10, closeIdx));
                                    if (idx >= 0 && idx < rows.size()) {
                                        UiHelper.setFieldError(null, rows.get(idx).tvRowError, entry.getValue());
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                };

                if (esEdicion) {
                    ApiClient.getInstance().actualizarCompra(
                            existente.id, naturaleza, fecha, comercio, desc, catId, metodo, notas, listaArticulos, cb
                    );
                } else {
                    ApiClient.getInstance().crearCompra(
                            naturaleza, fecha, comercio, desc, catId, metodo, notas, listaArticulos, cb
                    );
                }
            }
        });

        dialog.show();
    }

    private static void addArticuloRow(
            final Context context,
            final LinearLayout parent,
            final List<ArticuloRowHolder> rows,
            ArticuloCompra preload,
            final Runnable onChanged) {

        final ArticuloRowHolder holder = new ArticuloRowHolder();
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_card);
        int pad = UiHelper.dp(context, 10);
        box.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        boxLp.bottomMargin = UiHelper.dp(context, 8);
        box.setLayoutParams(boxLp);
        holder.container = box;

        // Fila 1: Nombre del articulo + boton quitar
        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        holder.etNombre = UiHelper.createInput(context, "Nombre del articulo *");
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameLp.rightMargin = UiHelper.dp(context, 8);
        holder.etNombre.setLayoutParams(nameLp);
        topRow.addView(holder.etNombre);

        TextView btnRemove = new TextView(context);
        FaIcon.apply(btnRemove, "fa-solid fa-square-minus");
        btnRemove.setTextColor(UiHelper.COLOR_PELIGRO);
        btnRemove.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        btnRemove.setPadding(UiHelper.dp(context, 8), UiHelper.dp(context, 4), UiHelper.dp(context, 8), UiHelper.dp(context, 4));
        btnRemove.setContentDescription("Quitar articulo");
        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rows.size() <= 1) {
                    return;
                }
                parent.removeView(holder.container);
                rows.remove(holder);
                onChanged.run();
            }
        });
        topRow.addView(btnRemove);
        box.addView(topRow);

        // Fila 2: Cantidad | Unidad | Precio Unitario
        LinearLayout midRow = new LinearLayout(context);
        midRow.setOrientation(LinearLayout.HORIZONTAL);
        midRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams midLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        midLp.topMargin = UiHelper.dp(context, 6);
        midRow.setLayoutParams(midLp);

        holder.etCantidad = UiHelper.createInput(context, "Cant.");
        holder.etCantidad.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        holder.etCantidad.setText("1");
        LinearLayout.LayoutParams cantLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f);
        cantLp.rightMargin = UiHelper.dp(context, 6);
        holder.etCantidad.setLayoutParams(cantLp);
        midRow.addView(holder.etCantidad);

        holder.spUnidad = new Spinner(context);
        holder.spUnidad.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, UNIDADES));
        LinearLayout.LayoutParams uniLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f);
        uniLp.rightMargin = UiHelper.dp(context, 6);
        holder.spUnidad.setLayoutParams(uniLp);
        midRow.addView(holder.spUnidad);

        holder.etPrecioUnitario = UiHelper.createInput(context, "Precio unit.");
        holder.etPrecioUnitario.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        LinearLayout.LayoutParams precLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        holder.etPrecioUnitario.setLayoutParams(precLp);
        midRow.addView(holder.etPrecioUnitario);

        box.addView(midRow);

        // Fila 3: Subtotal calculado
        holder.tvSubtotal = new TextView(context);
        holder.tvSubtotal.setText("Subtotal: " + CurrencyUtils.formatConMoneda(0L));
        holder.tvSubtotal.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        holder.tvSubtotal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        holder.tvSubtotal.setTypeface(Typeface.DEFAULT_BOLD);
        holder.tvSubtotal.setGravity(Gravity.END);
        UiHelper.applyTabularNums(holder.tvSubtotal);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        subLp.topMargin = UiHelper.dp(context, 4);
        holder.tvSubtotal.setLayoutParams(subLp);
        box.addView(holder.tvSubtotal);

        holder.tvRowError = UiHelper.createFieldErrorText(context);
        box.addView(holder.tvRowError);

        if (preload != null) {
            holder.etNombre.setText(preload.articulo != null ? preload.articulo : "");
            if (preload.cantidad == (long) preload.cantidad) {
                holder.etCantidad.setText(String.format(Locale.US, "%d", (long) preload.cantidad));
            } else {
                holder.etCantidad.setText(String.valueOf(preload.cantidad));
            }
            holder.etPrecioUnitario.setText(CurrencyUtils.centavosToInputString(preload.precioUnitarioCentavos));
            for (int i = 0; i < UNIDADES.length; i++) {
                if (UNIDADES[i].equals(preload.unidad)) {
                    holder.spUnidad.setSelection(i);
                    break;
                }
            }
        }

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                onChanged.run();
            }
        };
        holder.etCantidad.addTextChangedListener(watcher);
        holder.etPrecioUnitario.addTextChangedListener(watcher);

        rows.add(holder);
        parent.addView(box);
        onChanged.run();
    }
}
