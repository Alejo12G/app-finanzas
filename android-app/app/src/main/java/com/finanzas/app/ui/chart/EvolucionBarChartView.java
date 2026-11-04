package com.finanzas.app.ui.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.finanzas.app.model.DashboardData;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.DateUtils;
import com.finanzas.app.util.UiHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Grafico nativo en Canvas de evolucion de 6 meses: barras agrupadas Ingresos (#2E7D32) y Gastos (#C62828)
 * conforme a UI_DESIGN.md Seccion 6 y 6.1.
 */
public class EvolucionBarChartView extends View {

    private List<DashboardData.EvolucionMensualItem> items = new ArrayList<>();
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectBuffer = new RectF();

    public EvolucionBarChartView(Context context) {
        super(context);
        gridPaint.setColor(UiHelper.COLOR_BORDE);
        gridPaint.setStrokeWidth(UiHelper.dp(context, 1));

        axisTextPaint.setColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        axisTextPaint.setTextSize(UiHelper.dp(context, 11));

        legendPaint.setColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        legendPaint.setTextSize(UiHelper.dp(context, 12));

        barPaint.setStyle(Paint.Style.FILL);
    }

    public void setItems(List<DashboardData.EvolucionMensualItem> data) {
        this.items = data != null ? data : new ArrayList<DashboardData.EvolucionMensualItem>();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = UiHelper.dp(getContext(), 210);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        gridPaint.setColor(UiHelper.COLOR_BORDE);
        axisTextPaint.setColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        legendPaint.setColor(UiHelper.COLOR_TEXTO_SECUNDARIO);
        int w = getWidth();
        int h = getHeight();
        int dp8 = UiHelper.dp(getContext(), 8);
        int dp16 = UiHelper.dp(getContext(), 16);
        int dp24 = UiHelper.dp(getContext(), 24);

        // Leyenda superior: Ingresos (#2E7D32) | Gastos (#C62828)
        int boxSize = UiHelper.dp(getContext(), 10);
        barPaint.setColor(UiHelper.COLOR_EXITO);
        canvas.drawRect(dp8, dp8, dp8 + boxSize, dp8 + boxSize, barPaint);
        canvas.drawText("Ingresos", dp8 + boxSize + UiHelper.dp(getContext(), 6), dp8 + boxSize - 1, legendPaint);

        int leg2X = dp8 + UiHelper.dp(getContext(), 90);
        barPaint.setColor(UiHelper.COLOR_PELIGRO);
        canvas.drawRect(leg2X, dp8, leg2X + boxSize, dp8 + boxSize, barPaint);
        canvas.drawText("Gastos", leg2X + boxSize + UiHelper.dp(getContext(), 6), dp8 + boxSize - 1, legendPaint);

        float chartTop = UiHelper.dp(getContext(), 32);
        float chartBottom = h - dp24;
        float chartLeft = UiHelper.dp(getContext(), 56);
        float chartRight = w - dp8;
        float chartHeight = chartBottom - chartTop;

        long maxVal = 10000L; // Minimo 100.00 para escala
        if (items != null) {
            for (DashboardData.EvolucionMensualItem item : items) {
                if (item.ingresosCentavos > maxVal) maxVal = item.ingresosCentavos;
                if (item.gastosCentavos > maxVal) maxVal = item.gastosCentavos;
            }
        }

        // Cuadricula horizontal (3 niveles: 0%, 50%, 100%)
        axisTextPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 2; i++) {
            float ratio = i / 2.0f;
            float y = chartBottom - (chartHeight * ratio);
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);
            long labelVal = (long) (maxVal * ratio);
            String labelStr = CurrencyUtils.formatNeutro(labelVal);
            if (labelStr.endsWith(".00")) {
                labelStr = labelStr.substring(0, labelStr.length() - 3);
            }
            canvas.drawText(labelStr, chartLeft - UiHelper.dp(getContext(), 6), y + UiHelper.dp(getContext(), 4), axisTextPaint);
        }

        if (items == null || items.isEmpty()) {
            return;
        }

        int n = items.size();
        float slotWidth = (chartRight - chartLeft) / n;
        float barWidth = Math.min(UiHelper.dp(getContext(), 14), slotWidth * 0.32f);
        float gap = UiHelper.dp(getContext(), 3);
        int radius = UiHelper.dp(getContext(), 2);

        axisTextPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < n; i++) {
            DashboardData.EvolucionMensualItem item = items.get(i);
            float centerX = chartLeft + (i + 0.5f) * slotWidth;

            // Barra Ingresos
            float ingHeight = (float) item.ingresosCentavos / (float) maxVal * chartHeight;
            float ingTop = chartBottom - Math.max(UiHelper.dp(getContext(), 2), ingHeight);
            barPaint.setColor(UiHelper.COLOR_EXITO);
            rectBuffer.set(centerX - barWidth - (gap / 2f), ingTop, centerX - (gap / 2f), chartBottom);
            canvas.drawRoundRect(rectBuffer, radius, radius, barPaint);

            // Barra Gastos
            float gasHeight = (float) item.gastosCentavos / (float) maxVal * chartHeight;
            float gasTop = chartBottom - Math.max(UiHelper.dp(getContext(), 2), gasHeight);
            barPaint.setColor(UiHelper.COLOR_PELIGRO);
            rectBuffer.set(centerX + (gap / 2f), gasTop, centerX + (gap / 2f) + barWidth, chartBottom);
            canvas.drawRoundRect(rectBuffer, radius, radius, barPaint);

            // Etiqueta Mes
            String mLabel = DateUtils.getShortMonthLabel(item.mes) + " " + (item.anio % 100);
            canvas.drawText(mLabel, centerX, h - UiHelper.dp(getContext(), 6), axisTextPaint);
        }
    }
}
