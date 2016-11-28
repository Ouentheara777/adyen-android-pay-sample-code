package com.adyen.adyenshop;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.adyen.adyenshop.model.Product;
import com.adyen.adyenshop.util.Constants;
import com.adyen.adyenshop.util.PreferencesUtil;
import com.adyen.adyenshop.util.WalletUtil;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrei on 3/14/16.
 */
public class FullWalletConfirmationFragment extends Fragment implements
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final String tag = FullWalletConfirmationFragment.class.getSimpleName();
    public static final int REQUEST_CODE_RESOLVE_LOAD_FULL_WALLET = 1004;

    protected GoogleApiClient mGoogleApiClient;
    protected ProgressDialog mProgressDialog;

    private Button mConfirmButton;
    private MaskedWallet mMaskedWallet;
    private Intent mActivityLaunchIntent;

    private List<Product> productsList;
    private float orderTotal;
    private boolean isAuthAndCapture = false;
    private String merchantServerResponse;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityLaunchIntent = getActivity().getIntent();
        mMaskedWallet = mActivityLaunchIntent.getParcelableExtra(Constants.EXTRA_MASKED_WALLET);
        productsList = fillProductListFromIntent(mActivityLaunchIntent);
        orderTotal = mActivityLaunchIntent.getFloatExtra("orderTotal", 0);

        // Set up an API client
        FragmentActivity fragmentActivity = getActivity();

        // [START build_google_api_client]
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .enableAutoManage(fragmentActivity, this /* onConnectionFailedListener */)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(Constants.WALLET_ENVIRONMENT)
                        .setTheme(WalletConstants.THEME_LIGHT)
                        .build())
                .build();
        // [END build_google_api_client]
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        initializeProgressDialog();
        View view = inflater.inflate(R.layout.fragment_full_wallet_confirmation, container, false);

        mConfirmButton = (Button) view.findViewById(R.id.button_place_order);
        mConfirmButton.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        confirmPurchase();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(tag, "Google Play Services Error: " + result.getErrorMessage());
        handleError(result.getErrorCode());

        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    // [START on_activity_result]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mProgressDialog.hide();

        // retrieve the error code, if available
        int errorCode = -1;
        if (data != null) {
            errorCode = data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, -1);
        }

        switch (requestCode) {
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
                                additionalData.put("androidpay.token", fullWallet.getPaymentMethodToken().getToken());
                                paymentData.put("additionalData", additionalData);

                                JSONObject amount = new JSONObject();
                                amount.put("currency", PreferencesUtil.getDefaultSharedPreferences(getActivity().getApplicationContext()).getString(getActivity().getApplicationContext().getString(R.string.active_currency), "USD"));
                                amount.put("value", String.valueOf((int) (orderTotal * 100)));
                                paymentData.put("amount", amount);

                                //installments
                                String noOfInstallments = PreferencesUtil.getDefaultSharedPreferences(getActivity().getApplicationContext()).getString(getString(R.string.number_of_installments), "0");
                                if(Integer.valueOf(noOfInstallments) > 0) {
                                    Log.i(tag, "No. of installments: " + noOfInstallments);
                                    JSONObject installments = new JSONObject();
                                    installments.put("value", Integer.valueOf(noOfInstallments));
                                    paymentData.put("installments", installments);
                                }

                                if(isAuthAndCapture) {
                                    //paymentData.put("merchantAccount", "GooglePOSUK");
                                    paymentData.put("merchantAccount", "TestMerchantAP");
                                    paymentData.put("reference", "AdyenShop_Auth_Capture");
                                    paymentData.put("shopperEmail", fullWallet.getEmail());
                                    Log.i(tag, "Adding parameter captureDelayHours");
                                    paymentData.put("captureDelayHours", 0);
                                } else {
                                    //paymentData.put("merchantAccount", "GooglePOSUK");
                                    paymentData.put("merchantAccount", "TestMerchantAP");
                                    paymentData.put("reference", "AdyenShop_Auth");
                                    paymentData.put("shopperEmail", fullWallet.getEmail());
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Log.i(tag, "=======>" + paymentData.toString());

                            String url = "https://pal-live.adyen.com/pal/servlet/Payment/V12/authorise";
                            AuthRequest jsonObjectRequest = new AuthRequest
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
                                            if(error.networkResponse != null && error.networkResponse.data != null) {
                                                merchantServerResponse = new String(error.networkResponse.data);
                                                Log.d(tag, "Merchant server response: " + (new String(error.networkResponse.data)));
                                            }

                                            //Process the response received from the merchant server
                                            fetchTransactionStatus(false);
                                        }
                                    }) {
                                @Override
                                public byte[] getBody() {
                                    return paymentData.toString().getBytes();
                                }
                            };
                            RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
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
        }
    }
    // [END on_activity_result]

    public void updateMaskedWallet(MaskedWallet maskedWallet) {
        mMaskedWallet = maskedWallet;
    }

    /**
     * For unrecoverable Google Wallet errors, send the user back to the checkout page to handle the
     * problem.
     *
     * @param errorCode
     */
    protected void handleUnrecoverableGoogleWalletError(int errorCode) {
        Intent intent = new Intent(getActivity(), OrderConfirmationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(WalletConstants.EXTRA_ERROR_CODE, errorCode);
        intent.putExtra("totalPrice", orderTotal);
        intent.putExtra("itemsInCart", productsList.toArray(new Product[productsList.size()]));
        startActivity(intent);
    }

    private void handleError(int errorCode) {
        switch (errorCode) {
            case WalletConstants.ERROR_CODE_SPENDING_LIMIT_EXCEEDED:
                // may be recoverable if the user tries to lower their charge
                // take the user back to the checkout page to try to handle
            case WalletConstants.ERROR_CODE_INVALID_PARAMETERS:
            case WalletConstants.ERROR_CODE_AUTHENTICATION_FAILURE:
            case WalletConstants.ERROR_CODE_BUYER_ACCOUNT_ERROR:
            case WalletConstants.ERROR_CODE_MERCHANT_ACCOUNT_ERROR:
            case WalletConstants.ERROR_CODE_SERVICE_UNAVAILABLE:
            case WalletConstants.ERROR_CODE_UNSUPPORTED_API_VERSION:
            case WalletConstants.ERROR_CODE_UNKNOWN:
            default:
                // unrecoverable error
                // take the user back to the checkout page to handle these errors
                handleUnrecoverableGoogleWalletError(errorCode);
        }
    }

    public void confirmPurchase() {
        getFullWallet();
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void getFullWallet() {
        FullWalletRequest fullWalletRequest = WalletUtil.createFullWalletRequest(productsList,
                String.valueOf(orderTotal),
                mMaskedWallet.getGoogleTransactionId(),
                getActivity().getApplicationContext());

        // [START load_full_wallet]
        Wallet.Payments.loadFullWallet(mGoogleApiClient, fullWalletRequest,
                REQUEST_CODE_RESOLVE_LOAD_FULL_WALLET);
        // [END load_full_wallet]
    }

    private void fetchTransactionStatus(boolean successfulOrder) {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }

        Intent intent = new Intent(getActivity(), OrderCompleteActivity.class);

        if(successfulOrder) {
            intent.putExtra("completionMessage", getString(R.string.successful_tx));
        } else {
            intent.putExtra("completionMessage", getString(R.string.unsuccessful_tx) + "\n" + merchantServerResponse);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    protected void initializeProgressDialog() {
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setMessage(getString(R.string.loading));
        mProgressDialog.setIndeterminate(true);
    }


    public List<Product> fillProductListFromIntent(Intent intent) {
        List<Product> products = new ArrayList<>();
        Parcelable[] productsInCart = intent.getParcelableArrayExtra("itemsInCart");
        for(int i=0; i<productsInCart.length; i++) {
            products.add((Product)productsInCart[i]);
        }
        return products;
    }

    public void setIsAuthAndCapture(boolean isAuthAndCapture) {
        this.isAuthAndCapture = isAuthAndCapture;
    }
}
