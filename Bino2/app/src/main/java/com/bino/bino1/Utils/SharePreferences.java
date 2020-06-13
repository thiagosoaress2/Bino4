package com.bino.bino1.Utils;

import android.content.Context;

public class SharePreferences {

    public static void setEmergency(Context context, boolean emergency) {
        android.content.SharedPreferences preferences = context.getSharedPreferences("user_preferences", context.MODE_PRIVATE);
        preferences.edit().putBoolean("emergency", emergency).commit();
    }

    public static boolean getEmergency(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences("user_preferences", context.MODE_PRIVATE);
        boolean emergency = false;

        if (preferences.contains("emergency")) {
            emergency = preferences.getBoolean("emergency", false);
        }
        return emergency ;
    }

}
