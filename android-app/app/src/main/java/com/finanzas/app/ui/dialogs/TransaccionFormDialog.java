package com.finanzas.app.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.finanzas.app.MainActivity;
import com.finanzas.app.R;
import com.finanzas.app.api.ApiCallback;
import com.finanzas.app.api.ApiClient;
import com.finanzas.app.api.ApiError;
import com.finanzas.app.model.Categoria;
import com.finanzas.app.model.CreditCard;
import com.finanzas.app.model.PaginacionMeta;
import com.finanzas.app.model.Transaccion;
import com.finanzas.app.util.CreditCalculator;
import com.finanzas.app.util.CurrencyUtils;
import com.finanzas.app.util.DateUtils;
import com.finanzas.app.util.FaIcon;
import com.finanzas.app.util.QuickExpenseParser;
import com.finanzas.app.util.UiHelper;
import com.finanzas.app.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dialogo de alta y edicion de transacciones (POST /api/v1/transactions y PUT /api/v1/transactions/{id}).
 * Incluye seccion dinamica de financiacion con tarjeta de credito (CREDIT_CARD),
 * autocompletado de tasa por tarjeta, opcion de compra exenta de intereses (0%)
 * y calculo en vivo de cuota mensual, intereses totales y costo total.
 */
public class TransaccionFormDialog {

    public interface OnSavedListener {
        void onSaved(Transaccion transaccion);
    }

    private static final String[] TIPOS = {"GASTO", "INGRESO"};
    private static final String[] NATURALEZAS = {"VARIABLE", "FIJO"};
    private static final String[] METODOS_PAGO = {"EFECTIVO", "DEBITO", "CREDIT_CARD", "TRANSFERENCIA", "OTRO"};

    private static boolean isCreditCardMethod(String method) {
        return "CREDIT_CARD".equalsIgnoreCase(method) || "CREDITO".equalsIgnoreCase(method);
    }

    public static void show(
            final Context context,
            final Transaccion existente,
            final QuickExpenseParser.ParsedExpense prefill,
            final List<Categoria> todasCategorias,
            final OnSavedListener listener) {

        final boolean esEdicion = (existente != null && existente.id > 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View rootView = LayoutInflater.from(context).inflate(R.layout.dialog_transaction_form, null, false);

        TextView titleTv = rootView.findViewById(R.id.tvTxDialogTitle);
        titleTv.setText(esEdicion ? "Editar transaccion" : (prefill != null ? "Confirmar transaccion rapida IA" : "Nueva transaccion"));

        final TextView globalErrorTv = rootView.findViewById(R.id.tvTxGlobalError);

        final Spinner spTipo = rootView.findViewById(R.id.spTxTipo);
        spTipo.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, TIPOS));

        final Spinner spNaturaleza = rootView.findViewById(R.id.spTxNaturaleza);
        spNaturaleza.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, NATURALEZAS));

        TextView tvLabelMonto = rootView.findViewById(R.id.tvLabelTxMonto);
        tvLabelMonto.setText("Monto (" + CurrencyUtils.getActiveCurrency() + ") *");

        final EditText etMonto = rootView.findViewById(R.id.etTxMonto);
        final TextView errMonto = rootView.findViewById(R.id.errTxMonto);

        final EditText etFecha = rootView.findViewById(R.id.etTxFecha);
        final TextView errFecha = rootView.findViewById(R.id.errTxFecha);
        LinearLayout btnPickDate = rootView.findViewById(R.id.btnTxPickDate);
        TextView tvBtnPickDateIcon = rootView.findViewById(R.id.tvBtnTxPickDateIcon);
        FaIcon.apply(tvBtnPickDateIcon, "fa-solid fa-calendar-days");
        btnPickDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DateUtils.showDatePicker(context, etFecha);
            }
        });

        final EditText etDescripcion = rootView.findViewById(R.id.etTxDescripcion);
        final TextView errDescripcion = rootView.findViewById(R.id.errTxDescripcion);

        final Spinner spCategoria = rootView.findViewById(R.id.spTxCategoria);
        final TextView errCategoria = rootView.findViewById(R.id.errTxCategoria);

        final Spinner spMetodo = rootView.findViewById(R.id.spTxMetodoPago);
        spMetodo.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, METODOS_PAGO));

        // Seccion de financiacion con tarjeta de credito
        final LinearLayout layoutCreditCardFinancing = rootView.findViewById(R.id.layoutCreditCardFinancing);
        TextView tvFinancingHeaderIcon = rootView.findViewById(R.id.tvFinancingHeaderIcon);
        FaIcon.apply(tvFinancingHeaderIcon, "fa-solid fa-credit-card");

        final Spinner spCreditCard = rootView.findViewById(R.id.spCreditCard);
        final TextView tvSelectedCardAvailableInfo = rootView.findViewById(R.id.tvSelectedCardAvailableInfo);
        final TextView errCreditCard = rootView.findViewById(R.id.errCreditCard);

        final EditText etInstallments = rootView.findViewById(R.id.etInstallments);
        final TextView errInstallments = rootView.findViewById(R.id.errInstallments);

        final CheckBox cbInterestExempt = rootView.findViewById(R.id.cbInterestExempt);

        final EditText etAppliedInterestRate = rootView.findViewById(R.id.etAppliedInterestRate);
        final TextView errAppliedInterestRate = rootView.findViewById(R.id.errAppliedInterestRate);

        final TextView tvSummaryMonthlyInstallment = rootView.findViewById(R.id.tvSummaryMonthlyInstallment);
        final TextView tvSummaryTotalInterest = rootView.findViewById(R.id.tvSummaryTotalInterest);
        final TextView tvSummaryTotalWithInterest = rootView.findViewById(R.id.tvSummaryTotalWithInterest);
        UiHelper.applyTabularNums(tvSummaryMonthlyInstallment);
        UiHelper.applyTabularNums(tvSummaryTotalInterest);
        UiHelper.applyTabularNums(tvSummaryTotalWithInterest);

        final EditText etComercio = rootView.findViewById(R.id.etTxComercio);
        final TextView errComercio = rootView.findViewById(R.id.errTxComercio);

        final EditText etNotas = rootView.findViewById(R.id.etTxNotas);
        final TextView errNotas = rootView.findViewById(R.id.errTxNotas);

        TextView tvBtnCancelarIcon = rootView.findViewById(R.id.tvBtnTxCancelarIcon);
        FaIcon.apply(tvBtnCancelarIcon, "fa-solid fa-xmark");

        TextView tvBtnGuardarIcon = rootView.findViewById(R.id.tvBtnTxGuardarIcon);
        FaIcon.apply(tvBtnGuardarIcon, "fa-solid fa-floppy-disk");

        LinearLayout btnCancelar = rootView.findViewById(R.id.btnTxCancelar);
        final LinearLayout btnGuardar = rootView.findViewById(R.id.btnTxGuardar);

        final List<CreditCard> availableCards = new ArrayList<>();
        if (context instanceof MainActivity) {
            List<CreditCard> cached = ((MainActivity) context).getCachedCreditCards();
            if (cached != null) {
                availableCards.addAll(cached);
            }
        }

        // Bandera para preservar la tasa guardada al abrir la edicion inicial
        final boolean[] preserveInitialEditRate = {
                esEdicion && existente.isCreditCardTransaction() && existente.appliedInterestRate != null
        };

        // Calculo en vivo de cuota mensual, total de intereses y costo total
        final Runnable recalcularResumenFinanciacion = new Runnable() {
            @Override
            public void run() {
                long montoCentavos = CurrencyUtils.parseMontoToCentavos(etMonto.getText().toString());
                if (montoCentavos < 0L) {
                    montoCentavos = 0L;
                }

                int cuotas = 1;
                try {
                    cuotas = Integer.parseInt(etInstallments.getText().toString().trim());
                } catch (Exception ignored) {
                }
                if (cuotas < 1) {
                    cuotas = 1;
                } else if (cuotas > CreditCalculator.MAX_INSTALLMENTS) {
                    cuotas = CreditCalculator.MAX_INSTALLMENTS;
                }

                boolean exempt = cbInterestExempt.isChecked();
                double rate = 0.0;
                if (!exempt) {
                    try {
                        rate = Double.parseDouble(etAppliedInterestRate.getText().toString().trim().replace(",", "."));
                    } catch (Exception ignored) {
                    }
                    if (Double.isNaN(rate) || Double.isInfinite(rate) || rate < 0.0) {
                        rate = 0.0;
                    }
                }

                CreditCalculator.SimulationResult sim = CreditCalculator.simulate(montoCentavos, cuotas, rate, exempt);
                tvSummaryMonthlyInstallment.setText(CurrencyUtils.formatConMoneda(sim.monthlyInstallmentCentavos));
                tvSummaryTotalInterest.setText(CurrencyUtils.formatConMoneda(sim.totalInterestCentavos));
                tvSummaryTotalWithInterest.setText(CurrencyUtils.formatConMoneda(sim.totalWithInterestCentavos));
            }
        };

        final Runnable populateCardsSpinner = new Runnable() {
            @Override
            public void run() {
                ArrayAdapter<CreditCard> cardAdapter = new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_spinner_dropdown_item,
                        availableCards
                );
                spCreditCard.setAdapter(cardAdapter);

                if (availableCards.isEmpty()) {
                    tvSelectedCardAvailableInfo.setText("No hay tarjetas registradas. Cree una en la seccion Tarjetas.");
                    return;
                }

                int selectedIdx = 0;
                if (esEdicion && existente.cardId != null) {
                    for (int i = 0; i < availableCards.size(); i++) {
                        if (availableCards.get(i).id == existente.cardId) {
                            selectedIdx = i;
                            break;
                        }
                    }
                }
                spCreditCard.setSelection(selectedIdx);
            }
        };

        spCreditCard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < availableCards.size()) {
                    CreditCard selectedCard = availableCards.get(position);
                    tvSelectedCardAvailableInfo.setText(String.format(
                            Locale.US,
                            "Disponible: %s | Corte: dia %d | Pago: dia %d",
                            CurrencyUtils.formatUnitsConMoneda(selectedCard.available_limit),
                            selectedCard.cutoff_day,
                            selectedCard.payment_day
                    ));

                    if (cbInterestExempt.isChecked()) {
                        etAppliedInterestRate.setText("0.0");
                        etAppliedInterestRate.setEnabled(false);
                    } else if (preserveInitialEditRate[0]) {
                        preserveInitialEditRate[0] = false;
                        etAppliedInterestRate.setEnabled(true);
                        etAppliedInterestRate.setText(CurrencyUtils.formatTasaInput(existente.appliedInterestRate));
                    } else {
                        etAppliedInterestRate.setEnabled(true);
                        etAppliedInterestRate.setText(CurrencyUtils.formatTasaInput(selectedCard.default_interest_rate));
                    }
                    recalcularResumenFinanciacion.run();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        cbInterestExempt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    etAppliedInterestRate.setText("0.0");
                    etAppliedInterestRate.setEnabled(false);
                    UiHelper.setFieldError(etAppliedInterestRate, errAppliedInterestRate, null);
                } else {
                    etAppliedInterestRate.setEnabled(true);
                    int cardPos = spCreditCard.getSelectedItemPosition();
                    if (cardPos >= 0 && cardPos < availableCards.size()) {
                        CreditCard card = availableCards.get(cardPos);
                        etAppliedInterestRate.setText(CurrencyUtils.formatTasaInput(card.default_interest_rate));
                    }
                }
                recalcularResumenFinanciacion.run();
            }
        });

        TextWatcher liveWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                recalcularResumenFinanciacion.run();
            }
        };
        etMonto.addTextChangedListener(liveWatcher);
        etInstallments.addTextChangedListener(liveWatcher);
        etAppliedInterestRate.addTextChangedListener(liveWatcher);

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
                if ("INGRESO".equals(TIPOS[position]) && isCreditCardMethod(METODOS_PAGO[spMetodo.getSelectedItemPosition()])) {
                    spMetodo.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spMetodo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String metodo = METODOS_PAGO[position];
                if (isCreditCardMethod(metodo)) {
                    if ("INGRESO".equals(TIPOS[spTipo.getSelectedItemPosition()])) {
                        spTipo.setSelection(0);
                    }
                    layoutCreditCardFinancing.setVisibility(View.VISIBLE);
                    recalcularResumenFinanciacion.run();
                } else {
                    layoutCreditCardFinancing.setVisibility(View.GONE);
                }
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

            String rawMethod = existente.isCreditCardTransaction() ? "CREDIT_CARD" : existente.metodoPago;
            for (int i = 0; i < METODOS_PAGO.length; i++) {
                if (METODOS_PAGO[i].equalsIgnoreCase(rawMethod)) {
                    spMetodo.setSelection(i);
                    break;
                }
            }

            if (existente.isCreditCardTransaction()) {
                layoutCreditCardFinancing.setVisibility(View.VISIBLE);
                int inst = (existente.installments != null && existente.installments >= 1) ? existente.installments : 1;
                etInstallments.setText(String.valueOf(inst));
                boolean exempt = existente.isInterestExempt != null && existente.isInterestExempt;
                cbInterestExempt.setChecked(exempt);
                if (exempt) {
                    etAppliedInterestRate.setText("0.0");
                    etAppliedInterestRate.setEnabled(false);
                } else if (existente.appliedInterestRate != null) {
                    etAppliedInterestRate.setEnabled(true);
                    etAppliedInterestRate.setText(CurrencyUtils.formatTasaInput(existente.appliedInterestRate));
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
            String rawMethod = isCreditCardMethod(prefill.metodoPago) ? "CREDIT_CARD" : prefill.metodoPago;
            for (int i = 0; i < METODOS_PAGO.length; i++) {
                if (METODOS_PAGO[i].equalsIgnoreCase(rawMethod)) {
                    spMetodo.setSelection(i);
                    break;
                }
            }
            if (isCreditCardMethod(rawMethod)) {
                layoutCreditCardFinancing.setVisibility(View.VISIBLE);
            }
            etComercio.setText(prefill.comercio != null ? prefill.comercio : "");
            etNotas.setText(prefill.notas != null ? prefill.notas : "");
        } else {
            etFecha.setText(DateUtils.getTodayIso());
        }

        actualizarSpinnerCategorias.run();
        populateCardsSpinner.run();
        recalcularResumenFinanciacion.run();

        // Actualizar listado de tarjetas desde /api/v1/credit-cards
        ApiClient.getInstance().getCreditCards(new ApiCallback<List<CreditCard>>() {
            @Override
            public void onSuccess(List<CreditCard> data, PaginacionMeta meta) {
                if (data != null) {
                    availableCards.clear();
                    availableCards.addAll(data);
                    populateCardsSpinner.run();
                }
            }

            @Override
            public void onError(ApiError error) {
            }
        });

        builder.setView(rootView);
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
                UiHelper.setFieldError(etMonto, errMonto, null);
                UiHelper.setFieldError(etFecha, errFecha, null);
                UiHelper.setFieldError(etDescripcion, errDescripcion, null);
                UiHelper.setFieldError(null, errCategoria, null);
                UiHelper.setFieldError(null, errCreditCard, null);
                UiHelper.setFieldError(etInstallments, errInstallments, null);
                UiHelper.setFieldError(etAppliedInterestRate, errAppliedInterestRate, null);
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

                Integer cardId = null;
                Integer installments = null;
                Boolean isInterestExempt = null;
                Double appliedInterestRate = null;
                Double monthlyInstallmentAmount = null;
                Double totalInterestAmount = null;
                Double totalWithInterest = null;

                if (isCreditCardMethod(metodo)) {
                    if (!"GASTO".equals(tipo)) {
                        UiHelper.setFieldError(null, globalErrorTv, "CREDIT_CARD solo aplica a transacciones de tipo GASTO.");
                        valid = false;
                    }
                    int cardPos = spCreditCard.getSelectedItemPosition();
                    if (availableCards.isEmpty() || cardPos < 0 || cardPos >= availableCards.size()) {
                        UiHelper.setFieldError(null, errCreditCard, "Seleccione una tarjeta de credito registrada.");
                        valid = false;
                    } else {
                        cardId = availableCards.get(cardPos).id;
                    }

                    int parsedInstallments = -1;
                    try {
                        parsedInstallments = Integer.parseInt(etInstallments.getText().toString().trim());
                    } catch (Exception ignored) {
                    }
                    if (parsedInstallments < 1 || parsedInstallments > CreditCalculator.MAX_INSTALLMENTS) {
                        UiHelper.setFieldError(
                                etInstallments,
                                errInstallments,
                                "El numero de cuotas debe ser un entero entre 1 y " + CreditCalculator.MAX_INSTALLMENTS + "."
                        );
                        valid = false;
                    } else {
                        installments = parsedInstallments;
                    }

                    boolean exempt = cbInterestExempt.isChecked();
                    isInterestExempt = exempt;

                    if (exempt) {
                        appliedInterestRate = 0.0;
                    } else {
                        double parsedRate = -1.0;
                        try {
                            parsedRate = Double.parseDouble(etAppliedInterestRate.getText().toString().trim().replace(",", "."));
                        } catch (Exception ignored) {
                        }
                        if (Double.isNaN(parsedRate) || Double.isInfinite(parsedRate) || parsedRate < 0.0 || parsedRate > 100.0) {
                            UiHelper.setFieldError(etAppliedInterestRate, errAppliedInterestRate, "Ingrese una tasa mensual entre 0 y 100.");
                            valid = false;
                        } else {
                            appliedInterestRate = parsedRate;
                        }
                    }

                    if (valid && installments != null && appliedInterestRate != null) {
                        CreditCalculator.SimulationResult sim = CreditCalculator.simulate(
                                montoCentavos,
                                installments,
                                appliedInterestRate,
                                isInterestExempt
                        );
                        monthlyInstallmentAmount = sim.monthlyInstallmentAmount;
                        totalInterestAmount = sim.totalInterestAmount;
                        totalWithInterest = sim.totalWithInterest;
                    }
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
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).refreshCreditCardsCache();
                        }
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
                        if (fieldMap.containsKey("card_id")) {
                            UiHelper.setFieldError(null, errCreditCard, fieldMap.get("card_id"));
                        }
                        if (fieldMap.containsKey("installments")) {
                            UiHelper.setFieldError(etInstallments, errInstallments, fieldMap.get("installments"));
                        }
                        if (fieldMap.containsKey("applied_interest_rate")) {
                            UiHelper.setFieldError(etAppliedInterestRate, errAppliedInterestRate, fieldMap.get("applied_interest_rate"));
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
                            existente.id,
                            tipo,
                            naturaleza,
                            montoCentavos,
                            fecha,
                            desc,
                            categoriaId,
                            metodo,
                            comercio,
                            notas,
                            cardId,
                            installments,
                            isInterestExempt,
                            appliedInterestRate,
                            monthlyInstallmentAmount,
                            totalInterestAmount,
                            totalWithInterest,
                            cb
                    );
                } else {
                    ApiClient.getInstance().crearTransaccion(
                            tipo,
                            naturaleza,
                            montoCentavos,
                            fecha,
                            desc,
                            categoriaId,
                            metodo,
                            comercio,
                            notas,
                            cardId,
                            installments,
                            isInterestExempt,
                            appliedInterestRate,
                            monthlyInstallmentAmount,
                            totalInterestAmount,
                            totalWithInterest,
                            cb
                    );
                }
            }
        });

        dialog.show();
    }
}
