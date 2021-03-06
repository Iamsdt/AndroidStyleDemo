package com.blogspot.shudiptotrafder.androidstyledemo.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.blogspot.shudiptotrafder.androidstyledemo.data.DataBaseProvider;

import java.io.IOException;

/**
 * Created by Shudipto on 6/6/2017.
 */

public class Utility {

//    private static boolean getNightModeEnabled(Context context) {
//
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
//
//        return preferences.getBoolean(context.getString(R.string.switchKey),false);
//    }

    /**
     * this methods for database initialize
     * it's only called for the first time of the app run
     * or update of any data base
     */
    public static void initializedDatabase(Context context) {

        DataBaseProvider provider = new DataBaseProvider(context);

        //SharedPreferences preferences for database initialize
        // for first time value
        SharedPreferences preferences = context.getSharedPreferences(ConstantUtills.DATABASE_INIT_SP_KEY,
                Context.MODE_PRIVATE);
        //sate of database is initialized or not
        boolean state = preferences.getBoolean(ConstantUtills.DATABASE_INIT_SP_KEY, false);

        SharedPreferences.Editor editor = preferences.edit();
        try {
            if (!state) {
                provider.loadWords();
                editor.putBoolean(ConstantUtills.DATABASE_INIT_SP_KEY, true);
                sle("initializedDatabase called");
            }
        } catch (IOException e) {
            e.printStackTrace();
            slet("Error to initialized data", e);
            editor.putBoolean(ConstantUtills.DATABASE_INIT_SP_KEY, false);
        }
        editor.apply();
    }

    /**
     * This methods show log error message with throwable
     *
     * @param message String show on log
     */
    private static void sle(String message) {

        final String TAG = "Utility";


            Log.e(TAG, message);

    }

    /**
     * This methods show log error message with throwable
     *
     * @param message String show on log
     * @param t       throwable that's show on log
     */

    private static void slet(String message, Throwable t) {

        final String TAG = "Utility";


            Log.e(TAG, message, t);

    }
}
