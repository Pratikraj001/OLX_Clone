package com.example.olxclone;

import android.content.Context;
import android.text.format.DateFormat;
import android.widget.Toast;


import java.util.Calendar;
import java.util.Locale;


//A class that will contain static functions, constants, variables that we will be used in whole application
    public class Utils {

        /** A Function to show Toast
         *
         * @param context the context of activity/fragement from where this function will be called
         * @param message the message to be shown in the Toast
         */
    public static void toast(Context context, String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

        /** A function to get current timestamp
         * @return Return the current timestamp as long datatype
         */
    public static long getTimeStamp(){
        return System.currentTimeMillis();
    }

    public static String formatTimestampData(Long timestamp){
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(timestamp);

        String date = DateFormat.format("dd/MM/yyyy", calendar).toString();

        return date;
    }
}
