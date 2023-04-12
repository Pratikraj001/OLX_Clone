package com.example.olxclone;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.bumptech.glide.Glide;
import com.example.olxclone.databinding.ActivityProfileEditBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class ProfileEditActivity extends AppCompatActivity {

    private ActivityProfileEditBinding binding;

    private FirebaseAuth firebaseAuth;
    //ProgressDialog to show while profile update
    private ProgressDialog progressDialog;

    private Uri imageUri = null;

    private String myUserType = "";

    private static final String TAG = "PROFILE_EDIT_TAG";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //activity_profile_edit.xml = ActivityProfileEditBinding
        binding = ActivityProfileEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        //ProgressDialog to show while profile update
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait...");
        progressDialog.setCanceledOnTouchOutside(false);

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        loadMyInfo();

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        //handle profileImagePickFab click, show image pick popup menu
        binding.profileImagePickFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imagePickDialog();
            }
        });

        //handle updateBtn click, validate data
        binding.updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                validateData();
            }
        });
    }

    private String name="";
    private String dob = "";
    private String email = "";
    private String phoneCode = "";
    private String phoneNumber = "";
    private void validateData(){
        //input data
        name = binding.nameEt.getText().toString().trim();
        dob = binding.dobEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();
        phoneCode = binding.countryCodePicker.getSelectedCountryCodeWithPlus();
        phoneNumber = binding.phoneNumberEt.getText().toString().trim();
        //validate data
        if(imageUri == null){
            //no image to storage, just update db
            updateProfileDb(null);
        }
        else{
            //image need to upload to storage, first upload image then update db
            uploadProfileImageStorage();
        }
    }

    private void uploadProfileImageStorage(){
        Log.d(TAG, "uploadProfileImageStorage: ");
        //show progress
        progressDialog.setMessage("Uploading user profile image...");
        progressDialog.show();
        //setup image name and path e.g. UserImages/profile_userUid
        String filePathAndName = "UserImages/"+"profile_"+firebaseAuth.getUid();
        //Storage reference to upload image
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        ref.putFile(imageUri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progress = (100.0*snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        Log.d(TAG, "onProgress: "+progress);

                        progressDialog.setMessage("Uploading profile image. Progress: "+ (int)progress+"%");
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //Image uploaded successfully, get url of upload image
                        Log.d(TAG, "onSuccess: Uploaded");

                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();

                        while(uriTask.isSuccessful());

                        String uploadedImageUrl = uriTask.getResult().toString();

                        updateProfileDb(uploadedImageUrl);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Failed to upload image
                        Log.e(TAG, "onFailure: ",e);
                        progressDialog.dismiss();
                        Utils.toast(ProfileEditActivity.this, "Failed to upload profile image due to "+e.getMessage());
                    }
                });
    }

    private void updateProfileDb(String imageUrl){
        //show progress
        progressDialog.setMessage("Updating user info...");
        progressDialog.show();
        //setup data in hashmap to update to firebase db
        HashMap<String , Object> hashMap = new HashMap<>();
        hashMap.put("name", "" + name);
        hashMap.put("dob", "" + dob);

        if(imageUrl != null){//update profileImageUrl in db only if uploaded image url is not null
            hashMap.put("profileImageUrl", ""+imageUrl);
        }

        // if user type is Phone then allow to update email otherwise (in case of Google or email) allow to update Phone
        if(myUserType.equalsIgnoreCase("Phone")){
            //user type is Phone allow to update Email not Phone
            hashMap.put("email", ""+email);
        }
        else if(myUserType.equalsIgnoreCase("Email") || myUserType.equalsIgnoreCase("Google")){
            hashMap.put("phoneCode", phoneCode);
            hashMap.put("phoneNumber", phoneNumber);
        }

        //Database reference of user to update info
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid())
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //Update successfully
                        Log.d(TAG, "onSuccess: Info updated");
                        progressDialog.dismiss();
                        Utils.toast(ProfileEditActivity.this, "Profile Updated...");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to update
                        Log.e(TAG, "onFailure: ", e);
                        progressDialog.dismiss();
                        Utils.toast(ProfileEditActivity.this, "Failed to update info due to "+e.getMessage());
                    }
                });
    }


    private void loadMyInfo() {
        Log.d(TAG, "loadMyInfo: ");
        //Reference of current user info in firebase realtime database to get user info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get user info, spellings should be same as in firebase realtime database
                        String dob = ""+snapshot.child("dob").getValue();
                        String email = ""+snapshot.child("email").getValue();
                        String name = ""+snapshot.child("name").getValue();
                        String phoneCode = ""+snapshot.child("phoneCode").getValue();
                        String phoneNumber = ""+snapshot.child("phoneNumber").getValue();
                        String profileImageUrl = ""+snapshot.child("profileImageUrl").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();
                        myUserType = ""+snapshot.child("userType").getValue();
                        //concatenate phone code and phone number to make full phone number
                        String phone = phoneCode+phoneNumber;
                        //check User Type , if Email/google. Don't allow user to edit/update email
                        if(myUserType.equalsIgnoreCase("Email") || myUserType.equalsIgnoreCase("Google")){
                            //user type is email or google.Don't allow to edit email.
                            binding.emailTil.setEnabled(false);
                            binding.emailEt.setEnabled(false);
                        }
                        else{
                            //user type is Phone. Don't allow to edit phone
                            binding.phoneNumberTil.setEnabled(false);
                            binding.phoneNumberEt.setEnabled(false);
                            binding.countryCodePicker.setEnabled(false);
                        }
                        //set data to UI
                        binding.emailEt.setText(email);
                        binding.nameEt.setText(name);
                        binding.phoneNumberEt.setText(phone);

                        try{
                            int phoneCodeInt = Integer.parseInt(phoneCode.replace("+", "")); // e.g. +91 --> 91
                            binding.countryCodePicker.setCountryForPhoneCode(phoneCodeInt);
                        }
                        catch (Exception e){
                            Log.e(TAG, "onDataChange", e);
                        }

                        try {
                            Glide.with(ProfileEditActivity.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_person_white)
                                    .into(binding.profileIv);
                        }
                        catch (Exception e){
                            Log.e(TAG, "onDataChange: ", e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void imagePickDialog(){
        //init popup menu param 1 is context and param 2 is the UI View (profileImagePickFab) to above/below we need to show popup menu
        PopupMenu popupMenu = new PopupMenu(this, binding.profileImagePickFab);
        //add menu items to our popup menu Param#1 is GroupID, Param#2 is ItemID, Param#3 is OrderID, Param#4 is Menu Item Title
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");
        //show Popup Menu
        popupMenu.show();
        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //get the id of the menu item clicked
                int itemId = menuItem.getItemId();
                if(itemId == 1){
                    //camera is clicked we need to check if we have permission of Camera, Storage before launching Camera to capture image
                    Log.d(TAG, "onMenuItemClick: Camera Clicked, check if camera permission(s) granted or not");

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                        //Device version is TIRAMISU or above.We only need Camera permission
                        requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA});
                    }
                    else{
                        //Device version is below TIRAMISU. We need Camera & Storage permissions
                        requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
                    }
                }
                else if(itemId == 2){
                    Log.d(TAG, "onMenuItemClick: check if storage permission is granted or not");
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                        pickImageGallery();
                    }
                    else{
                        requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
                return false;
            }
        });
    }

    private ActivityResultLauncher<String[]>requestCameraPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    Log.d(TAG, "onActivityResult: "+result.toString());
                    //let's check if permissions are granted or not
                    boolean areAllGranted = true;
                    for(Boolean isGranted: result.values()){
                        areAllGranted = areAllGranted && isGranted;
                    }

                    if (areAllGranted){
                        //All Permissions Camera, Storage are granted, we can now launch camera to capture image
                        Log.d(TAG, "onActivityResult: All Granted e.g. Camera , Storage");
                        pickImageCamera();
                    }
                    else{
                        //Camera or storage or Both permissions are denied, Can't launch camera to capture image
                        Log.d(TAG, "onActivityResult: All or either one is denied");
                        Utils.toast(ProfileEditActivity.this, "Camera or Storage or both permissions denied...");
                    }
                }
            });

    private ActivityResultLauncher<String> requestStoragePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG, "onActivityResult: isGranted: "+isGranted);
                    //let's check if permission is granted or not
                    if(isGranted){
                        //Storage Permission granted, we can now launch gallery to pick image
                        pickImageGallery();
                    }
                    else{
                        //Storage Permission denied, we can't launch gallery to pick image
                        Utils.toast(ProfileEditActivity.this, "Storage permission denied...!");
                    }
                }

            }
    );

    private void pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "TEMP_TITLE");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"TEMP_DESCRIPTION");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //check if image is captured or not
                    if(result.getResultCode() == Activity.RESULT_OK){
                        //Image Captured, we have image in imageUri as asigned in pickImageCamera()
                        Log.d(TAG, "onActivityResult: Image Captured: "+imageUri);

                        //set to profileIv
                        try{
                            Glide.with(ProfileEditActivity.this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.ic_person_white)
                                    .into(binding.profileIv);
                        }catch (Exception e){
                            Log.e(TAG, "onActivityResult: ", e);
                        }
                    }
                    else{
                        Utils.toast(ProfileEditActivity.this, "Cancelled...");
                    }
                }
            }
    );

    private void pickImageGallery(){
        Log.d(TAG, "pickImageGallery: ");

        Intent intent = new Intent(Intent.ACTION_PICK);

        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);

    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode() == Activity.RESULT_OK){

                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: Image Picked From Gallery: "+imageUri);

                        try{
                            Glide.with(ProfileEditActivity.this)
                                    .load(imageUri)
                                    .placeholder(R.drawable.ic_person_white)
                                    .into(binding.profileIv);
                        }
                        catch (Exception e){
                            Log.d(TAG, "onActivityResult: ",e);
                        }
                    }
                    else{
                        Utils.toast(ProfileEditActivity.this,"Cancelled...");
                    }
                }
            }
    );

    
}