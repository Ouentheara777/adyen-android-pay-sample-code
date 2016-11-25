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
import com.adyen.adyenshop.util.CurrencyUtil;
import com.adyen.adyenshop.util.WalletUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrei on 3/9/16.
 */
public class OrderConfirmationActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{

    private static final String tag = OrderConfirmationActivity.class.getSimpleName();
    private static final int REQUEST_CODE_MASKED_WALLET = 1001;

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
    private String currency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_confirmation);
        Intent intent = getIntent();

        currency = intent.getStringExtra("currency");

        totalPriceTextView = (TextView)findViewById(R.id.total_price);
        shippingTextView = (TextView)findViewById(R.id.shipping_price);
        taxTextView = (TextView)findViewById(R.id.tax_price);
        orderTotalPriceTextView = (TextView)findViewById(R.id.order_total_price);

        mRecyclerView = (RecyclerView) findViewById(R.id.order_list);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        productsList = fillProductListFromIntent(intent);
        orderItemsAdapter = new OrderItemsAdapter(productsList, currency);
        mRecyclerView.setAdapter(orderItemsAdapter);

        cartTotal = intent.getFloatExtra("totalPrice", 0);

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
        totalPriceTextView.setText(CurrencyUtil.getCurrencySymbol(currency) + " " + String.valueOf(String.format("%.02f", cartTotal)));
        shippingTextView.setText(CurrencyUtil.getCurrencySymbol(currency) + " 0.12");
        taxTextView.setText(CurrencyUtil.getCurrencySymbol(currency) + " 0.07");
        orderTotal = (float)(cartTotal + 0.12 + 0.07);
        orderTotalPriceTextView.setText(CurrencyUtil.getCurrencySymbol(currency) + " " + String.valueOf(String.format("%.02f", orderTotal)));
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
                getString(R.string.public_key),
                getApplicationContext());

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
            case WalletConstants.RESULT_ERROR:
                handleError(errorCode);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void confirmPurchase() {
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(Constants.EXTRA_MASKED_WALLET, mMaskedWallet);
        intent.putExtra("orderTotal", orderTotal);
        intent.putExtra("itemsInCart", productsList.toArray(new Product[productsList.size()]));
        intent.putExtra("currency", currency);
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

    @Override
    protected void onStop() {
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if(mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        super.onPause();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }
}
