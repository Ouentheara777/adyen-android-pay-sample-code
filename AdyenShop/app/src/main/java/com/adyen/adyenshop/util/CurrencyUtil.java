package com.adyen.adyenshop.util;

/**
 * Created by andrei on 11/16/16.
 */

public class CurrencyUtil {

    public static String getCurrencySymbol(String currency) {
        if(currency.equals(CurrenciesEnum.USD.toString())) {
            return CurrenciesEnum.USD.getSymbol();
        }
        if(currency.equals(CurrenciesEnum.EUR.toString())) {
            return CurrenciesEnum.EUR.getSymbol();
        }
        if(currency.equals(CurrenciesEnum.GBP.toString())) {
            return CurrenciesEnum.GBP.getSymbol();
        }
        if(currency.equals(CurrenciesEnum.BRL.toString())) {
            return CurrenciesEnum.BRL.getSymbol();
        }
        return CurrenciesEnum.USD.getSymbol();
    }

}
