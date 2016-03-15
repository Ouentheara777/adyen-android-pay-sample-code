package com.adyen.adyenshop;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

/**
 * Created by andrei on 3/14/16.
 */
public abstract class AdyenShopFragmentActivity extends FragmentActivity {

    /**
     * When calling {@link Wallet#loadFullWallet(GoogleApiClient, FullWalletRequest, int)} or
     * resolving connection errors with
     * {@link ConnectionResult#startResolutionForResult(android.app.Activity, int)},
     * the given {@link Activity}'s callback is called. Since in this case, the caller is a
     * {@link Fragment}, and not {@link Activity} that is passed in, this callback is forwarded to
     * {@link FullWalletConfirmationFragment}, {@link PromoAddressLookupFragment}
     * If the requestCode is one of the predefined codes to handle
     * the API calls, pass it to the fragment or else treat it normally.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FullWalletConfirmationFragment.REQUEST_CODE_RESOLVE_LOAD_FULL_WALLET:
            case PromoAddressLookupFragment.REQUEST_CODE_RESOLVE_ADDRESS_LOOKUP:
            case PromoAddressLookupFragment.REQUEST_CODE_RESOLVE_ERR:
                Fragment fragment = getResultTargetFragment();
                if (fragment != null) {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
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

    /**
     * @return The Fragment that should handle result. Some implementations can return null.
     */
    protected abstract Fragment getResultTargetFragment();

}
