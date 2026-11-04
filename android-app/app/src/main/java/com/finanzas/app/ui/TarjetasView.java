package com.finanzas.app.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.finanzas.app.MainActivity;
import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.CreditCard;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.ui.dialogs.CreditCardFormDialog;
import com.finanzas.app.ui.dialogs.PaymentCardDialog;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.DateUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Vista de Gestion de Tarjetas de Credito (/api/v1/credit-cards).
 * Muestra en tarjetas visuales sobrias el cupo disponible, fechas reales de facturacion
 * (next_cutoff_date y next_payment_date), saldos de pago minimo/total/deuda total
 * y acciones rapidas de pago ("Pagar Minimo", "Pagar Total" y "Otro valor...").
 */
public class TarjetasView extends ScrollView {

    private final MainActivity host;
    private TextView tvResumenCupoDisponible;
    private TextView tvResumenCupoUsado;
    private TextView tvResumenCupoTotal;
    private LinearLayout layoutEmptyCreditCards;
    private LinearLayout creditCardsListContainer;

    public TarjetasView(Context context, MainActivity host) {
        super(context);
        this.host = host;
        setFillViewport(true);
        initUi(context);
    }

    private void initUi(final Context context) {
        View content = LayoutInflater.from(context).inflate(R.layout.view_credit_cards, this, false);
        addView(content);

        TextView tvBtnNuevaTarjetaIcon = content.findViewById(R.id.tvBtnNuevaTarjetaIcon);
        FaIcon.apply(tvBtnNuevaTarjetaIcon, "fa-solid fa-plus");

        TextView tvEmptyCardsIcon = content.findViewById(R.id.tvEmptyCardsIcon);
        FaIcon.apply(tvEmptyCardsIcon, "fa-solid fa-credit-card");

        tvResumenCupoDisponible = content.findViewById(R.id.tvResumenCupoDisponible);
        tvResumenCupoUsado = content.findViewById(R.id.tvResumenCupoUsado);
        tvResumenCupoTotal = content.findViewById(R.id.tvResumenCupoTotal);
        UiHelper.applyTabularNums(tvResumenCupoDisponible);
        UiHelper.applyTabularNums(tvResumenCupoUsado);
        UiHelper.applyTabularNums(tvResumenCupoTotal);

        layoutEmptyCreditCards = content.findViewById(R.id.layoutEmptyCreditCards);
        creditCardsListContainer = content.findViewById(R.id.creditCardsListContainer);

        LinearLayout btnNuevaTarjeta = content.findViewById(R.id.btnNuevaTarjeta);
        btnNuevaTarjeta.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                CreditCardFormDialog.show(context, null, new CreditCardFormDialog.OnCreditCardSavedListener() {
                    @Override
                    public void onSaved(CreditCard card) {
                        host.refreshCreditCardsCache();
                        loadData();
                    }
                });
            }
        });
    }

    public void loadData() {
        host.setGlobalLoading(true);
        ApiClient.getInstance().getCreditCards(new ApiCallback<List<CreditCard>>() {
            @Override
            public void onSuccess(List<CreditCard> data, PaginacionMeta meta) {
                host.setGlobalLoading(false);
                renderCards(data);
            }

            @Override
            public void onError(ApiError error) {
                host.setGlobalLoading(false);
                host.handleGlobalApiError(error);
            }
        });
    }

    private void renderCards(List<CreditCard> cards) {
        creditCardsListContainer.removeAllViews();

        long totalLimitCents = 0L;
        long totalAvailableCents = 0L;

        if (cards != null) {
            for (CreditCard c : cards) {
                totalLimitCents += CurrencyUtils.unitsToCentavos(c.credit_limit);
                totalAvailableCents += CurrencyUtils.unitsToCentavos(c.available_limit);
            }
        }
        long totalUsedCents = Math.max(0L, totalLimitCents - totalAvailableCents);

        tvResumenCupoDisponible.setText(CurrencyUtils.formatConMoneda(totalAvailableCents));
        tvResumenCupoUsado.setText(CurrencyUtils.formatConMoneda(totalUsedCents));
        tvResumenCupoTotal.setText(CurrencyUtils.formatConMoneda(totalLimitCents));

        if (cards == null || cards.isEmpty()) {
            layoutEmptyCreditCards.setVisibility(VISIBLE);
            return;
        }
        layoutEmptyCreditCards.setVisibility(GONE);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (final CreditCard card : cards) {
            View itemView = inflater.inflate(R.layout.item_credit_card, creditCardsListContainer, false);

            TextView tvCardIcon = itemView.findViewById(R.id.tvCardIcon);
            FaIcon.apply(tvCardIcon, "fa-solid fa-credit-card");

            TextView tvCardName = itemView.findViewById(R.id.tvCardName);
            tvCardName.setText(card.name);

            TextView tvCardBank = itemView.findViewById(R.id.tvCardBank);
            tvCardBank.setText("Banco: " + card.bank);

            TextView tvCardInterestRate = itemView.findViewById(R.id.tvCardInterestRate);
            tvCardInterestRate.setText(String.format(Locale.US, "Tasa: %.2f%% M.V.", card.default_interest_rate));
            UiHelper.applyTabularNums(tvCardInterestRate);

            TextView tvCardAvailableLimit = itemView.findViewById(R.id.tvCardAvailableLimit);
            tvCardAvailableLimit.setText(CurrencyUtils.formatUnitsConMoneda(card.available_limit));
            tvCardAvailableLimit.setTextColor(card.available_limit > 0 ? UiHelper.COLOR_EXITO : UiHelper.COLOR_PELIGRO);
            UiHelper.applyTabularNums(tvCardAvailableLimit);

            TextView tvCardTotalLimit = itemView.findViewById(R.id.tvCardTotalLimit);
            tvCardTotalLimit.setText("Cupo total: " + CurrencyUtils.formatUnitsConMoneda(card.credit_limit));
            UiHelper.applyTabularNums(tvCardTotalLimit);

            TextView tvCardUsedLimit = itemView.findViewById(R.id.tvCardUsedLimit);
            int usagePct = card.getUsagePercentage();
            tvCardUsedLimit.setText("Utilizado: " + CurrencyUtils.formatUnitsConMoneda(card.getUsedLimit()) + " (" + usagePct + "%)");
            UiHelper.applyTabularNums(tvCardUsedLimit);

            ProgressBar pbCardUtilization = itemView.findViewById(R.id.pbCardUtilization);
            pbCardUtilization.setProgress(usagePct);

            // Proximo corte (next_cutoff_date) con icono fa-calendar
            TextView tvCutoffIcon = itemView.findViewById(R.id.tvCutoffIcon);
            FaIcon.apply(tvCutoffIcon, "fa-solid fa-calendar");

            TextView tvCardCutoffDay = itemView.findViewById(R.id.tvCardCutoffDay);
            if (card.next_cutoff_date != null && !card.next_cutoff_date.trim().isEmpty()) {
                tvCardCutoffDay.setText("Proximo corte: " + card.next_cutoff_date + " (Dia " + card.cutoff_day + ")");
            } else {
                tvCardCutoffDay.setText("Fecha de corte: Dia " + card.cutoff_day);
            }
            UiHelper.applyTabularNums(tvCardCutoffDay);

            // Fecha limite de pago (next_payment_date) con icono fa-calendar-check y alerta <= 3 dias (#D97706)
            TextView tvPaymentIcon = itemView.findViewById(R.id.tvPaymentIcon);
            FaIcon.apply(tvPaymentIcon, "fa-solid fa-calendar-check");

            TextView tvCardPaymentDay = itemView.findViewById(R.id.tvCardPaymentDay);
            boolean dueSoon = DateUtils.isDueSoonOrOverdue(card.next_payment_date, 3);
            if (card.next_payment_date != null && !card.next_payment_date.trim().isEmpty()) {
                int daysRemaining = DateUtils.daysUntilIsoDate(card.next_payment_date);
                String suffix = "";
                if (daysRemaining == 0) {
                    suffix = " [Vence hoy]";
                } else if (daysRemaining > 0 && daysRemaining <= 3) {
                    suffix = " [Vence en " + daysRemaining + (daysRemaining == 1 ? " dia]" : " dias]");
                } else if (daysRemaining < 0) {
                    suffix = " [Vencido]";
                }
                tvCardPaymentDay.setText("Limite de pago: " + card.next_payment_date + suffix);
            } else {
                tvCardPaymentDay.setText("Limite de pago: Dia " + card.payment_day);
            }
            tvPaymentIcon.setTextColor(dueSoon ? UiHelper.COLOR_ADVERTENCIA_VENCIMIENTO : UiHelper.COLOR_PRIMARIO);
            tvCardPaymentDay.setTextColor(dueSoon ? UiHelper.COLOR_ADVERTENCIA_VENCIMIENTO : UiHelper.COLOR_TEXTO);
            UiHelper.applyTabularNums(tvCardPaymentDay);

            // Bloque de facturacion actual: minimum_payment, full_payment, total_debt
            TextView tvCardMinimumPayment = itemView.findViewById(R.id.tvCardMinimumPayment);
            tvCardMinimumPayment.setText(CurrencyUtils.formatUnitsConMoneda(card.minimum_payment));
            UiHelper.applyTabularNums(tvCardMinimumPayment);

            TextView tvCardFullPayment = itemView.findViewById(R.id.tvCardFullPayment);
            tvCardFullPayment.setText(CurrencyUtils.formatUnitsConMoneda(card.full_payment));
            UiHelper.applyTabularNums(tvCardFullPayment);

            TextView tvCardTotalDebt = itemView.findViewById(R.id.tvCardTotalDebt);
            tvCardTotalDebt.setText(CurrencyUtils.formatUnitsConMoneda(card.total_debt));
            tvCardTotalDebt.setTextColor(card.total_debt > 0.0 ? UiHelper.COLOR_PELIGRO : UiHelper.COLOR_EXITO);
            UiHelper.applyTabularNums(tvCardTotalDebt);

            // Botones de accion rapida de pago cuando total_debt > 0
            LinearLayout layoutCardPaymentActions = itemView.findViewById(R.id.layoutCardPaymentActions);
            if (card.total_debt > 0.0) {
                layoutCardPaymentActions.setVisibility(VISIBLE);

                TextView tvBtnPayMinimumIcon = itemView.findViewById(R.id.tvBtnPayMinimumIcon);
                FaIcon.apply(tvBtnPayMinimumIcon, "fa-solid fa-money-check");

                TextView tvBtnPayMinimumText = itemView.findViewById(R.id.tvBtnPayMinimumText);
                tvBtnPayMinimumText.setText("Pagar Minimo (" + CurrencyUtils.formatUnitsConMoneda(card.minimum_payment) + ")");
                UiHelper.applyTabularNums(tvBtnPayMinimumText);

                TextView tvBtnPayFullIcon = itemView.findViewById(R.id.tvBtnPayFullIcon);
                FaIcon.apply(tvBtnPayFullIcon, "fa-solid fa-check");

                TextView tvBtnPayFullText = itemView.findViewById(R.id.tvBtnPayFullText);
                tvBtnPayFullText.setText("Pagar Total (" + CurrencyUtils.formatUnitsConMoneda(card.full_payment) + ")");
                UiHelper.applyTabularNums(tvBtnPayFullText);

                TextView tvBtnPayOtherIcon = itemView.findViewById(R.id.tvBtnPayOtherIcon);
                FaIcon.apply(tvBtnPayOtherIcon, "fa-solid fa-hand-holding-dollar");

                LinearLayout btnPayMinimum = itemView.findViewById(R.id.btnPayMinimum);
                btnPayMinimum.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openPaymentDialog(card, "MINIMO");
                    }
                });

                LinearLayout btnPayFull = itemView.findViewById(R.id.btnPayFull);
                btnPayFull.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openPaymentDialog(card, "TOTAL");
                    }
                });

                LinearLayout btnPayOther = itemView.findViewById(R.id.btnPayOther);
                btnPayOther.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openPaymentDialog(card, "OTRO");
                    }
                });
            } else {
                layoutCardPaymentActions.setVisibility(GONE);
            }

            TextView tvBtnEditCardIcon = itemView.findViewById(R.id.tvBtnEditCardIcon);
            FaIcon.apply(tvBtnEditCardIcon, "fa-solid fa-pen");

            TextView tvBtnDeleteCardIcon = itemView.findViewById(R.id.tvBtnDeleteCardIcon);
            FaIcon.apply(tvBtnDeleteCardIcon, "fa-solid fa-trash-can");

            LinearLayout btnEditCard = itemView.findViewById(R.id.btnEditCard);
            btnEditCard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    CreditCardFormDialog.show(getContext(), card, new CreditCardFormDialog.OnCreditCardSavedListener() {
                        @Override
                        public void onSaved(CreditCard updated) {
                            host.refreshCreditCardsCache();
                            loadData();
                        }
                    });
                }
            });

            LinearLayout btnDeleteCard = itemView.findViewById(R.id.btnDeleteCard);
            btnDeleteCard.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmarEliminarTarjeta(card);
                }
            });

            creditCardsListContainer.addView(itemView);
        }
    }

    private void openPaymentDialog(final CreditCard card, String paymentType) {
        PaymentCardDialog.show(getContext(), card, paymentType, new PaymentCardDialog.OnPaymentRegisteredListener() {
            @Override
            public void onPaymentRegistered(JSONObject responseData) {
                host.refreshCreditCardsCache();
                loadData();
            }
        });
    }

    private void confirmarEliminarTarjeta(final CreditCard card) {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar tarjeta")
                .setMessage(getContext().getString(R.string.confirmar_eliminar_tarjeta))
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        host.setGlobalLoading(true);
                        ApiClient.getInstance().eliminarCreditCard(card.id, new ApiCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean data, PaginacionMeta meta) {
                                host.setGlobalLoading(false);
                                host.refreshCreditCardsCache();
                                loadData();
                            }

                            @Override
                            public void onError(ApiError error) {
                                host.setGlobalLoading(false);
                                Toast.makeText(getContext(), error.mensaje, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .show();
    }
}
