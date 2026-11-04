package com.finanzas.app.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.CreditCard;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.DateUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;
import com.finanzas.app.util.ValidationUtils;

import org.json.JSONObject;

import java.util.Map;

/**
 * Dialogo bancario sobrio para registrar pagos de tarjeta de credito
 * (POST /api/v1/credit-cards/{id}/payments) segun API_CONTRACT.md Seccion 11.2.
 */
public class PaymentCardDialog {

    public interface OnPaymentRegisteredListener {
        void onPaymentRegistered(JSONObject responseData);
    }

    private static final String[] PAYMENT_METHOD_CODES = {
            "DEBITO",
            "TRANSFERENCIA",
            "EFECTIVO"
    };

    private static final String[] PAYMENT_METHOD_LABELS = {
            "Tarjeta de debito (DEBITO)",
            "Transferencia bancaria (TRANSFERENCIA)",
            "Efectivo (EFECTIVO)"
    };

    public static void show(
            final Context context,
            final CreditCard card,
            final String paymentType,
            final OnPaymentRegisteredListener listener) {

        if (context == null || card == null) {
            return;
        }

        final String resolvedPaymentType = (paymentType != null && !paymentType.trim().isEmpty())
                ? paymentType.trim().toUpperCase()
                : "MINIMO";

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View rootView = LayoutInflater.from(context).inflate(R.layout.dialog_credit_card_payment, null, false);

        TextView tvTitle = rootView.findViewById(R.id.tvDialogPaymentTitle);
        if ("MINIMO".equals(resolvedPaymentType)) {
            tvTitle.setText("Pagar Minimo - " + card.name);
        } else if ("TOTAL".equals(resolvedPaymentType)) {
            tvTitle.setText("Pagar Total - " + card.name);
        } else {
            tvTitle.setText("Abono a tarjeta - " + card.name);
        }

        TextView tvSubtitle = rootView.findViewById(R.id.tvDialogPaymentSubtitle);
        tvSubtitle.setText("Banco: " + card.bank + " | Proximo limite: "
                + (card.next_payment_date != null && !card.next_payment_date.isEmpty() ? card.next_payment_date : "Dia " + card.payment_day));

        final TextView globalErrorTv = rootView.findViewById(R.id.tvPaymentGlobalError);

        TextView tvInfoMinimum = rootView.findViewById(R.id.tvPaymentInfoMinimum);
        tvInfoMinimum.setText("Pago Minimo sugerido: " + CurrencyUtils.formatUnitsConMoneda(card.minimum_payment));
        UiHelper.applyTabularNums(tvInfoMinimum);

        TextView tvInfoFull = rootView.findViewById(R.id.tvPaymentInfoFull);
        tvInfoFull.setText("Pago Total (0% interes): " + CurrencyUtils.formatUnitsConMoneda(card.full_payment));
        UiHelper.applyTabularNums(tvInfoFull);

        TextView tvInfoDebt = rootView.findViewById(R.id.tvPaymentInfoDebt);
        tvInfoDebt.setText("Deuda Total pendiente: " + CurrencyUtils.formatUnitsConMoneda(card.total_debt));
        UiHelper.applyTabularNums(tvInfoDebt);

        TextView tvLabelAmount = rootView.findViewById(R.id.tvLabelPaymentAmount);
        tvLabelAmount.setText("Monto a pagar (" + CurrencyUtils.getActiveCurrency() + ") *");

        final EditText etAmount = rootView.findViewById(R.id.etPaymentAmount);
        final TextView errAmount = rootView.findViewById(R.id.errPaymentAmount);

        if ("MINIMO".equals(resolvedPaymentType)) {
            double prefill = card.minimum_payment > 0.0 ? card.minimum_payment : card.total_debt;
            if (prefill > 0.0) {
                etAmount.setText(CurrencyUtils.unitsToInputString(prefill));
            }
        } else if ("TOTAL".equals(resolvedPaymentType)) {
            double prefill = card.full_payment > 0.0 ? card.full_payment : card.total_debt;
            if (prefill > 0.0) {
                etAmount.setText(CurrencyUtils.unitsToInputString(prefill));
            }
        } else {
            etAmount.setText("");
        }

        final Spinner spMethod = rootView.findViewById(R.id.spPaymentMethod);
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                PAYMENT_METHOD_LABELS
        );
        methodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMethod.setAdapter(methodAdapter);
        spMethod.setSelection(0);

        final EditText etDate = rootView.findViewById(R.id.etPaymentDate);
        final TextView errDate = rootView.findViewById(R.id.errPaymentDate);
        etDate.setText(DateUtils.getTodayIso());

        TextView tvPickDateIcon = rootView.findViewById(R.id.tvPickPaymentDateIcon);
        FaIcon.apply(tvPickDateIcon, "fa-solid fa-calendar-days");

        LinearLayout btnPickDate = rootView.findViewById(R.id.btnPickPaymentDate);
        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateUtils.showDatePicker(context, etDate);
            }
        });

        final CheckBox cbCreateTransaction = rootView.findViewById(R.id.cbCreateTransaction);
        cbCreateTransaction.setChecked(true);

        final EditText etNotes = rootView.findViewById(R.id.etPaymentNotes);
        final TextView errNotes = rootView.findViewById(R.id.errPaymentNotes);
        etNotes.setText("Pago de tarjeta desde la aplicacion");

        TextView tvBtnCancelIcon = rootView.findViewById(R.id.tvBtnCancelPaymentIcon);
        FaIcon.apply(tvBtnCancelIcon, "fa-solid fa-xmark");

        TextView tvBtnConfirmIcon = rootView.findViewById(R.id.tvBtnConfirmPaymentIcon);
        FaIcon.apply(tvBtnConfirmIcon, "fa-solid fa-check");

        LinearLayout btnCancel = rootView.findViewById(R.id.btnCancelPayment);
        final LinearLayout btnConfirm = rootView.findViewById(R.id.btnConfirmPayment);

        builder.setView(rootView);
        final AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiHelper.setFieldError(null, globalErrorTv, null);
                UiHelper.setFieldError(etAmount, errAmount, null);
                UiHelper.setFieldError(etDate, errDate, null);
                UiHelper.setFieldError(etNotes, errNotes, null);

                String amountStr = etAmount.getText().toString().trim().replace(",", ".");
                String dateStr = etDate.getText().toString().trim();
                String notesStr = etNotes.getText().toString().trim();

                boolean valid = true;

                double amount = -1.0;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (Exception ignored) {
                }
                if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0.0 || amount > 999999999999.0) {
                    UiHelper.setFieldError(etAmount, errAmount, "Ingrese un monto a pagar mayor a 0.00.");
                    valid = false;
                }

                if (!ValidationUtils.isValidIsoDate(dateStr)) {
                    UiHelper.setFieldError(etDate, errDate, "Formato de fecha invalido (YYYY-MM-DD).");
                    valid = false;
                }

                if (notesStr.length() > 500) {
                    UiHelper.setFieldError(etNotes, errNotes, "Maximo 500 caracteres.");
                    valid = false;
                } else if (ValidationUtils.containsEmoji(notesStr)) {
                    UiHelper.setFieldError(etNotes, errNotes, "No se permiten emojis en las notas.");
                    valid = false;
                }

                if (!valid) {
                    return;
                }

                int methodPos = spMethod.getSelectedItemPosition();
                String selectedMethod = (methodPos >= 0 && methodPos < PAYMENT_METHOD_CODES.length)
                        ? PAYMENT_METHOD_CODES[methodPos]
                        : "DEBITO";

                double normalizedAmount = CurrencyUtils.centavosToUnits(CurrencyUtils.unitsToCentavos(amount));
                boolean createTransaction = cbCreateTransaction.isChecked();

                btnConfirm.setEnabled(false);
                ApiClient.getInstance().payCreditCard(
                        card.id,
                        normalizedAmount,
                        resolvedPaymentType,
                        selectedMethod,
                        dateStr,
                        notesStr.isEmpty() ? "Pago de tarjeta desde la aplicacion" : notesStr,
                        createTransaction,
                        new ApiCallback<JSONObject>() {
                            @Override
                            public void onSuccess(JSONObject data, PaginacionMeta meta) {
                                btnConfirm.setEnabled(true);
                                dialog.dismiss();
                                Toast.makeText(context, "Pago registrado exitosamente", Toast.LENGTH_LONG).show();
                                if (listener != null) {
                                    listener.onPaymentRegistered(data);
                                }
                            }

                            @Override
                            public void onError(ApiError error) {
                                btnConfirm.setEnabled(true);
                                UiHelper.setFieldError(null, globalErrorTv, error.mensaje);
                                Map<String, String> fieldMap = error.getMapaErroresPorCampo();
                                if (fieldMap.containsKey("amount")) {
                                    UiHelper.setFieldError(etAmount, errAmount, fieldMap.get("amount"));
                                }
                                if (fieldMap.containsKey("date")) {
                                    UiHelper.setFieldError(etDate, errDate, fieldMap.get("date"));
                                }
                                if (fieldMap.containsKey("notes")) {
                                    UiHelper.setFieldError(etNotes, errNotes, fieldMap.get("notes"));
                                }
                            }
                        }
                );
            }
        });

        dialog.show();
    }
}
