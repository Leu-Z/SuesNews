package com.example.lee.suesnews.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.lee.suesnews.R;
import com.example.lee.suesnews.utils.CommonUtils;

import java.util.Random;

public class WelcomeActivity extends Activity {

    ImageView mBackgroundImage;
    TextView mTitleText;
    TextView mVersionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        //decorView是window中的最顶层view
        View decorView = getWindow().getDecorView();
        //得到系统UI可见性，再到下面进行设置对导航栏进行隐藏。
        int uiOptions = decorView.getSystemUiVisibility();
        int newUiOptions = uiOptions;
        //隐藏导航栏
        newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(newUiOptions);

        mBackgroundImage = (ImageView) findViewById(R.id.image_background);
        Random random = new Random();
        //0到4的整数
        int num = random.nextInt(4);
        int drawable = R.drawable.pic_background_1;
        //随机选择一个图片
        switch (num ){
            case 0:
                break;
            case 1:
                drawable = R.drawable.pic_background_2;
                break;
            case 2:
                drawable = R.drawable.pic_background_3;
                break;
            case 3:
                drawable = R.drawable.pic_background_4;
                break;
        }
        //给背景设置图片
        mBackgroundImage.setImageDrawable(getResources().getDrawable(drawable));
        Animation animImage = AnimationUtils.loadAnimation(this,R.anim.image_welcome);
        //开始动画
        mBackgroundImage.startAnimation(animImage);
        animImage.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //动画结束时打开首页
                startActivity(new Intent(WelcomeActivity.this,MainActivity.class));
                //一个参数是第一个activity进入时的动画，另外一个参数则是第二个activity退出时的动画。
                //它必需紧挨着startActivity()或者finish()函数之后调用"
                overridePendingTransition(R.anim.activity_slide_in, R.anim.no_anim);
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mTitleText = (TextView) findViewById(R.id.title_text);
        mVersionText = (TextView) findViewById(R.id.version_text);
        //获取版本号
        mVersionText.setText("版本："+ CommonUtils.getVersion(this));
    }

    @Override
    public void finish() {
        //销毁图片的缓存
        mBackgroundImage.destroyDrawingCache();
        super.finish();
    }
}
