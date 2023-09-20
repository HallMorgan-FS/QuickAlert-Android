package com.example.hallmorgan_quickalert.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hallmorgan_quickalert.R;
import com.example.hallmorgan_quickalert.fragments.ContactListFragment;
import com.example.hallmorgan_quickalert.user.Contacts;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;



import java.util.ArrayList;

public class ContactsActivity extends AppCompatActivity implements ContactListFragment.OnEditListener {
    private static final String TAG = "ContactsActivity";

    private FirebaseUser currentUser;
    private final ArrayList<Contacts> contactsArrayList = new ArrayList<>();
    private DatabaseReference userContactsRef;
    private ImageButton addContactsButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setCustomView(R.layout.action_bar_title);

            TextView titleTextView = actionBar.getCustomView().findViewById(R.id.title_text_view);
            titleTextView.setText(R.string.contacts);
        }
        addContactsButton = findViewById(R.id.add_contacts_button);


        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(MainActivity.CURRENT_USER)){
            currentUser = intent.getParcelableExtra(MainActivity.CURRENT_USER);
        }

        //Get the stored contacts from the current user ID
        if (currentUser != null){
            String userID = currentUser.getUid();

            userContactsRef = FirebaseDatabase.getInstance().getReference("users").child(userID).child("contacts");

            userContactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        // User's database node does not exist, create the necessary structure
                        userContactsRef.setValue(null)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        // Database structure created successfully, proceed with adding contacts and showing empty view stub
                                        getSupportFragmentManager().beginTransaction().replace(R.id.listFragment_container, ContactListFragment.newInstance(null)).commit();
                                        Log.i(TAG, "List Fragment should show empty view stub");
                                    } else {
                                        // Handle the error when creating the database structure
                                        Toast.makeText(ContactsActivity.this, "Failed to create user database", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // User's database node exists, proceed with retrieving contacts
                        retrieveContactsFromDatabase(userContactsRef);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle database error
                    Toast.makeText(ContactsActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }


        //Set up button action
        addContactsButton.setOnClickListener(view -> {
            //Set up the contact picker intent
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK);
            contactPickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            contactPickerIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            contactPickerLauncher.launch(contactPickerIntent);
        });

    }

    ActivityResultLauncher<Intent> contactPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    if (data != null){

                        Uri contactUri = data.getData();
                        Contacts contact = getContactDetails(contactUri);
                        if (contact != null && contactsArrayList.size() < 6) {
                            if (!contactsArrayList.contains(contact)){
                                contactsArrayList.add(contact);
                                // Handle the selected contact
                                addContactsToDatabase(contact);
                            } else {
                                //If the selected contact is already added, don't read the contact and send a toast
                                String toastMessage = contact.getName() + " is already a designated emergency contact.";
                                Toast.makeText(ContactsActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                            }

                            // Replace the fragment
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.listFragment_container, ContactListFragment.newInstance(contactsArrayList))
                                    .commit();
                        }
                    }
                }
            });

    private Contacts getContactDetails(Uri contactUri){
        String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()){
            int contactNameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int contactNumberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            String contactName = cursor.getString(contactNameIndex);
            String contactNumber = cursor.getString(contactNumberIndex);
            Log.i(TAG, "Contact name: " + contactName + " Contact Number: " + contactNumber);

            //Close the cursor
            cursor.close();

            return new Contacts(contactName, contactNumber);
        }

        return null;
    }

    private void addContactsToDatabase(Contacts contact){

        //Add each contact separately to the user's contacts node
        String key = userContactsRef.push().getKey();
        if (key != null){
            contact.setID(key);
            userContactsRef.child(key).child("name").setValue(contact.getName());
            userContactsRef.child(key).child("number").setValue(contact.getNumber());
        }
        Log.i(TAG, "Contact Name: " + contact.getName() + " Contact Number: " + contact.getNumber());
        Log.d(TAG, "Contacts were added to the database");
    }

    private void retrieveContactsFromDatabase(DatabaseReference contactsRef){
        contactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User has stored contacts, retrieve the data and update the list view fragment
                    for (DataSnapshot contactSnapshot : snapshot.getChildren()) {
                        String id = contactSnapshot.getKey();
                        String name = contactSnapshot.child("name").getValue(String.class);
                        String number = contactSnapshot.child("number").getValue(String.class);
                        Contacts contact = new Contacts(name, number);
                        contact.setID(id);
                        Log.i(TAG, "onDataChange: Contact name: " + name + " Contact Number: " + number);
                        contactsArrayList.add(contact);
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.listFragment_container, ContactListFragment.newInstance(contactsArrayList)).commit();
                } else {
                    // User does not have any stored contacts
                    getSupportFragmentManager().beginTransaction().replace(R.id.listFragment_container, ContactListFragment.newInstance(null)).commit();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error
                String errorMessage = "Error loading contacts from database";
                Toast.makeText(ContactsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                getSupportFragmentManager().beginTransaction().replace(R.id.listFragment_container, ContactListFragment.newInstance(null)).commit();
            }
        });
    }


    @Override
    public void onEdit(int titleResource, int instructionResource, boolean isEdit) {
        TextView title = findViewById(R.id.profile_title);
        TextView instructions = findViewById(R.id.profile_instructions);
        title.setText(titleResource);
        instructions.setText(instructionResource);
        if (isEdit){
            addContactsButton.setVisibility(View.GONE);
        } else {
            addContactsButton.setVisibility(View.VISIBLE);
        }
    }
}
