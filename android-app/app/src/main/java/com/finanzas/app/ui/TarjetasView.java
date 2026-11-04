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
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.UiHelper;

import java.util.List;
import java.util.Locale;

/**
 * Vista de Gestion de Tarjetas de Credito (/api/v1/credit-cards).
 * Muestra en tarjetas visuales sobrias el cupo disponible, fechas de corte/pago
 * y la tasa de interes mensual por defecto de cada plastico.
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

            TextView tvCutoffIcon = itemView.findViewById(R.id.tvCutoffIcon);
            FaIcon.apply(tvCutoffIcon, "fa-solid fa-calendar-days");

            TextView tvCardCutoffDay = itemView.findViewById(R.id.tvCardCutoffDay);
            tvCardCutoffDay.setText("Fecha de corte: Dia " + card.cutoff_day);

            TextView tvPaymentIcon = itemView.findViewById(R.id.tvPaymentIcon);
            FaIcon.apply(tvPaymentIcon, "fa-solid fa-calendar-days");

            TextView tvCardPaymentDay = itemView.findViewById(R.id.tvCardPaymentDay);
            tvCardPaymentDay.setText("Fecha de pago: Dia " + card.payment_day);

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
