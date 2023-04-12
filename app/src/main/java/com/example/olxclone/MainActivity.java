package com.example.olxclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.olxclone.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    //Firebase Auth for auth related tasks
    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //activity_main.xml = ActivityMainBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance();

        //check if user is logged in or not
        if(firebaseAuth.getCurrentUser() == null){
            //user is not logged in, move to loginOptionActivity
            startLoginOptions();
        }

        showHomeFragment();

        //handle bottomNv item clicks to navigate between fragments
        binding.bottomNv.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                //get id of the menu item clicked
                int itemId = item.getItemId();
                if(itemId == R.id.menu_home){
                    //Home item clicked, show HomeFragment
                    showHomeFragment();
                    return true;
                } else if(itemId == R.id.menu_chats){
                    //Chats item clicked, show HomeFragment
                    if(firebaseAuth.getCurrentUser() == null){
                        Utils.toast(MainActivity.this, "Login Required...");
                        startLoginOptions();

                        return false;
                    }else{
                        showChatsFragment();
                        return true;
                    }

                } else if(itemId == R.id.menu_my_ads){
                    //My Ads item clicked, show HomeFragment
                    if(firebaseAuth.getCurrentUser() == null){
                        Utils.toast(MainActivity.this, "Login Required...");
                        startLoginOptions();
                        return false;
                    }else{
                        showMyAdsFragment();
                        return true;
                    }

                } else if(itemId == R.id.menu_account){
                    //Account item clicked, show HomeFragment
                    if(firebaseAuth.getCurrentUser() == null){
                        Utils.toast(MainActivity.this, "Login Required...");
                        startLoginOptions();
                        return false;
                    }else{
                        showAccountFragment();
                        return true;
                    }

                } else{
                    return false;
                }
            }
        });
    }
    private void showHomeFragment(){
        //change toolbar textview text/title to Home
        binding.toolbarTitleTv.setText(R.string.home);

        //Show HomeFragment
        HomeFragment fragment = new HomeFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), fragment, "HomeFragment");
        fragmentTransaction.commit();
    }
    private void showChatsFragment(){
        //change toolbar textview text/title to Chats
        binding.toolbarTitleTv.setText(R.string.chats);

        //Show ChatsFragment
        ChatsFragment fragment = new ChatsFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), fragment, "ChatsFragment");
        fragmentTransaction.commit();
    }
    private void showMyAdsFragment(){
        //change toolbar textview text/title to My Ads
        binding.toolbarTitleTv.setText(R.string.my_ads);

        //Show MyAdsFragment
        MyAdsFragment fragment = new MyAdsFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), fragment, "MyAdsFragment");
        fragmentTransaction.commit();
    }
    private void showAccountFragment(){
        //change toolbar textview text/title to Account
        binding.toolbarTitleTv.setText(R.string.account);

        //Show AccountFragment
        AccountFragment fragment = new AccountFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(), fragment, "AccountFragment");
        fragmentTransaction.commit();
    }

    private void startLoginOptions(){
        startActivity(new Intent(this,LoginOptionActivity.class));
    }

}