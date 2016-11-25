package com.adyen.adyenshop.util;

import com.google.android.gms.wallet.WalletConstants;

/**
 * Created by andrei on 3/9/16.
 */
public class Constants {

    // Environment to use when creating an instance of Wallet.WalletOptions
    public static final int WALLET_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST;
    //public static final int WALLET_ENVIRONMENT = WalletConstants.ENVIRONMENT_PRODUCTION;

    public static final String CURRENCY_CODE_USD = "USD";

    // values to use with KEY_DESCRIPTION
    public static final String DESCRIPTION_LINE_ITEM_SHIPPING = "Shipping";
    public static final String DESCRIPTION_LINE_ITEM_TAX = "Tax";

    public static final String MERCHANT_NAME = "Adyen Shop";

    public static final String EXTRA_MASKED_WALLET = "EXTRA_MASKED_WALLET";

}
