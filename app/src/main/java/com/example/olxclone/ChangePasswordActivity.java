package com.example.olxclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.olxclone.databinding.ActivityChangePasswordBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {


    private ActivityChangePasswordBinding binding;

    //TAG for logs in logcat
    private static final String TAG = "CHANGE_PASS_TAG";

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    //ProgressDialog to show while sending password recovery instructions
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //init view binding.. activity_change_password.xml = ActivityChangePasswordBinding
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        //init/setup ProgressDialog to show while changing password
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle toolbarBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //handle submitBtn Click, validate data to start password change
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }

    private String currentPassword = "";
    private String newPassword = "";
    private String confirmNewPassword = "";

    private void validateData(){
        Log.d(TAG, "validateData: ");

        //input data
        currentPassword = binding.currentPasswordEt.getText().toString();
        newPassword = binding.newPasswordEt.getText().toString();
        confirmNewPassword = binding.confirmNewPasswordEt.getText().toString();

        Log.d(TAG, "validateData: currentPassword: "+currentPassword);
        Log.d(TAG, "validateData: newPassword: "+newPassword);
        Log.d(TAG, "validateData: confirmNewPassword: "+confirmNewPassword);

        //validate data
        if(currentPassword.isEmpty()){
            //Current Password Field (currentPasswordEt) is empty, show error in currentPasswordEt
            binding.currentPasswordEt.setError("Enter current password!");
            binding.currentPasswordEt.requestFocus();
        } else if (newPassword.isEmpty()) {
            //New Password Field (newPasswordEt) is empty, show in newPasswordEt
            binding.newPasswordEt.setError("Enter new password!");
            binding.newPasswordEt.requestFocus();
        } else if (confirmNewPassword.isEmpty()) {
            //Confirm New Password Field(ConfirmNewPasswordEt) is empty, show error in confirmNewPasswordEt
            binding.confirmNewPasswordEt.setError("Enter Confirm Password");
            binding.confirmNewPasswordEt.requestFocus();
        } else if (!newPassword.equals(confirmNewPassword)) {
            //password in newPasswordEt & ConfirmNewPassword doesn't match, show error in confirmNewPasswordEt
            binding.confirmNewPasswordEt.setError("Password doesn't match!");
            binding.confirmNewPasswordEt.requestFocus();
        }else{
            authenticateUserForUpdatePassword();
        }
    }

    private void authenticateUserForUpdatePassword(){
        Log.d(TAG, "authenticateUserForUpdatePassword: ");

        progressDialog.setMessage("Authenticating User");
        progressDialog.show();

        //before changing password re-authentication the user to check if the user has entered correct current password
        AuthCredential authCredential = EmailAuthProvider.getCredential(firebaseUser.getEmail(), currentPassword);
        firebaseUser.reauthenticate(authCredential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //successfully authenticated, begin update
                        updatePassword();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        Log.d(TAG, "onFailure: ",e);
                        progressDialog.dismiss();
                        Utils.toast(ChangePasswordActivity.this, "Failed to authenticate due to "+e.getMessage());
                    }
                });
    }

    private void updatePassword(){
        Log.d(TAG, "updatePassword: ");

        //show progress
        progressDialog.setMessage("Updating Password");
        progressDialog.show();

        //begin update password, pass the new password as parameter
        firebaseUser.updatePassword(newPassword)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //password update success, you may do logout and move to login activity if you want
                        progressDialog.dismiss();
                        Utils.toast(ChangePasswordActivity.this, "Password Updated");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //password update failure, show error message
                        Log.e(TAG, "onFailure: ",e);
                        progressDialog.dismiss();
                        Utils.toast(ChangePasswordActivity.this, "Failed to update password due to "+e.getMessage());

                    }
                });

    }
}