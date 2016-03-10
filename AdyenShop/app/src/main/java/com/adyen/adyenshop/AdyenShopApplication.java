package com.adyen.adyenshop;

import android.app.Application;
import android.content.SharedPreferences;

/**
 * Created by andrei on 3/9/16.
 */
public class AdyenShopApplication extends Application {

    private static final String USER_PREFS = "com.google.android.gms.samples.wallet.USER_PREFS";
    private static final String KEY_USERNAME = "com.google.android.gms.samples.wallet.KEY_USERNAME";
    private String mUserName;

    // Not being saved in shared preferences to let users try new addresses
    // between app invocations
    private boolean mAddressValidForPromo;

    private SharedPreferences mPrefs;

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = getSharedPreferences(USER_PREFS, MODE_PRIVATE);
        mUserName = mPrefs.getString(KEY_USERNAME, null);
    }

    public boolean isLoggedIn() {
        return mUserName != null;
    }

    public void login(String userName) {
        mUserName = userName;
        mPrefs.edit().putString(KEY_USERNAME, mUserName).commit();
    }

    public void logout() {
        mUserName = null;
        mPrefs.edit().remove(KEY_USERNAME).commit();
    }

    public String getAccountName() {
        return mPrefs.getString(KEY_USERNAME, null);
    }

    public boolean isAddressValidForPromo() {
        return mAddressValidForPromo;
    }

    public void setAddressValidForPromo(boolean addressValidForPromo) {
        this.mAddressValidForPromo = addressValidForPromo;
    }

}
