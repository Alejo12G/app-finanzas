package com.finanzas.app.util;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.EditText;

import java.util.Calendar;
import java.util.Locale;

public final class DateUtils {

    private static final String[] NOMBRES_MESES = {
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };

    private static final String[] NOMBRES_MESES_CORTOS = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    private DateUtils() {
    }

    public static int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public static int getCurrentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    public static String getTodayIso() {
        Calendar cal = Calendar.getInstance();
        return formatIsoDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    public static String getYesterdayIso() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        return formatIsoDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    public static String formatIsoDate(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    public static String getMonthYearLabel(int anio, int mes) {
        if (mes < 1 || mes > 12) {
            return anio + "";
        }
        return NOMBRES_MESES[mes - 1] + " " + anio;
    }

    public static String getShortMonthLabel(int mes) {
        if (mes < 1 || mes > 12) {
            return "";
        }
        return NOMBRES_MESES_CORTOS[mes - 1];
    }

    public static void showDatePicker(Context context, final EditText targetInput) {
        Calendar cal = Calendar.getInstance();
        String currentVal = targetInput.getText().toString().trim();
        if (currentVal.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] parts = currentVal.split("-");
                cal.set(Calendar.YEAR, Integer.parseInt(parts[0]));
                cal.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));
            } catch (Exception ignored) {
            }
        }
        DatePickerDialog dlg = new DatePickerDialog(
                context,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        targetInput.setText(formatIsoDate(year, month + 1, dayOfMonth));
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dlg.show();
    }
}
