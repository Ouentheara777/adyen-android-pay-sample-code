package com.adyen.adyenshop.util;

/**
 * Created by andrei on 11/16/16.
 */

public enum CurrenciesEnum {

    USD("$"),
    EUR("€"),
    GBP("£"),
    BRL("R$");

    private String symbol;

    CurrenciesEnum(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
