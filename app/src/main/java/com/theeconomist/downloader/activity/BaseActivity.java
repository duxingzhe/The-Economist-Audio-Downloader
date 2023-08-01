package com.theeconomist.downloader.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;

import com.theeconomist.downloader.R;
import com.theeconomist.downloader.utils.CommonUtil;

import org.greenrobot.eventbus.EventBus;

public class BaseActivity extends Activity {

    LinearLayout lySystemParent;

    ImageView ivLine;

    LinearLayout lySystemBar;

    TextView mtvTitle;

    ImageView mivBack;

    ImageView mivRight;

    TextView tvRight;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //5.0 全透明实现
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE );
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        } else {
            //透明状态栏
            //4.4全透明
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

        lySystemParent=findViewById(R.id.ly_system_parent);

        ivLine=findViewById(R.id.iv_line);

        lySystemBar=findViewById(R.id.ly_system_bar);
        mtvTitle=findViewById(R.id.tv_title);
        mivBack=findViewById(R.id.iv_back);
        mivRight=findViewById(R.id.iv_right);
        tvRight=findViewById(R.id.tv_right);
        lySystemBar = (LinearLayout) findViewById(R.id.ly_system_bar);
        if(lySystemBar != null) {
            initSystembar(lySystemBar);
        }
        if(mivBack != null) {
            mivBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickBack();
                }
            });
        }
        if(mivRight != null) {
            mivRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickMenu();
                }
            });
        }

        if(tvRight != null) {
            tvRight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickTxtMenu();
                }
            });
        }

    }

    public void setTitleTrans(int color) {
        if(lySystemParent != null) {
            lySystemParent.setBackgroundColor(getResources().getColor(color));
        }
    }

    public void setTitleLine(int color) {
        if(ivLine != null) {
            ivLine.setBackgroundColor(getResources().getColor(color));
        }
    }

    public void onClickMenu(){

    }

    public void onClickBack(){
        finish();
        overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
    }

    public void onClickTxtMenu(){

    }

    public void initSystembar(View lySystemBar) {
        if (lySystemBar != null) {
            lySystemBar.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) lySystemBar.getLayoutParams();
            lp.height = CommonUtil.getStatusHeight(this);
            lySystemBar.requestLayout();
        }
    }


    /**
     * 设置标题
     * @param title
     */
    public void setTitle(String title) {
        if(mtvTitle != null) {
            mtvTitle.setText(title);
        }
    }

    /**
     * 显示返回图标
     */
    public void setBackView() {
        if(mivBack != null) {
            mivBack.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 显示返回图标
     */
    public void setBackView(int resId) {
        if(mivBack != null) {
            mivBack.setVisibility(View.VISIBLE);
            mivBack.setImageResource(resId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public static void startActivity(Context context, Class clz) {
        Intent intent = new Intent(context, clz);
        context.startActivity(intent);
        ((Activity)context).overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);

    }

    public static void startActivity(Context context, Class clz, Bundle bundle) {
        Intent intent = new Intent(context, clz);
        intent.putExtras(bundle);
        context.startActivity(intent);
        ((Activity)context).overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);

    }

    public void onBack(View view) {
        overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
    }

    @Override
    public void onBackPressed() {
        this.finish();
        overridePendingTransition(R.anim.in_from_left, R.anim.out_to_right);
    }

}
