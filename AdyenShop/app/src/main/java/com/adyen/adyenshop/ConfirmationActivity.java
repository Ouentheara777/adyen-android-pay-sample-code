package com.adyen.adyenshop;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import com.adyen.adyenshop.adapter.OrderItemsAdapter;
import com.adyen.adyenshop.model.Product;
import com.adyen.adyenshop.util.Constants;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrei on 3/14/16.
 */
public class ConfirmationActivity extends AdyenShopFragmentActivity {

    private static final String tag = ConfirmationActivity.class.getSimpleName();

    private static final int REQUEST_CODE_CHANGE_MASKED_WALLET = 1002;
    private SupportWalletFragment mWalletFragment;
    private MaskedWallet mMaskedWallet;

    private OrderItemsAdapter orderItemsAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;

    private List<Product> productsList;
    private String currency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMaskedWallet = getIntent().getParcelableExtra(Constants.EXTRA_MASKED_WALLET);
        setContentView(R.layout.activity_confirmation);
        Intent intent = getIntent();

        currency = intent.getStringExtra("currency");

        mRecyclerView = (RecyclerView)findViewById(R.id.order_confirmation_list);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        productsList = fillProductListFromIntent(intent);
        orderItemsAdapter = new OrderItemsAdapter(productsList, currency);
        mRecyclerView.setAdapter(orderItemsAdapter);

        createAndAddWalletFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // no need to show login menu on confirmation screen
        return false;
    }

    private void createAndAddWalletFragment() {
        WalletFragmentStyle walletFragmentStyle = new WalletFragmentStyle()
                .setMaskedWalletDetailsTextAppearance(
                        R.style.AdyenShopWalletFragmentDetailsTextAppearance)
                .setMaskedWalletDetailsHeaderTextAppearance(
                        R.style.AdyenShopWalletFragmentDetailsHeaderTextAppearance)
                .setMaskedWalletDetailsBackgroundColor(getResources().getColor(R.color.white));

        // [START wallet_fragment_options]
        WalletFragmentOptions walletFragmentOptions = WalletFragmentOptions.newBuilder()
                .setEnvironment(Constants.WALLET_ENVIRONMENT)
                .setFragmentStyle(walletFragmentStyle)
                .setTheme(WalletConstants.THEME_LIGHT)
                .setMode(WalletFragmentMode.SELECTION_DETAILS)
                .build();
        mWalletFragment = SupportWalletFragment.newInstance(walletFragmentOptions);
        // [END wallet_fragment_options]

        // Now initialize the Wallet Fragment
        WalletFragmentInitParams.Builder startParamsBuilder = WalletFragmentInitParams.newBuilder()
                .setMaskedWallet(mMaskedWallet)
                .setMaskedWalletRequestCode(REQUEST_CODE_CHANGE_MASKED_WALLET)
                .setAccountName(null);
        mWalletFragment.initialize(startParamsBuilder.build());

        // add Wallet fragment to the UI
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.dynamic_wallet_masked_wallet_fragment, mWalletFragment)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CHANGE_MASKED_WALLET:
                if (resultCode == Activity.RESULT_OK &&
                        data.hasExtra(WalletConstants.EXTRA_MASKED_WALLET)) {
                    mMaskedWallet = data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
                    ((FullWalletConfirmationFragment) getResultTargetFragment())
                            .updateMaskedWallet(mMaskedWallet);
                }
                // you may also want to use the new masked wallet data here, say to recalculate
                // shipping or taxes if shipping address changed
                break;
            case WalletConstants.RESULT_ERROR:
                int errorCode = data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, 0);
                handleError(errorCode);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public Fragment getResultTargetFragment() {
        return getSupportFragmentManager().findFragmentById(
                R.id.full_wallet_confirmation_button_fragment);
    }

    public List<Product> fillProductListFromIntent(Intent intent) {
        List<Product> products = new ArrayList<>();
        Parcelable[] productsInCart = intent.getParcelableArrayExtra("itemsInCart");
        for(int i=0; i<productsInCart.length; i++) {
            products.add((Product)productsInCart[i]);
        }
        return products;
    }

    public void authAndCapture(View view) {
        Log.i(tag, "AUTH + CAPTURE was chosen");
        ((FullWalletConfirmationFragment) getResultTargetFragment()).setIsAuthAndCapture(true);
        ((FullWalletConfirmationFragment) getResultTargetFragment()).confirmPurchase();
    }

}
