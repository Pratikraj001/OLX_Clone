package com.example.olxclone;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class AccountFragment extends Fragment {

    private FragmentAccountBinding binding;

    private static final String TAG = "ACCOUNT_TAG";

    private FirebaseAuth firebaseAuth;

    private Context mContext;

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

        firebaseAuth = FirebaseAuth.getInstance();

        loadMyInfo();

        binding.logoutCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth.signOut();

                startActivity(new Intent(mContext, MainActivity.class));
                getActivity().finishAffinity();

            }
        });

        binding.editProfileCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(mContext, ProfileEditActivity.class));
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
                                //verified
                                binding.verificationTv.setText("Verified");
                            }
                            else {
                                //Not verified
                                binding.verificationTv.setText("Not Verified");
                            }
                        }
                        else{
                            //userType is Google or Phone, no need to check if verified or not as it is already verified
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
}