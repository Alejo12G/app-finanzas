package com.finanzas.app.util;

/**
 * Calculadora de amortizacion (sistema frances de cuota fija) e intereses para compras con tarjeta.
 * Replica con exactitud al centavo la logica de /api/v1/credit-cards/simulate y /api/v1/transactions.
 */
public final class CreditCalculator {

    public static final int MAX_INSTALLMENTS = 48;

    public static class SimulationResult {
        public final long principalCentavos;
        public final int installments;
        public final boolean isInterestExempt;
        public final double appliedInterestRate;
        public final long monthlyInstallmentCentavos;
        public final long totalInterestCentavos;
        public final long totalWithInterestCentavos;
        public final double monthlyInstallmentAmount;
        public final double totalInterestAmount;
        public final double totalWithInterest;

        public SimulationResult(
                long principalCentavos,
                int installments,
                boolean isInterestExempt,
                double appliedInterestRate,
                long monthlyInstallmentCentavos,
                long totalInterestCentavos,
                long totalWithInterestCentavos) {
            this.principalCentavos = principalCentavos;
            this.installments = installments;
            this.isInterestExempt = isInterestExempt;
            this.appliedInterestRate = appliedInterestRate;
            this.monthlyInstallmentCentavos = monthlyInstallmentCentavos;
            this.totalInterestCentavos = totalInterestCentavos;
            this.totalWithInterestCentavos = totalWithInterestCentavos;
            this.monthlyInstallmentAmount = CurrencyUtils.centavosToUnits(monthlyInstallmentCentavos);
            this.totalInterestAmount = CurrencyUtils.centavosToUnits(totalInterestCentavos);
            this.totalWithInterest = CurrencyUtils.centavosToUnits(totalWithInterestCentavos);
        }
    }

    private CreditCalculator() {
    }

    public static SimulationResult simulate(
            long principalCentavos,
            int installments,
            double ratePct,
            boolean isExempt) {

        long safePrincipal = Math.max(0L, principalCentavos);
        int n = Math.max(1, Math.min(MAX_INSTALLMENTS, installments));
        double effectiveRate = isExempt ? 0.0 : Math.max(0.0, ratePct);
        boolean noInterest = isExempt || !(effectiveRate > 0.0);

        if (safePrincipal == 0L) {
            return new SimulationResult(0L, n, isExempt, effectiveRate, 0L, 0L, 0L);
        }

        if (noInterest) {
            long installmentCents = Math.round((double) safePrincipal / (double) n);
            return new SimulationResult(
                    safePrincipal,
                    n,
                    isExempt,
                    0.0,
                    installmentCents,
                    0L,
                    safePrincipal
            );
        }

        double r = effectiveRate / 100.0;
        double denom = 1.0 - Math.pow(1.0 + r, -n);
        long installment = denom > 0.0
                ? Math.round((safePrincipal * r) / denom)
                : Math.round((double) safePrincipal / (double) n);

        long balance = safePrincipal;
        long totalInterest = 0L;
        for (int k = 1; k <= n; k++) {
            long interest = Math.round(balance * r);
            long principal = (k == n) ? balance : (installment - interest);
            balance -= principal;
            totalInterest += interest;
        }
        long totalWithInterest = safePrincipal + totalInterest;

        return new SimulationResult(
                safePrincipal,
                n,
                false,
                effectiveRate,
                installment,
                totalInterest,
                totalWithInterest
        );
    }
}
