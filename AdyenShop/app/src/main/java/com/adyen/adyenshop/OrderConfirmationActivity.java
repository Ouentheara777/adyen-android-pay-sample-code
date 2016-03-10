package com.adyen.adyenshop;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.adyen.adyenshop.adapter.OrderItemsAdapter;
import com.adyen.adyenshop.model.Product;
import com.adyen.adyenshop.util.Constants;
import com.adyen.adyenshop.util.WalletUtil;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrei on 3/9/16.
 */
public class OrderConfirmationActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{

    private static final String tag = OrderConfirmationActivity.class.getSimpleName();
    private static final int REQUEST_CODE_MASKED_WALLET = 1001;
    public static final int REQUEST_CODE_RESOLVE_LOAD_FULL_WALLET = 1004;

    private OrderItemsAdapter orderItemsAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;
    private ProgressDialog mProgressDialog;
    private TextView totalPriceTextView;
    private TextView shippingTextView;
    private TextView taxTextView;
    private TextView orderTotalPriceTextView;

    private GoogleApiClient mGoogleApiClient;
    private SupportWalletFragment mWalletFragment;
    private MaskedWallet mMaskedWallet;

    private List<Product> productsList;
    private float cartTotal;
    private float orderTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_confirmation);
        Intent intent = getIntent();

        totalPriceTextView = (TextView)findViewById(R.id.total_price);
        shippingTextView = (TextView)findViewById(R.id.shipping_price);
        taxTextView = (TextView)findViewById(R.id.tax_price);
        orderTotalPriceTextView = (TextView)findViewById(R.id.order_total_price);

        mRecyclerView = (RecyclerView) findViewById(R.id.order_list);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        productsList = fillProductListFromIntent(intent);
        orderItemsAdapter = new OrderItemsAdapter(productsList);
        mRecyclerView.setAdapter(orderItemsAdapter);

        cartTotal = getIntent().getFloatExtra("totalPrice", 0);

        // [START basic_google_api_client]
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder().build())
                .enableAutoManage(this, this)
                .build();
        // [END basic_google_api_client]

        validateAndroidPay();
        fillOrderAmounts();
    }

    public void fillOrderAmounts() {
        totalPriceTextView.setText("$ " + String.valueOf(String.format("%.02f", cartTotal)));
        shippingTextView.setText("$ 0.12");
        taxTextView.setText("$ 0.07");
        orderTotal = (float)(cartTotal + 0.12 + 0.07);
        orderTotalPriceTextView.setText("$ " + String.valueOf(String.format("%.02f", orderTotal)));
    }

    public List<Product> fillProductListFromIntent(Intent intent) {
        List<Product> products = new ArrayList<>();
        Parcelable[] productsInCart = intent.getParcelableArrayExtra("itemsInCart");
        for(int i=0; i<productsInCart.length; i++) {
            products.add((Product)productsInCart[i]);
        }
        return products;
    }

    private void validateAndroidPay() {
        showProgressDialog();
        Wallet.Payments.isReadyToPay(mGoogleApiClient).setResultCallback(
                new ResultCallback<BooleanResult>() {
                    @Override
                    public void onResult(@NonNull BooleanResult booleanResult) {
                        hideProgressDialog();

                        if (booleanResult.getStatus().isSuccess()) {
                            if (booleanResult.getValue()) {
                                // Show Android Pay buttons and hide regular checkout button
                                Log.d(tag, "isReadyToPay:true");
                                createAndAddWalletFragment();
                                findViewById(R.id.button_regular_checkout).setVisibility(View.GONE);
                            } else {
                                // Hide Android Pay buttons, show a message that Android Pay
                                // cannot be used yet, and display a traditional checkout button
                                Log.d(tag, "isReadyToPay:false:" + booleanResult.getStatus());
                                findViewById(R.id.checkout_fragment_container).setVisibility(View.GONE);
                                findViewById(R.id.android_pay_message).setVisibility(View.VISIBLE);
                                findViewById(R.id.button_regular_checkout).setVisibility(View.VISIBLE);
                            }
                        } else {
                            // Error making isReadyToPay call
                            Log.e(tag, "isReadyToPay:" + booleanResult.getStatus());
                        }
                    }
                }
        );
    }

    private void createAndAddWalletFragment() {
        // [START fragment_style_and_options]
        WalletFragmentStyle walletFragmentStyle = new WalletFragmentStyle()
                .setBuyButtonText(WalletFragmentStyle.BuyButtonText.BUY_WITH)
                .setBuyButtonAppearance(WalletFragmentStyle.BuyButtonAppearance.ANDROID_PAY_DARK)
                .setBuyButtonWidth(WalletFragmentStyle.Dimension.MATCH_PARENT);

        WalletFragmentOptions walletFragmentOptions = WalletFragmentOptions.newBuilder()
                .setEnvironment(Constants.WALLET_ENVIRONMENT)
                .setFragmentStyle(walletFragmentStyle)
                .setTheme(WalletConstants.THEME_LIGHT)
                .setMode(WalletFragmentMode.BUY_BUTTON)
                .build();
        mWalletFragment = SupportWalletFragment.newInstance(walletFragmentOptions);
        // [END fragment_style_and_options]

        // Now initialize the Wallet Fragment
        //String accountName = ((AdyenShopApplication)getApplication()).getAccountName();
        MaskedWalletRequest maskedWalletRequest;

        // Direct integration
        maskedWalletRequest = WalletUtil.createMaskedWalletRequest(
                productsList,
                String.valueOf(orderTotal),
                getString(R.string.public_key));

        // [START params_builder]
        WalletFragmentInitParams.Builder startParamsBuilder = WalletFragmentInitParams.newBuilder()
                .setMaskedWalletRequest(maskedWalletRequest)
                .setMaskedWalletRequestCode(REQUEST_CODE_MASKED_WALLET)
                .setAccountName(null);
        mWalletFragment.initialize(startParamsBuilder.build());

        // add Wallet fragment to the UI
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.checkout_fragment_container, mWalletFragment)
                .commit();
        // [END params_builder]
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(tag, "onConnectionFailed:" + connectionResult.getErrorMessage());
        Toast.makeText(this, "Google Play Services error", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // retrieve the error code, if available
        int errorCode = -1;
        if (data != null) {
            errorCode = data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, -1);
        }
        switch (requestCode) {
            case REQUEST_CODE_MASKED_WALLET:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null) {
                            mMaskedWallet = data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
                            confirmPurchase();
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                    default:
                        handleError(errorCode);
                        break;
                }
                break;
            case REQUEST_CODE_RESOLVE_LOAD_FULL_WALLET:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        if (data != null && data.hasExtra(WalletConstants.EXTRA_FULL_WALLET)) {
                            FullWallet fullWallet = data.getParcelableExtra(WalletConstants.EXTRA_FULL_WALLET);
                            // the full wallet can now be used to process the customer's payment
                            // send the wallet info up to server to process, and to get the result
                            // for sending a transaction status
                            /*
                            * Building the object to be sent to the merchant server
                            * */
                            final JSONObject paymentData = new JSONObject();
                            JSONObject additionalData = new JSONObject();
                            try {
                                additionalData.put("token", fullWallet.getPaymentMethodToken().getToken());
                                paymentData.put("additionalData", additionalData);

                                JSONObject amount = new JSONObject();
                                amount.put("currency", "USD");
                                amount.put("amount", String.valueOf(cartTotal));
                                paymentData.put("amount", amount);

                                paymentData.put("merchantReference", "AdyenShop");
                                paymentData.put("shopperEmail", fullWallet.getEmail());

                                JSONObject deliveryAddress = new JSONObject();
                                deliveryAddress.put("street", fullWallet.getBuyerShippingAddress().getAddress1());
                                deliveryAddress.put("houseNoOrName", fullWallet.getBuyerShippingAddress().getAddress2());
                                deliveryAddress.put("city", fullWallet.getBuyerShippingAddress().getLocality());
                                deliveryAddress.put("postalCode", fullWallet.getBuyerShippingAddress().getPostalCode());
                                deliveryAddress.put("stateOrProvince", fullWallet.getBuyerShippingAddress().getAddress3());
                                deliveryAddress.put("country", fullWallet.getBuyerShippingAddress().getCountryCode());
                                paymentData.put("deliveryAddress", deliveryAddress);

                                JSONObject billingAddress = new JSONObject();
                                billingAddress.put("street", fullWallet.getBuyerBillingAddress().getAddress1());
                                billingAddress.put("houseNoOrName", fullWallet.getBuyerBillingAddress().getAddress2());
                                billingAddress.put("city", fullWallet.getBuyerBillingAddress().getLocality());
                                billingAddress.put("postalCode", fullWallet.getBuyerBillingAddress().getPostalCode());
                                billingAddress.put("stateOrProvince", fullWallet.getBuyerBillingAddress().getAddress3());
                                billingAddress.put("country", fullWallet.getBuyerBillingAddress().getCountryCode());
                                paymentData.put("billingAddress", billingAddress);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            /*
                            * Send data to merchant server
                            * */
                            //String url = "http://192.168.10.126:8080/api/payment";
                            String url = "http://192.168.19.98:8080/api/payment";
                            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                                    (Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject response) {
                                            Log.d(tag, "Merchant server response: " + response.toString());
                                            //Process the response received from the merchant server
                                            fetchTransactionStatus(true);
                                        }
                                    }, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            VolleyLog.d(tag, "Error: " + error.getMessage());
                                            //Process the response received from the merchant server
                                            fetchTransactionStatus(false);
                                        }
                                    }) {
                                @Override
                                public byte[] getBody() {
                                    return paymentData.toString().getBytes();
                                }
                            };
                            RequestQueue requestQueue = Volley.newRequestQueue(this);
                            requestQueue.add(jsonObjectRequest);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // nothing to do here
                        break;
                    default:
                        handleError(errorCode);
                        break;
                }
                break;
            case WalletConstants.RESULT_ERROR:
                handleError(errorCode);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void confirmPurchase() {
        getFullWallet();
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void getFullWallet() {
        FullWalletRequest fullWalletRequest = WalletUtil.createFullWalletRequest(productsList,
                String.valueOf(orderTotal),
                mMaskedWallet.getGoogleTransactionId());

        // [START load_full_wallet]
        Wallet.Payments.loadFullWallet(mGoogleApiClient, fullWalletRequest,
                REQUEST_CODE_RESOLVE_LOAD_FULL_WALLET);
        // [END load_full_wallet]
    }

    private void fetchTransactionStatus(boolean successfulOrder) {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        Intent intent = new Intent(this, OrderCompleteActivity.class);

        if(successfulOrder) {
            intent.putExtra("completionMessage", getString(R.string.successful_tx));
        } else {
            intent.putExtra("completionMessage", getString(R.string.unsuccessful_tx));
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    protected void handleError(int errorCode) {
        switch (errorCode) {
            case WalletConstants.ERROR_CODE_SPENDING_LIMIT_EXCEEDED:
                Toast.makeText(this, getString(R.string.spending_limit_exceeded, errorCode),
                        Toast.LENGTH_LONG).show();
                break;
            case WalletConstants.ERROR_CODE_INVALID_PARAMETERS:
            case WalletConstants.ERROR_CODE_AUTHENTICATION_FAILURE:
            case WalletConstants.ERROR_CODE_BUYER_ACCOUNT_ERROR:
            case WalletConstants.ERROR_CODE_MERCHANT_ACCOUNT_ERROR:
            case WalletConstants.ERROR_CODE_SERVICE_UNAVAILABLE:
            case WalletConstants.ERROR_CODE_UNSUPPORTED_API_VERSION:
            case WalletConstants.ERROR_CODE_UNKNOWN:
            default:
                // unrecoverable error
                String errorMessage = getString(R.string.google_wallet_unavailable) + "\n" +
                        getString(R.string.error_code, errorCode);
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                break;
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setMessage("Loading...");
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }
}
