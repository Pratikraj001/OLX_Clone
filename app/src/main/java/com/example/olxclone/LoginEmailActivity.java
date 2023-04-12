package com.example.olxclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import com.example.olxclone.databinding.ActivityLoginEmailBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;



public class LoginEmailActivity extends AppCompatActivity {


    private ActivityLoginEmailBinding binding;

    private static final String TAG = "LOGIN_TAG";

    private ProgressDialog progressDialog;

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //activity login email.xml = ActivityLoginEmailBlinding
        binding = ActivityLoginEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        //init setup ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle toolbarBackBtn click go back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //handle noAccountTv click, open RegisterEmailActivity to register user with Email & Password
        binding.noAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginEmailActivity.this, RegisterEmailActivity.class));

            }
        });

        //handle forgetPasswordTv click, open ForgotPasswordActivity to send password recovery instructions to registered email
        binding.forgotPasswordTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginEmailActivity.this, ForgotPasswordActivity.class));
            }
        });

        //handle loginBtn click, start login
        binding.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }

    private String email, password;

    private void validateData(){
        //input data
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString();

        Log.d(TAG, "validateDate: email: "+email);
        Log.d(TAG, "validateDate: password: "+password);

        //validate data
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //email pattern is invalid, show error
            binding.emailEt.setError("Invalid Email");
            binding.emailEt.requestFocus();
        } else if (password.isEmpty()) {
            //password is not entered, show error
            binding.passwordEt.setError("Enter Password");
            binding.passwordEt.requestFocus();
        }else{
            //email pattern is valid and password is enter login
            loginUser();
        }
    }

    private void loginUser(){
        //show progress
        progressDialog.setMessage("Logging In");
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {

                Log.d(TAG, "onSuccess: Logged In...");
                progressDialog.dismiss();

                startActivity(new Intent(LoginEmailActivity.this, MainActivity.class));
                finishAffinity();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Log.e(TAG, "onFailure: ",e);
                Utils.toast(LoginEmailActivity.this, "Failed due to "+e.getMessage());
                progressDialog.dismiss();
            }
        });
    }
}