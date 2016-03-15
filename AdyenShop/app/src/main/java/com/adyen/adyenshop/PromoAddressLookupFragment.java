package com.adyen.adyenshop;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by andrei on 3/14/16.
 */
public class PromoAddressLookupFragment extends Fragment implements
        View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final int REQUEST_CODE_RESOLVE_ADDRESS_LOOKUP = 1006;
    public static final int REQUEST_CODE_RESOLVE_ERR = 1007;
    private static final String KEY_PROMO_CLICKED = "KEY_PROMO_CLICKED";


    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
