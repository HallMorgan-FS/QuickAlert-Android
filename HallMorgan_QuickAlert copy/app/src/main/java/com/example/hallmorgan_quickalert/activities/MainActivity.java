package com.example.hallmorgan_quickalert.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.hallmorgan_quickalert.R;
import com.example.hallmorgan_quickalert.fragments.ContactListFragment;
import com.example.hallmorgan_quickalert.user.Contacts;
import com.example.hallmorgan_quickalert.util.HelperMethods;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private static final int REQUEST_LOCATION_PERMISSION = 123;
    public static final String CURRENT_USER = ".com.example.hallmorgan_quickalert.CURRENT_USER";
    private FirebaseUser currentUser;
    private final ArrayList<Contacts> contacts = new ArrayList<>();

    private static final int MAX_TAPS = 5;
    private int tapCount = 0;
    private long lastTapTime = 0;
    private boolean accessLocation;
    private String readableAddress;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double latitude;
    private double longitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.action_bar_title);

            TextView titleTextView = actionBar.getCustomView().findViewById(R.id.title_text_view);
            titleTextView.setText(R.string.home);
        }

        //Access Permissions
        checkLocationPermissions();

        // Initialize the LocationManager and LocationListener in your onCreate() method or relevant initialization code
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Handle location updates here
                //get the latitude and longitude of the user
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // Log the status change
                switch (status) {
                    case LocationProvider.AVAILABLE:
                        // Provider is available and providing location updates
                        Log.d("Location", "Provider status changed - Provider: " + provider + ", Status: AVAILABLE");
                        break;
                    case LocationProvider.OUT_OF_SERVICE:
                        // Provider is out of service and temporarily unavailable
                        Log.d("Location", "Provider status changed - Provider: " + provider + ", Status: OUT_OF_SERVICE");
                        break;
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        // Provider is temporarily unavailable
                        Log.d("Location", "Provider status changed - Provider: " + provider + ", Status: TEMPORARILY_UNAVAILABLE");
                        break;
                    default:
                        // Unknown status
                        Log.d("Location", "Provider status changed - Provider: " + provider + ", Status: UNKNOWN");
                        break;
                }
            }

            @Override
            public void onProviderEnabled(String provider) {
                // Log the provider enabled event
                Log.d("Location", "Provider enabled - Provider: " + provider);
            }

            @Override
            public void onProviderDisabled(String provider) {
                // Log the provider disabled event
                Log.d("Location", "Provider disabled - Provider: " + provider);
            }
        };

        requestLocationUpdates();

        // Retrieve the current user from the intent extras
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(CURRENT_USER)) {
            currentUser = intent.getParcelableExtra(CURRENT_USER);
            String userID = currentUser.getUid();
            String userName = currentUser.getDisplayName();
            if (userName == null){
                getUsersName();
            }

            DatabaseReference userContactsRef = FirebaseDatabase.getInstance().getReference("users").child(userID).child("contacts");

            //Get the list of contacts from the ContactsActivity
            retrieveContactsFromDatabase(userContactsRef);
        }

        ImageButton sosButton = (ImageButton) findViewById(R.id.sos_button);
        ImageButton warningButton = (ImageButton) findViewById(R.id.warning_button);
        sosButton.setOnClickListener(this);
        warningButton.setOnClickListener(this);

    }


    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            //request location services again
            showPermissionRationaleDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onClick(View view) {
        String buttonTag = (String) view.getTag();
        //Check the number of taps
        long currentTapTime = System.currentTimeMillis();

        if (accessLocation){
            //Go ahead and request location updates
            requestLocationUpdates();
            //Get their readable address
            getAddress(latitude, longitude);
        }

        // Calculate the time elapsed since the last tap
        long timeElapsed = currentTapTime - lastTapTime;

        // Check if the time elapsed is within a certain threshold (5 taps in 1 second)
        if (timeElapsed < 1000) {
            tapCount++;
        } else {
            tapCount = 1;
        }

        // Update the last tap time
        lastTapTime = currentTapTime;

        // Check the tap count
        if (tapCount == 1) {
            // User tapped once
            // Perform the action for a single tap
            showSendSMSDialog(buttonTag);


        } else if (tapCount <= MAX_TAPS) {
            // User tapped the maximum number of times in a row
            // Perform the action for multiple taps
            sendSMS(buttonTag);
        }

    }

    private void showSendSMSDialog(String buttonTag) {
        String sosTitle = "Send Emergency SOS";
        String warningTitle = "Send Possible Danger SMS";
        String sosAlertMessage = "Are you sure you want to send the SOS emergency message to all designated contacts and 911?";
        String warningAlertMessage = "Are you sure you want to send the potential danger message to all designated contacts?";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (buttonTag.equals("SOS")) {
            builder.setTitle(sosTitle).setMessage(sosAlertMessage);
        } else {
            builder.setTitle(warningTitle).setMessage(warningAlertMessage);
        }
        builder.setPositiveButton("Confirm", (dialogInterface, i) -> {
            //Send emergency SOS
            sendSMS(buttonTag);
        })
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }


    private void sendSMS(String buttonTag) {
        String name = currentUser.getDisplayName();
        String alertMessage;
        int num_contacts = contacts.size();
        SmsManager smsManager = SmsManager.getDefault();
        if (buttonTag.equals("SOS")){
            String sos911message = HelperMethods.get911SOSMessage(name, readableAddress, num_contacts) + HelperMethods.getSignature(name);
            String sosContactMessage = HelperMethods.getContactSOSMessage(name, readableAddress, num_contacts) + HelperMethods.getSignature(name);
            smsManager.sendTextMessage("+19108845882", null, sos911message, null, null);
            for (Contacts contact : contacts){
                smsManager.sendTextMessage(contact.getNumber(), null, sosContactMessage, null, null);
            }
            alertMessage = "The distress message with your location has been sent 911 and all designated contacts. Stay safe, help is on the way.\nThis box will close in 3 minutes";
        } else {
            String message = HelperMethods.getWarningMessage(readableAddress, num_contacts) + HelperMethods.getSignature(name);
            for (Contacts contact : contacts){
                smsManager.sendTextMessage(contact.getNumber(), null, message, null, null);
            }
            alertMessage = "The distress message with your location has been sent to all designated contacts. Stay safe, help is on the way.\nThis box will close in 3 minutes";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Help Is On The Way");
        builder.setMessage(alertMessage);
        builder.setPositiveButton("OK", null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // Define a delay of 3 minutes (in milliseconds)
        long delayMillis = 3 * 60 * 1000; // 3 minutes

        // Create a handler and post a runnable to dismiss the dialog after the delay
        new Handler().postDelayed(() -> {
            if (alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
        }, delayMillis);

    }

    private void getAddress(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Extract the address components
                String addressLine = address.getAddressLine(0);
                String city = address.getLocality();
                String state = address.getAdminArea();
                String country = address.getCountryName();
                String postalCode = address.getPostalCode();

                // Use the address components as needed
                readableAddress = addressLine + ", " + city + ", " + state + ", " + country + ", " + postalCode;

            } else {
                // No address found
                readableAddress = "No readable address found. Coordinates are: Lat: " + latitude + " Long: " + longitude;
            }
        } catch (IOException e) {
            // Error occurred while geocoding
            e.printStackTrace();
        }
    }



    private void checkLocationPermissions(){
        // Check if the permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Display a rationale to the user for requesting the permission (optional)
                // For example, you can show a dialog explaining why you need the location permission
                showPermissionRationaleDialog();
            } else {
                // Request the permission directly
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            }
        }
    }

    private void showPermissionRationaleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location Permission");
        builder.setMessage("We need access to your location to ensure application performs as intended. Do you give QuickAlert permission to access your location only while using the app?");
        builder.setPositiveButton("YES", (dialog, which) -> {
            // Request the permission after the user acknowledges the rationale
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            accessLocation = true;
            dialog.dismiss();
        });
        builder.setNegativeButton("NO", (dialog, which) -> {
            // Handle the user's decision to not grant the permission
            accessLocation = false;
            Toast.makeText(MainActivity.this, "App needs location permissions to work properly. Turn on location services in App Settings", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with accessing the location
                accessLocation = true;
            } else {
                // Permission denied
                accessLocation = false;
                Toast.makeText(this, "App needs location permissions to work properly. Turn on location services in App Settings", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void retrieveContactsFromDatabase(DatabaseReference contactsRef){
        contactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User has stored contacts, retrieve the data and update the list view fragment
                    for (DataSnapshot contactSnapshot : snapshot.getChildren()) {
                        String name = contactSnapshot.child("name").getValue(String.class);
                        String number = contactSnapshot.child("number").getValue(String.class);
                        Contacts contact = new Contacts(name, number);
                        Log.i(TAG, "onDataChange: Contact name: " + name + " Contact Number: " + number);
                        contacts.add(contact);
                    }

                } else {
                    // User does not have any stored contacts
                    //Show no contacts alert
                    noContactsAlert();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
                String errorMessage = "Error loading contacts from database";
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                getSupportFragmentManager().beginTransaction().replace(R.id.listFragment_container, ContactListFragment.newInstance(null)).commit();
            }
        });
    }

    private void noContactsAlert() {
        //Create alert
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Emergency Contacts")
                .setMessage("You don't have any designated emergency contacts. Would you like to add some now?")
                .setPositiveButton("YES", (dialogInterface, i) -> goToContactsActivity())
                .setNegativeButton("NO", (dialogInterface, i) -> {
                    dialogInterface.dismiss(); //Close the dialog
                });

        //Create and display the alert
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    private void getUsersName() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter First and Last Name");

        final EditText editTextFirstName = new EditText(this);
        editTextFirstName.setHint("First Name");

        final EditText editTextLastName = new EditText(this);
        editTextLastName.setHint("Last Name");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(editTextFirstName);
        layout.addView(editTextLastName);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialogInterface, i) -> {


            if (currentUser != null){
                if (editTextFirstName.getText() == null || editTextLastName.getText() == null){
                    Toast.makeText(MainActivity.this, "Please enter a valid first and last name.", Toast.LENGTH_SHORT).show();
                } else {
                    String firstName = editTextFirstName.getText().toString().trim();
                    String lastName = editTextLastName.getText().toString().trim();
                    String displayName = firstName + " " + lastName;
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build();

                    currentUser.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                        if (task.isSuccessful()){
                            //Display name updated successfully
                            Toast.makeText(MainActivity.this, "Display name updated", Toast.LENGTH_SHORT).show();
                        } else {
                            // Failed to update display name
                            Toast.makeText(MainActivity.this, "Failed to update display name", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            }
        });


        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.profile_menu_item){
            goToContactsActivity();
            return true;
        }
        return false;
    }

    //Go to Profile Activity
    private void goToContactsActivity(){
        Intent profileIntent = new Intent(this, ContactsActivity.class);
        profileIntent.putExtra(CURRENT_USER, currentUser);
        startActivity(profileIntent);
    }

}
