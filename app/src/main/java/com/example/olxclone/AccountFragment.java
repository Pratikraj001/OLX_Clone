package com.example.olxclone;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.olxclone.databinding.FragmentAccountBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;

    private static final String TAG = "ACCOUNT_TAG";

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;

    //Context for this fragment class
    private Context mContext;

    private ProgressDialog progressDialog;

    @Override
    public void onAttach(@NonNull Context context){

        mContext = context;
        super.onAttach(context);
    }

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAccountBinding.inflate(LayoutInflater.from(mContext), container, false);


        return  binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @NonNull Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        //init/setup ProgressDialog to show while account verification
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //get instance of Firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        loadMyInfo();

        //handle logoutBtn click, logout user and start MainActivity
        binding.logoutCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth.signOut();

                startActivity(new Intent(mContext, MainActivity.class));
                getActivity().finishAffinity();

            }
        });
        // handle editProfileCv Click, Start ProfileEditActivity
        binding.editProfileCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(mContext, ProfileEditActivity.class));
            }
        });

        //handle changePassword click, Start ChangePasswordActivity
        binding.changePasswordCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(mContext, ChangePasswordActivity.class));
            }
        });

        //handle verifyAccount click, start ChangePasswordActivity
        binding.verifyAccountCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                verifyAccount();
            }
        });

    }
    private void loadMyInfo(){
        DatabaseReference ref  = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get user info
                        String dob = ""+snapshot.child("dob").getValue();
                        String email = ""+snapshot.child("email").getValue();
                        String name = ""+snapshot.child("name").getValue();
                        String phoneCode = ""+snapshot.child("phoneCode").getValue();
                        String phoneNumber = ""+snapshot.child("phoneNumber").getValue();
                        String profileImageUrl = ""+snapshot.child("profileImageUrl").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();
                        String userType = ""+snapshot.child("userType").getValue();

                        //concatenate phone code and phone number to make full phone number
                        String phone = phoneCode + phoneNumber;

                        //to avoid null or format exception
                        if(timestamp.equals("null")){
                            timestamp = "0";
                        }

                        //format timestamp to dd/MM/yyyy
                        String formattedData = Utils.formatTimestampData(Long.parseLong(timestamp));

                        //set data to UI
                        binding.emailTv.setText(email);
                        binding.nameTv.setText(name);
                        binding.dobTv.setText(dob);
                        binding.phoneTv.setText(phone);
                        binding.memberSinceTv.setText(formattedData);

                        //check user type i.e. Email/Phone/Google In case of Phone & Google account is already verified but in case of Email account user have to verify
                        if(userType.equals("Email")){
                            //userType is Email, have to check if verified or not
                            boolean isVerified = firebaseAuth.getCurrentUser().isEmailVerified();
                            if(isVerified){
                                //verified, hide the verify Account option
                                binding.verifyAccountCv.setVisibility(View.GONE);
                                binding.verificationTv.setText("Verified");
                            }
                            else {
                                //Not verified, Show the verify Account option
                                binding.verifyAccountCv.setVisibility(View.VISIBLE);
                                binding.verificationTv.setText("Not Verified");
                            }
                        }
                        else{
                            //userType is Google or Phone, no need to check if verified or not as it is already verified
                            binding.verifyAccountCv.setVisibility(View.GONE);
                            binding.verificationTv.setText("Verified");
                        }
                        try{
                            //set profile image to profileIv
                            Glide.with(mContext)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_person_white)
                                    .into(binding.profileIv);
                        }
                        catch (Exception e){
                            Log.e(TAG,"onDataChange", e);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void verifyAccount(){
        Log.d(TAG, "verifyAccount: ");

        //show progress
        progressDialog.setMessage("Sending Account verification instruction to your mail.");
        progressDialog.show();

        //send account/email verification instruction to the registered email.
        firebaseAuth.getCurrentUser().sendEmailVerification()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //instruction sent, check email, sometimes it goes in spam folder so if not in inbox check your spam folder
                        Log.d(TAG, "onSuccess: Sent");
                        progressDialog.dismiss();
                        Utils.toast(mContext, "Account verification instruction sent to your email");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Failed to send instruction
                        Log.d(TAG, "onFailure: ",e);
                        progressDialog.dismiss();
                        Utils.toast(mContext, "Failed due to "+e.getMessage());
                    }
                });
    }
}