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



    public static void setSound(Context context, boolean sound) {
        android.content.SharedPreferences preferences = context.getSharedPreferences("user_preferences", context.MODE_PRIVATE);
        preferences.edit().putBoolean("sound", sound).commit();
    }

    public static boolean getSound(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences("user_preferences", context.MODE_PRIVATE);
        boolean sound = true;

        if (preferences.contains("sound")) {
            sound = preferences.getBoolean("sound", true);
        }
        return sound ;
    }

    public static void setPoints(Context context, int points) {
        android.content.SharedPreferences preferences = context.getSharedPreferences("user_preferences", context.MODE_PRIVATE);

        /*
        val sharedPref: SharedPreferences = getSharedPreferences(getString(R.string.sharedpreferences), 0) //0 é private mode
        val editor = sharedPref.edit()
        editor.apply()

         */
        preferences.edit().putInt("points", points).commit();
    }

    public static int getPoints(Context context) {
        android.content.SharedPreferences preferences = context.getSharedPreferences("user_preferences", context.MODE_PRIVATE);
        int points = 0;

        if (preferences.contains("points")) {
            points = preferences.getInt("points", 0);
        }
        return points ;
    }


}
