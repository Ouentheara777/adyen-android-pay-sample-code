package com.adyen.adyenshop.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.adyen.adyenshop.R;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by andrei on 11/15/16.
 */

public class PreferencesUtil {

    public static SharedPreferences getCurrencySharedPreferences(Context context) {
        return context.getSharedPreferences(context.getString(R.string.currency_preferences_file_name), MODE_PRIVATE);
    }

    public static void addStringToSharedPreferences(Context context, String key, String value) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.currency_preferences_file_name), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void registerSharedPreferenceListener(Context context, SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.currency_preferences_file_name), MODE_PRIVATE);
        sharedPref.registerOnSharedPreferenceChangeListener(listener);
    }

}
