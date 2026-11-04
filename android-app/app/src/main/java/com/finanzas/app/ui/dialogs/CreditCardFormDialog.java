package com.finanzas.app.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.CreditCard;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;
import com.finanzas.app.util.ValidationUtils;

import java.util.Map;

/**
 * Dialogo de creacion (POST /api/v1/credit-cards) y edicion (PUT /api/v1/credit-cards/{id})
 * de tarjetas de credito.
 */
public class CreditCardFormDialog {

    public interface OnCreditCardSavedListener {
        void onSaved(CreditCard card);
    }

    public static void show(
            final Context context,
            final CreditCard existente,
            final OnCreditCardSavedListener listener) {

        final boolean esEdicion = (existente != null && existente.id > 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View rootView = LayoutInflater.from(context).inflate(R.layout.dialog_credit_card_form, null, false);

        TextView tvTitle = rootView.findViewById(R.id.tvDialogCardTitle);
        tvTitle.setText(esEdicion ? "Editar tarjeta de credito" : "Nueva tarjeta de credito");

        final TextView globalErrorTv = rootView.findViewById(R.id.tvCardGlobalError);

        TextView tvLabelCreditLimit = rootView.findViewById(R.id.tvLabelCreditLimit);
        tvLabelCreditLimit.setText("Cupo total aprobado (" + CurrencyUtils.getActiveCurrency() + ") *");

        final EditText etName = rootView.findViewById(R.id.etCardName);
        final TextView errName = rootView.findViewById(R.id.errCardName);

        final EditText etBank = rootView.findViewById(R.id.etCardBank);
        final TextView errBank = rootView.findViewById(R.id.errCardBank);

        final EditText etCreditLimit = rootView.findViewById(R.id.etCardCreditLimit);
        final TextView errCreditLimit = rootView.findViewById(R.id.errCardCreditLimit);

        final EditText etDefaultInterestRate = rootView.findViewById(R.id.etCardDefaultInterestRate);
        final TextView errDefaultInterestRate = rootView.findViewById(R.id.errCardDefaultInterestRate);

        final EditText etCutoffDay = rootView.findViewById(R.id.etCardCutoffDay);
        final TextView errCutoffDay = rootView.findViewById(R.id.errCardCutoffDay);

        final EditText etPaymentDay = rootView.findViewById(R.id.etCardPaymentDay);
        final TextView errPaymentDay = rootView.findViewById(R.id.errCardPaymentDay);

        TextView tvBtnCancelIcon = rootView.findViewById(R.id.tvBtnCancelCardIcon);
        FaIcon.apply(tvBtnCancelIcon, "fa-solid fa-xmark");

        TextView tvBtnSaveIcon = rootView.findViewById(R.id.tvBtnSaveCardIcon);
        FaIcon.apply(tvBtnSaveIcon, "fa-solid fa-floppy-disk");

        LinearLayout btnCancel = rootView.findViewById(R.id.btnCancelCard);
        final LinearLayout btnSave = rootView.findViewById(R.id.btnSaveCard);

        if (esEdicion) {
            etName.setText(existente.name != null ? existente.name : "");
            etBank.setText(existente.bank != null ? existente.bank : "");
            etCreditLimit.setText(CurrencyUtils.unitsToInputString(existente.credit_limit));
            etDefaultInterestRate.setText(CurrencyUtils.formatTasaInput(existente.default_interest_rate));
            etCutoffDay.setText(String.valueOf(existente.cutoff_day));
            etPaymentDay.setText(String.valueOf(existente.payment_day));
        }

        builder.setView(rootView);
        final AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UiHelper.setFieldError(null, globalErrorTv, null);
                UiHelper.setFieldError(etName, errName, null);
                UiHelper.setFieldError(etBank, errBank, null);
                UiHelper.setFieldError(etCreditLimit, errCreditLimit, null);
                UiHelper.setFieldError(etDefaultInterestRate, errDefaultInterestRate, null);
                UiHelper.setFieldError(etCutoffDay, errCutoffDay, null);
                UiHelper.setFieldError(etPaymentDay, errPaymentDay, null);

                String name = etName.getText().toString().trim();
                String bank = etBank.getText().toString().trim();
                String limitStr = etCreditLimit.getText().toString().trim().replace(",", ".");
                String rateStr = etDefaultInterestRate.getText().toString().trim().replace(",", ".");
                String cutoffStr = etCutoffDay.getText().toString().trim();
                String paymentStr = etPaymentDay.getText().toString().trim();

                boolean valid = true;

                if (name.isEmpty() || name.length() > 80) {
                    UiHelper.setFieldError(etName, errName, "Obligatorio, entre 1 y 80 caracteres.");
                    valid = false;
                } else if (ValidationUtils.containsEmoji(name)) {
                    UiHelper.setFieldError(etName, errName, "No se permiten emojis en el texto.");
                    valid = false;
                }

                if (bank.isEmpty() || bank.length() > 80) {
                    UiHelper.setFieldError(etBank, errBank, "Obligatorio, entre 1 y 80 caracteres.");
                    valid = false;
                } else if (ValidationUtils.containsEmoji(bank)) {
                    UiHelper.setFieldError(etBank, errBank, "No se permiten emojis en el texto.");
                    valid = false;
                }

                double creditLimit = -1.0;
                try {
                    creditLimit = Double.parseDouble(limitStr);
                } catch (Exception ignored) {
                }
                if (Double.isNaN(creditLimit) || Double.isInfinite(creditLimit) || creditLimit <= 0.0 || creditLimit > 999999999999.0) {
                    UiHelper.setFieldError(etCreditLimit, errCreditLimit, "Ingrese un cupo mayor a 0.00.");
                    valid = false;
                }

                double interestRate = -1.0;
                try {
                    interestRate = Double.parseDouble(rateStr);
                } catch (Exception ignored) {
                }
                if (Double.isNaN(interestRate) || Double.isInfinite(interestRate) || interestRate < 0.0 || interestRate > 100.0) {
                    UiHelper.setFieldError(etDefaultInterestRate, errDefaultInterestRate, "Ingrese una tasa mensual entre 0 y 100.");
                    valid = false;
                }

                int cutoffDay = -1;
                try {
                    cutoffDay = Integer.parseInt(cutoffStr);
                } catch (Exception ignored) {
                }
                if (cutoffDay < 1 || cutoffDay > 31) {
                    UiHelper.setFieldError(etCutoffDay, errCutoffDay, "Dia entre 1 y 31.");
                    valid = false;
                }

                int paymentDay = -1;
                try {
                    paymentDay = Integer.parseInt(paymentStr);
                } catch (Exception ignored) {
                }
                if (paymentDay < 1 || paymentDay > 31) {
                    UiHelper.setFieldError(etPaymentDay, errPaymentDay, "Dia entre 1 y 31.");
                    valid = false;
                }

                if (!valid) {
                    return;
                }

                CreditCard payload = new CreditCard();
                payload.name = name;
                payload.bank = bank;
                payload.credit_limit = CurrencyUtils.centavosToUnits(CurrencyUtils.unitsToCentavos(creditLimit));
                payload.default_interest_rate = interestRate;
                payload.cutoff_day = cutoffDay;
                payload.payment_day = paymentDay;

                btnSave.setEnabled(false);
                ApiCallback<CreditCard> cb = new ApiCallback<CreditCard>() {
                    @Override
                    public void onSuccess(CreditCard data, PaginacionMeta meta) {
                        btnSave.setEnabled(true);
                        dialog.dismiss();
                        if (listener != null) {
                            listener.onSaved(data);
                        }
                    }

                    @Override
                    public void onError(ApiError error) {
                        btnSave.setEnabled(true);
                        UiHelper.setFieldError(null, globalErrorTv, error.mensaje);
                        Map<String, String> fieldMap = error.getMapaErroresPorCampo();
                        if (fieldMap.containsKey("name")) {
                            UiHelper.setFieldError(etName, errName, fieldMap.get("name"));
                        }
                        if (fieldMap.containsKey("bank")) {
                            UiHelper.setFieldError(etBank, errBank, fieldMap.get("bank"));
                        }
                        if (fieldMap.containsKey("credit_limit")) {
                            UiHelper.setFieldError(etCreditLimit, errCreditLimit, fieldMap.get("credit_limit"));
                        }
                        if (fieldMap.containsKey("default_interest_rate")) {
                            UiHelper.setFieldError(etDefaultInterestRate, errDefaultInterestRate, fieldMap.get("default_interest_rate"));
                        }
                        if (fieldMap.containsKey("cutoff_day")) {
                            UiHelper.setFieldError(etCutoffDay, errCutoffDay, fieldMap.get("cutoff_day"));
                        }
                        if (fieldMap.containsKey("payment_day")) {
                            UiHelper.setFieldError(etPaymentDay, errPaymentDay, fieldMap.get("payment_day"));
                        }
                    }
                };

                if (esEdicion) {
                    ApiClient.getInstance().actualizarCreditCard(existente.id, payload, cb);
                } else {
                    ApiClient.getInstance().crearCreditCard(payload, cb);
                }
            }
        });

        dialog.show();
    }
}
