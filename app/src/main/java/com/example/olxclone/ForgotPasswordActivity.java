package com.example.olxclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import com.example.olxclone.databinding.ActivityForgotPasswordBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;

    private static final String TAG = "FORGOT_PASS_TAG";

    //Firebase  Auth for auth related tasks
    private FirebaseAuth firebaseAuth;

    //ProgressDialog to show while sending password recovery instructions
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater()) ;
        setContentView(binding.getRoot());

        //setup progressDialog to show sending password recovery instruction
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        // get instance of firebase Auth for Auth related tasks
        firebaseAuth = firebaseAuth.getInstance();

        //handle toolBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });

    }

    private String email = "";

    public void validateData(){
        Log.d(TAG, "validateData: ");
        // input data
        email = binding.emailEt.getText().toString().trim();

        Log.d(TAG, "validateData: email: "+email);
        //validate data
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //invalid email pattern, show error in emailEt
            binding.emailEt.setError("Invalid Email Pattern!");
            binding.emailEt.requestFocus();
        }
        else{
            //email pattern is valid, send password recovery instructions
            sendPasswordRecoveryInstruction();

        }

    }
    private void sendPasswordRecoveryInstruction(){
        Log.d(TAG, "sendPasswordRecoveryInstruction: ");
        //show progress
        progressDialog.setMessage("Sending password recovery instructions to "+email);
        progressDialog.show();

        //show password recovery instruction, pass the input email as param
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // instructions sent, check email, sometimes it goes in spam folder so if not in inbox check your spam folder
                        progressDialog.dismiss();
                        Utils.toast(ForgotPasswordActivity.this, "Instructions to reset password is sent to "+email);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to send instructions
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        Utils.toast(ForgotPasswordActivity.this, "Failed to send due to "+e.getMessage());
                    }
                });
    }
}