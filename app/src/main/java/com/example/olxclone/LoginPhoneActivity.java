package com.example.olxclone;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.olxclone.databinding.ActivityLoginPhoneBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class LoginPhoneActivity extends AppCompatActivity {

    private ActivityLoginPhoneBinding binding;

    private ProgressDialog progressDialog;

    private FirebaseAuth firebaseAuth;

    private PhoneAuthProvider.ForceResendingToken forceResendingToken;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private String mVerificationId;

    private static final String TAG = "LOGIN_PHONE_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //activity_phone_login.xml = ActivityLoginPhoneBlinding
        binding = ActivityLoginPhoneBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //For the start show phone input UI and hide OTP UI
        binding.phoneInputRl.setVisibility(View.VISIBLE);
        binding.otpInputRl.setVisibility(View.GONE);

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth = FirebaseAuth.getInstance();

        phoneLoginCallBack();

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        binding.sendOtpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateDate();
            }
        });

        binding.resendOtpTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resendVerificationCode(forceResendingToken);
            }
        });

        binding.verifyOtpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String otp = binding.otpEt.getText().toString().trim();

                Log.d(TAG, "onClick: OTP: "+otp);

                if(otp.isEmpty()){

                    binding.otpEt.setError("Enter OTP");
                    binding.otpEt.requestFocus();
                }
                else if(otp.length() < 6){
                    binding.otpEt.setError("OTP length must be 6 Characters");
                    binding.otpEt.requestFocus();
                }
                else{
                    verifyPhoneNumberWithCode(mVerificationId, otp);
                }
            }
        });
    }
    private String phoneCode = "", phoneNumber = "", phoneNumberWithCode = "";

    private void validateDate(){
        //input data
        phoneCode = binding.phoneCodeTil.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();
        phoneNumberWithCode = phoneCode+phoneNumber;

        Log.d(TAG, "validateDate: phoneCode: "+phoneCode);
        Log.d(TAG, "validateDate: phoneNumber: "+phoneNumber);
        Log.d(TAG, "validateDate: phoneNumberWithCode: "+phoneNumberWithCode);

        //validate data
        if(phoneNumber.isEmpty()){
            //phoneNumber is not entered, show error
            Utils.toast(this, "Please enter Phone Number");
        }
        else{
            startPhoneNumberVerification();
        }
    }

    private void startPhoneNumberVerification(){
        //show progress
        progressDialog.setMessage("Sending OTP to "+phoneNumberWithCode.toString());
        progressDialog.show();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)   //FirebaseAuth instance
                .setPhoneNumber(phoneNumberWithCode)                           //Phone Number with country Code e.g. +91********
                .setTimeout(60L, TimeUnit.SECONDS)                      //Timeout and unit
                .setActivity(this)                                              //Activity (for callback binding)
                .setCallbacks(mCallbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }
    private void phoneLoginCallBack(){
        Log.d(TAG, "phoneLoginCallBack: ");

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted: ");
                // This callback will be invoked in two situation:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //      verified without needed to send or enter a verification code.
                // 2 - Auto-retrieval. On some Google Play services can automatically
                //      detect the incoming verification SMS and perform verification without
                //      user action.

                signInWithPhoneAuthCredential(credential);

            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.e(TAG, "onVerificationFailed: ", e);
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the phone number format is not valid.
                progressDialog.dismiss();

                Utils.toast(LoginPhoneActivity.this, ""+e.getMessage());
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                super.onCodeSent(verificationId, token);
                // This SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                // Save verification ID and resending taken so we can use them later
                mVerificationId = verificationId;
                forceResendingToken = token;
                //OTP is sent so hide progress for now
                progressDialog.dismiss();
                //OTP is sent so hide phone ui and show otp ui
                binding.phoneInputRl.setVisibility(View.INVISIBLE);
                binding.otpInputRl.setVisibility(View.VISIBLE);

                //show toast for success sending OTP
                Utils.toast(LoginPhoneActivity.this, "OTP Sent to "+phoneNumberWithCode);

                //Show user a message that Please type the verification code sent to the phone number user has input
                binding.loginLabelTv.setText("Please enter the verification code sent to "+phoneNumberWithCode);
            }
        };
    }

    private void verifyPhoneNumberWithCode(String verificationId, String otp){
        Log.d(TAG, "verifyPhoneNumberWithCode: verification: "+verificationId);
        Log.d(TAG, "verifyPhoneNumberWithCode: otp: "+otp);
        //show Progress
        progressDialog.setMessage("Verifying OTP");
        progressDialog.show();
        //PhoneAuthCredential with verification id and OTP to signIn user with signInWithPhoneAuthCredential
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);

        signInWithPhoneAuthCredential(credential);
    }

    private void resendVerificationCode(PhoneAuthProvider.ForceResendingToken token){
        Log.d(TAG, "resendVerificationCode: ForceResendingToken: "+ token);

        progressDialog.setMessage("Resending OTP to "+phoneNumberWithCode);
        progressDialog.show();

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(firebaseAuth)   //FirebaseAuth instance
                .setPhoneNumber(phoneNumberWithCode)                           //Phone Number with country Code e.g. +91********
                .setTimeout(60L, TimeUnit.SECONDS)                      //Timeout and unit
                .setActivity(this)                                              //Activity (for callback binding)
                .setCallbacks(mCallbacks)
                .setForceResendingToken(token)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential){
        Log.d(TAG, "signInWithPhoneAuthCredential: ");
        progressDialog.setMessage("Logging In");
        //signIn in to firebase auth using Phone credentials
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Log.d(TAG, "onSuccess: ");
                        //SignIn Success , Let's check if new (New Account Register) or existing (Existing Login)
                        if(authResult.getAdditionalUserInfo().isNewUser()){
                            Log.d(TAG, "onSuccess: New User,Account created...");

                            updateUserInfo();
                        }
                        else{
                            Log.d(TAG, "onSuccess: Existing User, Logged In");
                            //New User, Account created. No need to save user info to firebase realtime database, start MainActivity
                            startActivity(new Intent(LoginPhoneActivity.this, MainActivity.class));
                            finishAffinity();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //signIn failed, show exception message
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        Utils.toast(LoginPhoneActivity.this, "Failed to login due to "+e.getMessage());
                    }
                });
    }
    private void updateUserInfo(){
        Log.d(TAG, "updateUserInfo: ");
        progressDialog.setMessage("Saving user info");
        //Let's save user info to Firebase Realtime database key namesShould be same as we done in Register User via Phone and otp

        //get current timestamp e.g. to show user registration data/time
        Long timestamp  = Utils.getTimeStamp();
        String registerUserUid = firebaseAuth.getUid();//get uid of registered user

        //setup data to save in firebase realtime db. most of the data will be empty and will set in edit profile
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("name", "");
        hashMap.put("phoneCode", ""+phoneCode);
        hashMap.put("phoneNumber", ""+phoneNumber);
        hashMap.put("profileImageUrl", "");
        hashMap.put("dob", "");
        hashMap.put("userType", "Phone");//possible values Email/Phone/Google
        hashMap.put("typingTo", "");
        hashMap.put("timestamp", timestamp);
        hashMap.put("onlineStatus", true);
        hashMap.put("email","");
        hashMap.put("uid", registerUserUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(registerUserUid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: User info saved");
                        progressDialog.dismiss();

                        startActivity(new Intent(LoginPhoneActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        Utils.toast(LoginPhoneActivity.this, "Failed to save user info due to "+e.getMessage());
                    }
                });
    }
}