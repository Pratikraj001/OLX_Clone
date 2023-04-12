package com.example.olxclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import com.example.olxclone.databinding.ActivityRegisterEmailBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterEmailActivity extends AppCompatActivity {

    private ActivityRegisterEmailBinding binding;

    private static final String TAG = "REGISTER_TAG";

    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //activity register_email.xml =  ActivityRegisterEmailBinding
        binding = ActivityRegisterEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get instance of firebase auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle toolbarBackBtn click go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //handle haveAccountTv click go-back-to LoginEmailActivity
        binding.haveAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();

            }
        });

        //handle registerBtn click, start user registration
        binding.registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }
    private String email, password, cPassword;
    private void validateData(){
        //input data
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString().trim();
        cPassword = binding.cPasswordEt.getText().toString().trim();

        Log.d(TAG, "validateDate: email: "+email);
        Log.d(TAG, "validateDate: password: "+password);
        Log.d(TAG, "validateDate: cPassword: "+cPassword);

        //validate data
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //email pattern is invalid. show error
            binding.emailEt.setError("Invalid Email Pattern");
            binding.emailEt.requestFocus();
        }else if(password.isEmpty()){
            //password is not entered. show error
            binding.passwordEt.setError("Enter Password");
            binding.passwordEt.requestFocus();

        }else if(!password.equals(cPassword)){
            //password and confirm password is not same. show error
            binding.cPasswordEt.setError("Password doesn't match");
            binding.cPasswordEt.requestFocus();

        }else{
            //all data is valid, start sign-up
            registerUser();
        }
    }
    private void registerUser(){
        progressDialog.setMessage("Creating Account");
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                Log.d(TAG, "onSuccess: Register Success");
                //SignIn Success , Let's check if new (New Account Register) or existing (Existing Login)
                if(authResult.getAdditionalUserInfo().isNewUser()){
                    Log.d(TAG, "onSuccess: New User,Account created...");

                    updateUserInfo();
                }
                else{
                    Log.d(TAG, "onSuccess: Existing User, Logged In");
                    //New User, Account created. No need to save user info to firebase realtime database, start MainActivity
                    startActivity(new Intent(RegisterEmailActivity.this, MainActivity.class));
                    finishAffinity();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "onFailure: ", e);
                Utils.toast(RegisterEmailActivity.this, "Failed due to "+e.getMessage());
                progressDialog.dismiss();
            }
        });

    }
    private void updateUserInfo(){
        Log.d(TAG, "updateUserInfo: ");
        //change progress dialog message
        progressDialog.setMessage("Saving User Info");
        //Let's save user info to Firebase Realtime database key namesShould be same as we done in Register User via email and Google

        //get current timestamp e.g. to show user registration data/time
        Long timestamp  = Utils.getTimeStamp();
        String registerUserEmail = firebaseAuth.getCurrentUser().getEmail();
        String registerUserUid = firebaseAuth.getUid();

        //setup data to save in firebase realtime db. most of the data will be empty and will set in edit profile
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("name", "");
        hashMap.put("phoneCode", "");
        hashMap.put("phoneNumber", "");
        hashMap.put("profileImageUrl", "");
        hashMap.put("dob", "");
        hashMap.put("userType", "Email");
        hashMap.put("typingTo", "");
        hashMap.put("timestamp", timestamp);
        hashMap.put("onlineStatus", true);
        hashMap.put("email", registerUserEmail);
        hashMap.put("uid", registerUserUid);

        //set data to firebase db
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(registerUserUid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Firebase db save success
                        Log.d(TAG, "onSuccess: Info saved...");
                        progressDialog.dismiss();

                        startActivity(new Intent(RegisterEmailActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Firebase db save failed
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        Utils.toast(RegisterEmailActivity.this, "Failed to save info due to "+e.getMessage());
                    }
                });
    }

}