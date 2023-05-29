package com.example.hallmorgan_quickalert.util;

import android.content.Context;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.hallmorgan_quickalert.R;
import com.example.hallmorgan_quickalert.activities.ContactsActivity;
import com.example.hallmorgan_quickalert.fragments.ContactListFragment;
import com.example.hallmorgan_quickalert.user.Contacts;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class HelperMethods {

    //Method to format phone number
    public static String formatPhoneNumberForMatch(String _phoneNumber){

        return "(" + _phoneNumber.substring(0, 3) + ")" + _phoneNumber.substring(3, 6) + "-" + _phoneNumber.substring(6, 10);
    }

    //Method to verify phone number
    public static boolean numberIsValid(String _phoneNumber){
        if (_phoneNumber.length() == 10){
            String formattedNumber = formatPhoneNumberForMatch(_phoneNumber);
            return Patterns.PHONE.matcher(formattedNumber).matches();
        }

        return false;
    }

    //Method to format phone number for login
    public static String formatNumberForLogin(String _phoneNumber){
        return "+1 " + _phoneNumber.substring(0, 3) + "-" + _phoneNumber.substring(3, 6) + "-" + _phoneNumber.substring(6, 10);
    }

    public static String get911SOSMessage(String name, String location, int contacts){
        return "My name is " + name + ". I am in danger and in need of local police to come to my aid immediately at "
                + location + ". I am in a life threatening situation that is disabling me from calling. "
                + contacts + " emergency contacts have been contacted with my location as well.\n";
    }

    public static String getContactSOSMessage(String name, String location, int num_contacts){
        return "I am in danger and in need of local police to come to my aid immediately at "
                + location + ". I am in a life threatening situation that is disabling me from calling. 911 and "
                + (num_contacts - 1) + " other contacts have been contacted with my location as well. \n";
    }

    public static String getWarningMessage(String location, int num_contacts){
        return "I am potentially in danger at this location, " + location + ". Police have not been contacted yet " +
                "and I do not believe the environment is safe enough to call. Please come to my aid as soon as possible.\n";
    }

    public static String getSignature(String name){
        return "This text was sent on behalf of " + name + " using QuickAlert." +
                " The app that helps you get safe and connected with the touch of a button";
    }


}
