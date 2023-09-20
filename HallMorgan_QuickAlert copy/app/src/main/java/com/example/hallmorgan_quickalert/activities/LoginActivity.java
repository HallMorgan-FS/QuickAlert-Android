package com.example.hallmorgan_quickalert.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hallmorgan_quickalert.R;
import com.example.hallmorgan_quickalert.util.HelperMethods;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthMultiFactorException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks verificationCallbacks;
    private String verificationId;

    private EditText phoneNumberEditText;
    private LinearLayout verificationCodeLayout;
    private EditText[] verificationCodeEditTexts;
    private ImageButton toVerifyButton;
    private TextView instructionText;
    private String enteredNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Make the activity full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        phoneNumberEditText = findViewById(R.id.phoneNumber_editText);
        // Check if the EditText is not null
        if (phoneNumberEditText != null) {
            phoneNumberEditText.setHint(R.string.phoneNumber_placeholder);
        } else {
            Log.e("LoginActivity", "phoneNumberEditText is null");
        }


        //Get the instance of the Firebase Authentication
        firebaseAuth = FirebaseAuth.getInstance();

        //firebaseAuth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
        firebaseAuth.signOut();
        //Check if user is already signed in
        //get the current user
        currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null){
            //User is already signed in
            goToHomeScreen();
        } else {
            //Set up UI
            setUpUI();
        }

    }


    private void setUpUI(){
        //Set up the login activity's UI
        instructionText = findViewById(R.id.login_instruction_textView);
        verificationCodeLayout = findViewById(R.id.verification_code_layout);
        verificationCodeEditTexts = new EditText[]{
                findViewById(R.id.first_digit_et),
                findViewById(R.id.second_digit_et),
                findViewById(R.id.third_digit_et),
                findViewById(R.id.fourth_digit_et),
                findViewById(R.id.fifth_digit_et),
                findViewById(R.id.sixth_digit_et)
        };


        for (EditText editText : verificationCodeEditTexts){
            editText.setHint(R.string.zero);

        }
        Button goBackToNumberButton = findViewById(R.id.goBack_button);
        goBackToNumberButton.setOnClickListener(view -> {
            verificationCodeLayout.setVisibility(View.GONE);
            phoneNumberEditText.setVisibility(View.VISIBLE);
            toVerifyButton.setVisibility(View.VISIBLE);
            clearPhoneNumber();
            instructionText.setText(R.string.enter_your_phone_number);
        });
        toVerifyButton = findViewById(R.id.toVerify_button);
        ImageButton verifyCodeButton = findViewById(R.id.verifyCode_button);

        //Set up the verification callbacks
        verificationCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    Toast.makeText(LoginActivity.this, "Invalid phone number.", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Toast.makeText(LoginActivity.this, "Quota exceeded. Please try again later.", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseAuthMultiFactorException) {
                    // Multi-factor authentication is enabled
                    Toast.makeText(LoginActivity.this, "Multi-factor authentication is not supported.", Toast.LENGTH_SHORT).show();
                }
                clearPhoneNumber();
            }

            @Override
            public void onCodeSent(@NonNull String _verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                Log.i(TAG, "onCodeSent: " + _verificationId);
                //capture the verification id
                verificationId = _verificationId;
                // Hide the phone edit text and verify phone button
                phoneNumberEditText.setVisibility(View.GONE);
                toVerifyButton.setVisibility(View.GONE);
                // Change the instruction text
                instructionText.setText(R.string.verify_code);
                // Enable the verification code layout
                verificationCodeLayout.setVisibility(View.VISIBLE);
                // Set up the edit text boxes
                setupVerificationCodeEditTexts();
            }
        }; //End of callbacks

        //Set onClickListener for toVerify_button
        toVerifyButton.setOnClickListener(view -> {
            String _phoneNumber = phoneNumberEditText.getText().toString().trim();
            if (!_phoneNumber.isEmpty() && HelperMethods.numberIsValid(_phoneNumber)){
                //If the phone number is not empty, verify the phone number and send verification code
                enteredNumber = HelperMethods.formatNumberForLogin(_phoneNumber);
                sendVerificationCode(_phoneNumber);
            } else {
                //Phone number is empty, notify the user
                Toast.makeText(LoginActivity.this, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show();
            }
        });

        //Set onClickListener for the verify code button
        verifyCodeButton.setOnClickListener(view -> {
            //Should only play a part if a code is sent to the user
            String verificationCode = getEnteredVerificationCode();
            if (!verificationCode.isEmpty()){
                //verify the entered verification code
                Log.i(TAG, "setUpUI: Verification code not empty. Verify entered verification code\nVerification Code:" + verificationCode);
                PhoneAuthCredential _credential = PhoneAuthProvider.getCredential(verificationId, verificationCode);
                //Sign in with phone auth credential
                signInWithPhoneAuthCredential(_credential);
            } else {
                //Verification code is empty
                Log.i(TAG, "setUpUI: Verification code is empty");
                Toast.makeText(LoginActivity.this, "Please enter a valid verification code.", Toast.LENGTH_SHORT).show();
                clearCodeFields();
            }
        });
    }

    private void sendVerificationCode(String _phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber("+1"+_phoneNumber).setActivity(this)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setCallbacks(verificationCallbacks).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        Log.i(TAG, "sendVerificationCode: Verification code sent: " + verificationId);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()){
                        Log.i(TAG, "signInWithPhoneAuthCredential: Sign in Successful");
                        //Sign in success
                        currentUser = task.getResult().getUser();
                        //Go to the main activity
                        goToHomeScreen();
                        finish();
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                            //Invalid verification code
                            Log.e(TAG, "signInWithPhoneAuthCredential: Sign in unsuccessful due to incorrect verification code");
                            Toast.makeText(LoginActivity.this, "Invalid verification code. Please try again.", Toast.LENGTH_SHORT).show();
                            //clear code edit text fields
                            clearCodeFields();
                        } else {
                            Log.e(TAG, "signInWithPhoneAuthCredential: Sign in unsuccessful due to unidentified reasons");
                            Toast.makeText(LoginActivity.this, "Sign in failed. Please try again.", Toast.LENGTH_SHORT).show();
                            //Clear phone number
                            clearPhoneNumber();
                        }
                    }
                });
    }


    private String getEnteredVerificationCode(){
        StringBuilder codeSb = new StringBuilder();
        for (int i = 0; i < verificationCodeEditTexts.length; i++) {
            EditText editText = verificationCodeEditTexts[i];
            String code = editText.getText().toString().trim();
            if (code.length() == 1) {
                codeSb.append(code);
                if (i < verificationCodeEditTexts.length - 1) {
                    // Move the focus to the next EditText
                    verificationCodeEditTexts[i + 1].requestFocus();
                }
            }
        }
        Log.i(TAG, "getEnteredVerificationCode: Verification code:");
        return codeSb.toString();
    }

    private void setupVerificationCodeEditTexts() {
        for (EditText editText : verificationCodeEditTexts) {
            editText.setHint(R.string.zero);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    String code = editable.toString();
                    if (code.length() == 1) {
                        if (editText == verificationCodeEditTexts[verificationCodeEditTexts.length - 1]) {
                            // Last EditText, close the keyboard
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        } else {
                            // Move the focus to the next EditText
                            View nextFocusView = editText.focusSearch(View.FOCUS_RIGHT);
                            if (nextFocusView != null) {
                                nextFocusView.requestFocus();
                            }
                        }
                    }
                }
            });
        }
    }

    private void clearCodeFields() {
        for (EditText editText : verificationCodeEditTexts){
            editText.setText("");
        }
    }
    private void clearPhoneNumber(){
        phoneNumberEditText.setText("");
        phoneNumberEditText.setHint(R.string.phoneNumber_placeholder);
    }

    private void goToHomeScreen(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.CURRENT_USER, currentUser);
        startActivity(intent);
    }

}
