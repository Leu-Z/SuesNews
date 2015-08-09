package com.example.lee.suesnews.ui;


import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.extras.toolbar.MaterialMenuIconToolbar;
import com.example.lee.suesnews.R;
import com.example.lee.suesnews.common.NewsTypes;
import com.example.lee.suesnews.ui.fragments.NewsListFragment;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.umeng.update.UmengUpdateAgent;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.OnClickListener;


public class MainActivity extends BaseActivity implements ObservableScrollViewCallbacks {

    private ViewPager mViewPager;
    //新闻列表
    private List<NewsListFragment> mFragmentList;

    private ViewGroup mMainPage;
    private DrawerLayout mDrawerLayout;
    private ViewGroup mDrawer;

    //设置按钮区域
    private View mAppSetting;
    private View mAboutButton;
    private View mShareButton;
    private View mFeedBackButton;

    private MaterialMenuIconToolbar mMaterialMenu;

    PagerSlidingTabStrip mTabs;

    private ViewGroup mContent;

//    private final int CURRENT_VERSION = Build.VERSION.SDK_INT;
//    private final int VERSION_KITKAT = Build.VERSION_CODES.KITKAT;
//    private final int VERSION_LOLLIPOP = Build.VERSION_CODES.LOLLIPOP;

    //侧边栏头部图片
    private ImageView mHeaderImage;

    //标识是否点击过一次back退出
    private boolean mIsExit = false;
    //点击返回键时，延时 TIME_TO_EXIT 毫秒发送此handler重置mIsExit，再其被重置前如果再按一次返回键则退出应用
    private Handler mExitHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mIsExit = false;
        }
    };
    final static int TIME_TO_EXIT = 2000;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initViews();

        initViewPager();

        UmengUpdateAgent.update(this);  //检查更新
    }

    //
    private void initViews() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(getResources().getString(R.string.main_activity_title));
        //取代表原本的 actionbar
        setSupportActionBar(mToolbar);

        //点击toolbar的导航键可以打开抽屉
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //将抽屉打开
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        });

        //新建MaterialMenuIcon对象，并和toolbar进行关联
        mMaterialMenu = new MaterialMenuIconToolbar(this, Color.BLACK, MaterialMenuDrawable.Stroke.THIN) {
            @Override
            public int getToolbarViewId() {
                return R.id.toolbar;
            }
        };
        mMaterialMenu.setNeverDrawTouch(true);

        //状态栏不进行过渡颜色
        mTintManager.setStatusBarTintEnabled(false);

        //设置侧滑菜单的监听
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            //抽屉滑动，出现动画由汉堡变为箭头
            @Override
            public void onDrawerSlide(View view, float v) {
                //主动触发动画效果
                mMaterialMenu.setTransformationOffset(MaterialMenuDrawable.AnimationState.BURGER_ARROW,
                        v);
            }
            //抽屉打开，变成箭头
            @Override
            public void onDrawerOpened(View view) {
                //改变drawable state 带有动画并且有圆形按下的效果
                mMaterialMenu.animatePressedState(intToState(1));
            }
            //抽屉关闭，变成汉堡
            @Override
            public void onDrawerClosed(View view) {
                mMaterialMenu.animatePressedState(intToState(0));
            }

            @Override
            public void onDrawerStateChanged(int i) {

            }
        });

        //content包括PagerSlidingTabStrip和ViewPager
        mContent = (ViewGroup) findViewById(R.id.content);
        mDrawer = (ViewGroup) findViewById(R.id.drawer);
        mMainPage = (ViewGroup) findViewById(R.id.main_page);
        //如果是API19,因为导航栏透明，要让出顶部和底部空间,如果不让出的话，就会充满整个页面
        if (isNavBarTransparent()) {
            mMainPage.setPadding(0, getStatusBarHeight(), 0, getNavigationBarHeight());
            //抽屉没有把状态栏让出，且把状态栏给挡住了
            mDrawer.setPadding(0, 0, 0, getNavigationBarHeight());
        }

        //侧边栏，导航栏的图片
        mHeaderImage = (ImageView) findViewById(R.id.header_img);
        mHeaderImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //resume the click
            }
        });
        //设置，导航栏最底端，点击进入设置活动里
        mAppSetting = (ViewGroup) findViewById(R.id.bottom_drawer);
        mAppSetting.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,SettingActivity.class);
                startActivity(intent);
            }
        });
        //关于，点击进入关于活动
        mAboutButton = findViewById(R.id.drawer_item_about);
        mAboutButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,AboutActivity.class);
                startActivity(intent);
            }
        });
        //分享，启动分享GUI
        mShareButton = findViewById(R.id.drawer_item_share);
        mShareButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showShare(MainActivity.this,MainActivity.this.getResources().getString(R.string.share_app_string));
            }
        });

    }

    //初始化ViewPager ，把PagerSlidingTabStrip和ViewPager进行关联
    private void initViewPager(){

        mTabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mFragmentList = new ArrayList<NewsListFragment>();

        //初始化fragment，新建五个不同新闻内容的碎片实例
        NewsListFragment fragment1 = NewsListFragment.newInstance(NewsTypes.NEWS_TPYE_XXYW);
        NewsListFragment fragment2 = NewsListFragment.newInstance(NewsTypes.NEWS_TPYE_XYKX);
        NewsListFragment fragment3 = NewsListFragment.newInstance(NewsTypes.NEWS_TPYE_KJDT);
        NewsListFragment fragment4 = NewsListFragment.newInstance(NewsTypes.NEWS_TPYE_MTJJ);
        NewsListFragment fragment5 = NewsListFragment.newInstance(NewsTypes.NEWS_TPYE_BMXW);

        mFragmentList.add(fragment1);
        mFragmentList.add(fragment2);
        mFragmentList.add(fragment3);
        mFragmentList.add(fragment4);
        mFragmentList.add(fragment5);


        //初始化ViewPager，ViewPager里面添加碎片时他的适配器比较简单。
        MyViewPagerAdapter adapter = new MyViewPagerAdapter(getSupportFragmentManager(),mFragmentList);
        //设置适配器,把碎片和viewpager进行关联，就是说把碎皮装进viewpager里面
        mViewPager.setAdapter(adapter);
        //设置当前显示的item,即首先显示第一页
        mViewPager.setCurrentItem(0);

        //PagerSlidingTabStrip和viewPager关联
        mTabs.setViewPager(mViewPager);
//        mTabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int i, float v, int i2) {
//
//            }
//
//            @Override
//            public void onPageSelected(int i) {
////                mViewPager.setCurrentItem(i);
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int i) {
//
//            }
//        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //菜单响应事件，跳转到菜单活动
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this,SettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {

    }

    @Override
    public void onDownMotionEvent() {

    }

    /**
     * 根据滑动方向设置ToolBar的显隐
     * @param scrollState   滑动方向
     */
    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
        //如果向上滑且toolbar显示在顶部就隐藏ToolBar
        if (scrollState == ScrollState.UP){
            if (toolbarIsShown()){
                hideToolbar();
            }//如果向下滑且toolbar被隐藏起来了，就显示把他显示出来
        } else if (scrollState == ScrollState.DOWN){
            if (toolbarIsHidden()){
                showToolbar();
            }
        }
    }

    //如果toolbar显示在顶部
    private boolean toolbarIsShown(){
        //toolbar相对于顶部的位置
        return mToolbar.getTranslationY() == 0;
    }

    //如果toolbar显示在顶部上面，就是看不见了
    private boolean toolbarIsHidden(){
        return mToolbar.getTranslationY() == -mToolbar.getHeight();
    }

    private void showToolbar(){
        moveToolbar(0);
    }


    private void hideToolbar(){
        moveToolbar(-mToolbar.getHeight());
    }


    /**
     * 将toolbar移动到某个位置
     * @param toTranslationY 移动到的Y轴位置
     */
    private void moveToolbar(float toTranslationY){
        if(mToolbar.getTranslationY() == toTranslationY){
            return;
        }
        //利用动画过渡移动的过程
        //ValueAnimator 在两个参数之间计算动画的值，持续时间200ms，这里就是在0和-mToolbar之间变化
        final ValueAnimator animator = ValueAnimator.ofFloat(mToolbar.getTranslationY(),toTranslationY).
                setDuration(200);
        //它可以对一个值做动画，监听其动画过程，在动画过程中修改我们的对象的属性值
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            //它会在200ms内将一个值在设定范围内变化，然后动画的每一帧会回调onAnimationUpdate方法
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                 //获得当前动画的进度值,在0和-mToolbar之间变化,当前变化到哪一个数值了
                float translationY = (Float) animator.getAnimatedValue();
                //动态改变Toolbar和Content的垂直位置
                mToolbar.setTranslationY(translationY);
                mContent.setTranslationY(translationY);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mContent.getLayoutParams();
                //这里的
                lp.height = (int) (getScreenHeight() - translationY - getStatusBarHeight()
                        - lp.topMargin);
                //API为19时，再减去导航栏后的高度，这里不合理其实。。。
                /*if (CURRENT_VERSION >= VERSION_KITKAT && VERSION_LOLLIPOP > CURRENT_VERSION){
                    lp.height -= getNavigationBarHeight();
                }*/
                Log.i("TEST", "after" + Float.toString(mToolbar.getHeight()));
                /*当view确定自身已经不再适合现有的区域时，该view本身调用这个方法要求parent view重新调用他的onMeasure onLayout来对重新设置自己位置。
                特别的当view的layoutparameter发生改变，并且它的值还没能应用到view上，这时候适合调用这个方法。*/
                mContent.requestLayout();
            }
        });
        animator.start();
    }

    //按下返回键触发
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 实现点击两次退出程序
     */
    private void exit(){
        if (mIsExit){
            finish();
            System.exit(0);//退出程序，0--正常结束程序1--异常关闭程序；
        }else {
            mIsExit = true;
            Toast.makeText(getApplicationContext(),R.string.click_to_exit,Toast.LENGTH_SHORT).show();
            //两秒内不点击back则重置mIsExit，延时两秒发送消息
            mExitHandler.sendEmptyMessageDelayed(0,TIME_TO_EXIT);
        }
    }
}
