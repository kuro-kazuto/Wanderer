//AKTIVITY SPLASH SCREEN
package com.untirta.wanderer;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;


public class Splash extends AppCompatActivity {

    //1000=1 detik

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);



        setContentView(R.layout.activity_splash);
        int waktu_loading = 3000;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //setelah loading maka akan langsung berpindah ke home activity
                Intent start=new Intent(Splash.this, MainActivity.class);
                startActivity(start);
                finish();

            }
        }, waktu_loading);
    }
}