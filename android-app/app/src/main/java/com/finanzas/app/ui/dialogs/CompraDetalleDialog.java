package com.finanzas.app.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.finanzas.app.R;
import com.finanzas.app.model.ArticuloCompra;
import com.finanzas.app.model.Compra;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;

import java.util.Locale;

/**
 * Dialogo de vista de detalle tipo Ticket de una Compra (GET /compras/{id}).
 */
public class CompraDetalleDialog {

    public interface OnTicketActionListener {
        void onEdit(Compra compra);

        void onDelete(Compra compra);
    }

    public static void show(final Context context, final Compra compra, final OnTicketActionListener listener) {
        if (compra == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ScrollView scroll = new ScrollView(context);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiHelper.COLOR_SUPERFICIE);
        int pad = UiHelper.dp(context, 16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        // Encabezado del ticket
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView iconTv = new TextView(context);
        FaIcon.apply(iconTv, "fa-solid fa-receipt");
        iconTv.setTextColor(UiHelper.COLOR_PRIMARIO);
        iconTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        iconLp.rightMargin = UiHelper.dp(context, 10);
        iconTv.setLayoutParams(iconLp);
        header.addView(iconTv);

        TextView titleTv = new TextView(context);
        titleTv.setText(compra.comercio != null && !compra.comercio.isEmpty() ? compra.comercio : "Detalle de Compra");
        titleTv.setTextColor(UiHelper.COLOR_PRIMARIO);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(titleTv);
        root.addView(header);

        // Metadatos de la compra
        TextView metaTv = new TextView(context);
        String catName = compra.categoria != null ? compra.categoria.nombre : "Gasto";
        String metaStr = "Fecha: " + compra.fecha
                + " | Categoria: " + catName
                + "\nMetodo: " + FaIcon.getMetodoPagoLabel(compra.metodoPago)
                + " | Naturaleza: " + compra.naturaleza;
        if (compra.descripcion != null && !compra.descripcion.isEmpty()) {
            metaStr += "\nDescripcion: " + compra.descripcion;
        }
        if (compra.notas != null && !compra.notas.isEmpty()) {
            metaStr += "\nNotas: " + compra.notas;
        }
        metaTv.setText(metaStr);
        metaTv.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        metaTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        metaLp.topMargin = UiHelper.dp(context, 8);
        metaLp.bottomMargin = UiHelper.dp(context, 12);
        metaTv.setLayoutParams(metaLp);
        root.addView(metaTv);

        // Encabezado de tabla (#1B2A4A con texto blanco, UI_DESIGN.md Seccion 4)
        LinearLayout tableHeader = new LinearLayout(context);
        tableHeader.setOrientation(LinearLayout.HORIZONTAL);
        tableHeader.setBackgroundColor(UiHelper.COLOR_PRIMARIO);
        int pad8 = UiHelper.dp(context, 8);
        tableHeader.setPadding(pad8, pad8, pad8, pad8);

        TextView thArt = new TextView(context);
        thArt.setText("ARTICULO");
        thArt.setTextColor(UiHelper.COLOR_TEXTO_INVERTIDO);
        thArt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        thArt.setTypeface(Typeface.DEFAULT_BOLD);
        thArt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.8f));

        TextView thCant = new TextView(context);
        thCant.setText("CANT.");
        thCant.setTextColor(UiHelper.COLOR_TEXTO_INVERTIDO);
        thCant.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        thCant.setTypeface(Typeface.DEFAULT_BOLD);
        thCant.setGravity(Gravity.END);
        thCant.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f));

        TextView thSub = new TextView(context);
        thSub.setText("SUBTOTAL");
        thSub.setTextColor(UiHelper.COLOR_TEXTO_INVERTIDO);
        thSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        thSub.setTypeface(Typeface.DEFAULT_BOLD);
        thSub.setGravity(Gravity.END);
        thSub.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f));

        tableHeader.addView(thArt);
        tableHeader.addView(thCant);
        tableHeader.addView(thSub);
        root.addView(tableHeader);

        // Filas alternas (#FFFFFF / #F4F6F8)
        if (compra.articulos != null) {
            for (int i = 0; i < compra.articulos.size(); i++) {
                ArticuloCompra art = compra.articulos.get(i);
                LinearLayout row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackgroundColor((i % 2 == 0) ? UiHelper.COLOR_SUPERFICIE : UiHelper.COLOR_FONDO);
                row.setPadding(pad8, pad8, pad8, pad8);

                TextView tvName = new TextView(context);
                tvName.setText(art.articulo + "\n(" + CurrencyUtils.formatNeutro(art.precioUnitarioCentavos) + " c/u)");
                tvName.setTextColor(UiHelper.COLOR_TEXTO);
                tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                tvName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.8f));

                String cantFormatted = (art.cantidad == (long) art.cantidad)
                        ? String.format(Locale.US, "%d", (long) art.cantidad)
                        : String.valueOf(art.cantidad);
                TextView tvCant = new TextView(context);
                tvCant.setText(cantFormatted + " " + FaIcon.getUnidadLabel(art.unidad));
                tvCant.setTextColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
                tvCant.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                tvCant.setGravity(Gravity.END);
                UiHelper.applyTabularNums(tvCant);
                tvCant.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f));

                TextView tvSub = new TextView(context);
                tvSub.setText(CurrencyUtils.formatNeutro(art.subtotalCentavos));
                tvSub.setTextColor(UiHelper.COLOR_TEXTO);
                tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                tvSub.setTypeface(Typeface.DEFAULT_BOLD);
                tvSub.setGravity(Gravity.END);
                UiHelper.applyTabularNums(tvSub);
                tvSub.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.1f));

                row.addView(tvName);
                row.addView(tvCant);
                row.addView(tvSub);
                root.addView(row);
            }
        }

        // Total del ticket
        LinearLayout totalRow = new LinearLayout(context);
        totalRow.setOrientation(LinearLayout.HORIZONTAL);
        totalRow.setBackgroundResource(R.drawable.bg_badge_primary);
        totalRow.setPadding(pad8, pad8, pad8, pad8);
        LinearLayout.LayoutParams totLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        totLp.topMargin = UiHelper.dp(context, 8);
        totalRow.setLayoutParams(totLp);

        TextView lblTotal = new TextView(context);
        lblTotal.setText("TOTAL (" + compra.cantidadArticulos + " articulos)");
        lblTotal.setTextColor(UiHelper.COLOR_PRIMARIO);
        lblTotal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        lblTotal.setTypeface(Typeface.DEFAULT_BOLD);
        lblTotal.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView valTotal = new TextView(context);
        valTotal.setText(CurrencyUtils.formatPorTipo(compra.montoCentavos, "GASTO"));
        valTotal.setTextColor(UiHelper.COLOR_PELIGRO);
        valTotal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        valTotal.setTypeface(Typeface.DEFAULT_BOLD);
        UiHelper.applyTabularNums(valTotal);

        totalRow.addView(lblTotal);
        totalRow.addView(valTotal);
        root.addView(totalRow);

        // Botones de accion
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnRowLp.topMargin = UiHelper.dp(context, 16);
        btnRow.setLayoutParams(btnRowLp);

        LinearLayout btnEliminar = UiHelper.createActionButton(
                context,
                "fa-solid fa-trash-can",
                "Eliminar",
                R.drawable.bg_button_danger,
                UiHelper.COLOR_TEXTO_INVERTIDO
        );
        LinearLayout.LayoutParams delLp = (LinearLayout.LayoutParams) btnEliminar.getLayoutParams();
        delLp.rightMargin = UiHelper.dp(context, 8);
        btnEliminar.setLayoutParams(delLp);

        LinearLayout btnEditar = UiHelper.createActionButton(
                context,
                "fa-solid fa-pen",
                "Editar",
                R.drawable.bg_button_secondary,
                UiHelper.COLOR_SECUNDARIO
        );
        LinearLayout.LayoutParams editLp = (LinearLayout.LayoutParams) btnEditar.getLayoutParams();
        editLp.rightMargin = UiHelper.dp(context, 8);
        btnEditar.setLayoutParams(editLp);

        LinearLayout btnCerrar = UiHelper.createActionButton(
                context,
                "fa-solid fa-xmark",
                "Cerrar",
                R.drawable.bg_button_primary,
                UiHelper.COLOR_TEXTO_INVERTIDO
        );

        btnRow.addView(btnEliminar);
        btnRow.addView(btnEditar);
        btnRow.addView(btnCerrar);
        root.addView(btnRow);

        builder.setView(scroll);
        final AlertDialog dialog = builder.create();

        btnCerrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        btnEditar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (listener != null) listener.onEdit(compra);
            }
        });
        btnEliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (listener != null) listener.onDelete(compra);
            }
        });

        dialog.show();
    }
}
