package com.example.olxclone;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.olxclone.databinding.ActivityLoginOptionBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;


public class LoginOptionActivity extends AppCompatActivity {

    private ActivityLoginOptionBinding binding;

    private static final String TAG = "LOGIN_OPTIONS_TAG";

    private ProgressDialog progressDialog;

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //activity_login_options.xml = ActivityLoginOptionsBinding
        binding = ActivityLoginOptionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init/setup ProgressDialog to show while sign-up
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        //Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        //handle closeBtn click, goBack
//        binding.closeBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onBackPressed();
//            }
//
//        });
        //handle loginEmailBtn click, open LoginEmailActivity to login with Email & Password
        binding.loginEmailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginOptionActivity.this,LoginEmailActivity.class));
            }
        });
        //handle loginEmailBtn click, open LoginPhoneActivity to login with number & OTP
        binding.loginPhoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginOptionActivity.this, LoginPhoneActivity.class));
            }
        });
        //handle loginGoogleBtn click, begin google signIn
        binding.loginGoogleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginGoogleLogin();
            }
        });
    }

    private void beginGoogleLogin(){
        Intent googleSignInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInnARL.launch(googleSignInIntent);
    }

    private ActivityResultLauncher<Intent> googleSignInnARL = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    Log.d(TAG, "onActivityResult: ");

                    if(result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

                        try{
                            //Google Sign In was successful, authenticate with firebase
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            Log.d(TAG, "onActivityResult: Account ID: "+account.getId());
                            firebaseAuthWithGoogleAccount(account.getIdToken());
                        }
                        //Google sign in failed, update UI appropriately
                        catch (Exception e){
                            Log.e(TAG, "onActivityResult: ",e);
                        }
                    }
                    else{
                        //Cancelled from google SignIn options/confirmation dialog
                        Log.d(TAG, "onActivityResult: Cancelled");
                        Utils.toast(LoginOptionActivity.this, "Cancelled...");
                    }
                }
            }
    );

    private void firebaseAuthWithGoogleAccount(String idToken){
        Log.d(TAG, "firebaseAuthWithGoogleAccount: idToken: "+idToken);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //SignIn Success, Let's check if the user is new(New Account Register) or existing (Existing Login)
                        if(authResult.getAdditionalUserInfo().isNewUser()){
                            Log.d(TAG, "onSuccess: New User, Account created...");
                            //New user, Account created. Let's save user info to firebase realtime database
                            updateUserInfoDb();
                        }
                        else{
                            Log.d(TAG, "onSuccess: Existing User, Logged ");
                            startActivity(new Intent(LoginOptionActivity.this, MainActivity.class));
                            finishAffinity();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ",e);
                    }
                });
    }
    private void updateUserInfoDb(){
        Log.d(TAG, "updateUserInfoDb: ");

        progressDialog.setMessage("Saving User Info");
        progressDialog.show();

        //get current timestamp e.g. to show user registration data/time
        Long timestamp  = Utils.getTimeStamp();
        String registerUserEmail = firebaseAuth.getCurrentUser().getEmail();//get email of registered user
        String registerUserUid = firebaseAuth.getUid();//get uid of registered user
        String name = firebaseAuth.getCurrentUser().getDisplayName();

        //setup data to save in firebase realtime db. most of the data will be empty and will set in edit profile
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("name", ""+name);
        hashMap.put("phoneCode", "");
        hashMap.put("phoneNumber", "");
        hashMap.put("profileImageUrl", "");
        hashMap.put("dob", "");
        hashMap.put("userType", "Google");//possible values Email/Phone/Google
        hashMap.put("typingTo", "");
        hashMap.put("timestamp", timestamp);
        hashMap.put("onlineStatus", true);
        hashMap.put("email", registerUserEmail);
        hashMap.put("uid", registerUserUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(registerUserUid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: User info saved...");
                        progressDialog.dismiss();

                        startActivity(new Intent(LoginOptionActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        Utils.toast(LoginOptionActivity.this, "Failed to save user info due to "+e.getMessage());
                    }
                });
    }
}