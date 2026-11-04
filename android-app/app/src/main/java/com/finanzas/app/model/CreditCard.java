package com.finanzas.app.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de Tarjeta de Credito segun contrato JSON de /api/v1/credit-cards (API_CONTRACT.md Seccion 11.1).
 * Campos exactos: id, name, bank, credit_limit, available_limit, default_interest_rate, cutoff_day, payment_day.
 */
public class CreditCard {
    public int id;
    public String name;
    public String bank;
    public double credit_limit;
    public double available_limit;
    public double default_interest_rate;
    public int cutoff_day;
    public int payment_day;

    public static CreditCard fromJson(JSONObject obj) {
        if (obj == null) {
            return null;
        }
        CreditCard card = new CreditCard();
        card.id = obj.optInt("id", 0);
        card.name = obj.optString("name", "");
        card.bank = obj.optString("bank", "");
        card.credit_limit = obj.optDouble("credit_limit", 0.0);
        card.available_limit = obj.has("available_limit") && !obj.isNull("available_limit")
                ? obj.optDouble("available_limit", card.credit_limit)
                : card.credit_limit;
        card.default_interest_rate = obj.optDouble("default_interest_rate", 0.0);
        card.cutoff_day = obj.optInt("cutoff_day", 1);
        card.payment_day = obj.optInt("payment_day", 1);
        return card;
    }

    public static List<CreditCard> fromJsonArray(JSONArray arr) {
        List<CreditCard> list = new ArrayList<>();
        if (arr == null) {
            return list;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item != null) {
                list.add(fromJson(item));
            }
        }
        return list;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", name != null ? name.trim() : "");
        obj.put("bank", bank != null ? bank.trim() : "");
        obj.put("credit_limit", credit_limit);
        obj.put("default_interest_rate", default_interest_rate);
        obj.put("cutoff_day", cutoff_day);
        obj.put("payment_day", payment_day);
        return obj;
    }

    public double getUsedLimit() {
        double used = credit_limit - available_limit;
        return used > 0.0 ? used : 0.0;
    }

    public int getUsagePercentage() {
        if (credit_limit <= 0.0) {
            return 0;
        }
        double pct = (getUsedLimit() / credit_limit) * 100.0;
        if (pct < 0.0) return 0;
        if (pct > 100.0) return 100;
        return (int) Math.round(pct);
    }

    @Override
    public String toString() {
        if (bank != null && !bank.trim().isEmpty()) {
            return name + " (" + bank + ")";
        }
        return name != null ? name : "";
    }
}
