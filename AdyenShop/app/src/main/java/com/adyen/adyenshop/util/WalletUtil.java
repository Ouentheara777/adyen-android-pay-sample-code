package com.adyen.adyenshop.util;

import android.content.Context;

import com.adyen.adyenshop.R;
import com.adyen.adyenshop.model.Product;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentMethodTokenizationType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrei on 3/9/16.
 */
public class WalletUtil {

    /**
     * Creates a MaskedWalletRequest for direct merchant integration (no payment processor)
     *
     * @param product {@link Product} containing details
     *                 of an item.
     * @param publicKey base64-encoded public encryption key. See instructions for more details.
     * @return {@link MaskedWalletRequest} instance
     */
    public static MaskedWalletRequest createMaskedWalletRequest(List<Product> product,
                                                                String orderTotal,
                                                                String publicKey,
                                                                Context context) {
        // Validate the public key
        if (publicKey == null || publicKey.contains("REPLACE_ME")) {
            throw new IllegalArgumentException("Invalid public key, see README for instructions.");
        }

        // Create direct integration parameters
        // [START direct_integration_parameters]
        PaymentMethodTokenizationParameters parameters =
                PaymentMethodTokenizationParameters.newBuilder()
                        .setPaymentMethodTokenizationType(PaymentMethodTokenizationType.NETWORK_TOKEN)
                        .addParameter("publicKey", publicKey)
                        .build();
        // [END direct_integration_parameters]

        return createMaskedWalletRequest(product, orderTotal, parameters, context);
    }

    private static MaskedWalletRequest createMaskedWalletRequest(List<Product> product,
                                                                 String orderTotal,
                                                                 PaymentMethodTokenizationParameters parameters,
                                                                 Context context) {
        // Build a List of all line items
        List<LineItem> lineItems = buildLineItems(product, true, context);

        // [START masked_wallet_request]
        MaskedWalletRequest request = MaskedWalletRequest.newBuilder()
                .setMerchantName(Constants.MERCHANT_NAME)
                .setPhoneNumberRequired(true)
                .setShippingAddressRequired(true)
                .setCurrencyCode(PreferencesUtil.getDefaultSharedPreferences(context).getString(context.getString(R.string.active_currency), "USD"))
                .setEstimatedTotalPrice(orderTotal)
                // Create a Cart with the current line items. Provide all the information
                // available up to this point with estimates for shipping and tax included.
                .setCart(Cart.newBuilder()
                        .setCurrencyCode(PreferencesUtil.getDefaultSharedPreferences(context).getString(context.getString(R.string.active_currency), "USD"))
                        .setTotalPrice(orderTotal)
                        .setLineItems(lineItems)
                        .build())
                .setPaymentMethodTokenizationParameters(parameters)
                .build();

        return request;
        // [END masked_wallet_request]
    }

    /**
     *
     * @param products {@link Product} to use for creating
     *                 the {@link com.google.android.gms.wallet.FullWalletRequest}
     * @param googleTransactionId
     * @return {@link FullWalletRequest} instance
     */
    public static FullWalletRequest createFullWalletRequest(List<Product> products,
                                                            String orderTotal,
                                                            String googleTransactionId,
                                                            Context context) {

        List<LineItem> lineItems = buildLineItems(products, false, context);

        // [START full_wallet_request]
        FullWalletRequest request = FullWalletRequest.newBuilder()
                .setGoogleTransactionId(googleTransactionId)
                .setCart(Cart.newBuilder()
                        .setCurrencyCode(PreferencesUtil.getDefaultSharedPreferences(context).getString(context.getString(R.string.active_currency), "USD"))
                        .setTotalPrice(orderTotal)
                        .setLineItems(lineItems)
                        .build())
                .build();
        // [END full_wallet_request]

        return request;
    }

    /**
     * Build a list of line items based on the {@link Product} and a boolean that indicates
     * whether to use estimated values of tax and shipping for setting up the
     * {@link MaskedWalletRequest} or actual values in the case of a {@link FullWalletRequest}
     *
     * @param products {@link Product} used for building the
     *                 {@link com.google.android.gms.wallet.LineItem} list.
     * @param isEstimate {@code boolean} that indicates whether to use estimated values for
     *                   shipping and tax values.
     * @return list of line items
     */
    private static List<LineItem> buildLineItems(List<Product> products, boolean isEstimate, Context context) {
        List<LineItem> list = new ArrayList<LineItem>();

        for (Product product : products) {
            String itemPrice = toDollars((long) product.getPrice());

            list.add(LineItem.newBuilder()
                    .setCurrencyCode(PreferencesUtil.getDefaultSharedPreferences(context).getString(context.getString(R.string.active_currency), "USD"))
                    .setDescription(product.getName())
                    .setQuantity("1")
                    .setUnitPrice(itemPrice)
                    .setTotalPrice(itemPrice)
                    .build());
        }

        String shippingPrice = toDollars((long) 0.11);

        list.add(LineItem.newBuilder()
                .setCurrencyCode(PreferencesUtil.getDefaultSharedPreferences(context).getString(context.getString(R.string.active_currency), "USD"))
                .setDescription(Constants.DESCRIPTION_LINE_ITEM_SHIPPING)
                .setRole(LineItem.Role.SHIPPING)
                .setTotalPrice(shippingPrice)
                .build());

        String tax = toDollars((long) 0.11);

        list.add(LineItem.newBuilder()
                .setCurrencyCode(PreferencesUtil.getDefaultSharedPreferences(context).getString(context.getString(R.string.active_currency), "USD"))
                .setDescription(Constants.DESCRIPTION_LINE_ITEM_TAX)
                .setRole(LineItem.Role.TAX)
                .setTotalPrice(tax)
                .build());

        return list;
    }

    /**
     * @param micros Amount micros
     * @return string formatted as "0.00" required by the Instant Buy API.
     */
    private static String toDollars(long micros) {
        return new BigDecimal(micros).setScale(2, RoundingMode.HALF_EVEN).toString();
    }

}
