package com.adyen.adyenshop.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by andrei on 11/15/16.
 */

public class PreferencesUtil {

    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void addStringToSharedPreferences(Context context, String key, String value) {
        SharedPreferences sharedPref = getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void registerSharedPreferenceListener(Context context, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sharedPref = getDefaultSharedPreferences(context);
        sharedPref.registerOnSharedPreferenceChangeListener(listener);
    }

}
