package com.theeconomist.downloader.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.theeconomist.downloader.R;
import com.theeconomist.downloader.dialog.NormalAskDialog;

public class SplashActivity extends BaseActivity {

    private static final int REQUEST_SDCARD_CODE = 0x0002;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ( ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED//读取存储卡权限
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_SDCARD_CODE);
            } else {
                toHomeActivity();
            }
        } else {
            toHomeActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SDCARD_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toHomeActivity();
            } else {
                NormalAskDialog normalAskDialog = new NormalAskDialog(this);
                normalAskDialog.show();
                normalAskDialog.setData("请同意读取存储卡权限", "退出", "好的", false);
                normalAskDialog.setOnActionListener(new NormalAskDialog.OnActionListener() {
                    @Override
                    public void onLeftAction() {
                        SplashActivity.this.finish();
                    }

                    @Override
                    public void onRightAction() {
                        ActivityCompat.requestPermissions(SplashActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_SDCARD_CODE);
                        toHomeActivity();
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {

    }

    private void toHomeActivity() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(SplashActivity.this, MainActivity.class);
                SplashActivity.this.finish();
            }
        }, 3000);
    }
}
