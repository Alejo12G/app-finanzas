package com.finanzas.app.ui.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import com.finanzas.app.model.DashboardData;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.UiHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Grafico nativo en Canvas de barras horizontales para Gastos por Categoria (UI_DESIGN.md 6.1).
 * Sin dependencias externas ni efectos 3D.
 */
public class CategoriaBarChartView extends View {

    private List<DashboardData.MontoCategoriaItem> items = new ArrayList<>();
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectBuffer = new RectF();

    public CategoriaBarChartView(Context context) {
        super(context);
        labelPaint.setColor(UiHelper.COLOR_TEXTO);
        labelPaint.setTextSize(UiHelper.dp(context, 13));
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);

        valuePaint.setColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        valuePaint.setTextSize(UiHelper.dp(context, 12));
        valuePaint.setTextAlign(Paint.Align.RIGHT);

        trackPaint.setColor(UiHelper.COLOR_BORDE);
        trackPaint.setStyle(Paint.Style.FILL);

        barPaint.setStyle(Paint.Style.FILL);
    }

    public void setItems(List<DashboardData.MontoCategoriaItem> data) {
        this.items = data != null ? data : new ArrayList<DashboardData.MontoCategoriaItem>();
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int count = items != null ? items.size() : 0;
        int rowHeight = UiHelper.dp(getContext(), 44);
        int height = count == 0 ? UiHelper.dp(getContext(), 48) : (count * rowHeight + UiHelper.dp(getContext(), 8));
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        valuePaint.setColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        trackPaint.setColor(UiHelper.COLOR_BORDE);
        if (items == null || items.isEmpty()) {
            labelPaint.setColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
            canvas.drawText("Sin gastos por categoria en este periodo.", UiHelper.dp(getContext(), 4), UiHelper.dp(getContext(), 28), labelPaint);
            return;
        }

        int width = getWidth();
        int rowHeight = UiHelper.dp(getContext(), 44);
        int barHeight = UiHelper.dp(getContext(), 10);
        int radius = UiHelper.dp(getContext(), 4);
        int padH = UiHelper.dp(getContext(), 4);

        for (int i = 0; i < items.size(); i++) {
            DashboardData.MontoCategoriaItem item = items.get(i);
            float top = i * rowHeight + UiHelper.dp(getContext(), 6);
            String catName = (item.categoria != null && item.categoria.nombre != null) ? item.categoria.nombre : "Categoria";
            String rightLabel = CurrencyUtils.formatNeutro(item.totalCentavos)
                    + String.format(Locale.US, " (%.2f %%)", item.porcentaje);

            labelPaint.setColor(UiHelper.COLOR_TEXTO);
            canvas.drawText(catName, padH, top + UiHelper.dp(getContext(), 14), labelPaint);
            canvas.drawText(rightLabel, width - padH, top + UiHelper.dp(getContext(), 14), valuePaint);

            float barTop = top + UiHelper.dp(getContext(), 22);
            float barBottom = barTop + barHeight;
            float usableWidth = width - (padH * 2);

            rectBuffer.set(padH, barTop, padH + usableWidth, barBottom);
            canvas.drawRoundRect(rectBuffer, radius, radius, trackPaint);

            float pct = (float) Math.max(0.0, Math.min(100.0, item.porcentaje));
            float fillWidth = (usableWidth * pct) / 100f;
            if (fillWidth > 0) {
                int barColor = (item.categoria != null)
                        ? UiHelper.parseColorSafe(item.categoria.color, UiHelper.COLOR_PRIMARIO)
                        : UiHelper.COLOR_PRIMARIO;
                barPaint.setColor(barColor);
                rectBuffer.set(padH, barTop, padH + fillWidth, barBottom);
                canvas.drawRoundRect(rectBuffer, radius, radius, barPaint);
            }
        }
    }
}
