package com.example.lee.suesnews.ui;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.balysv.materialmenu.extras.toolbar.MaterialMenuIconToolbar;
import com.example.lee.suesnews.R;
import com.example.lee.suesnews.bean.NewsContent;
import com.example.lee.suesnews.biz.NewsItemBiz;
import com.example.lee.suesnews.ui.widget.GestureFrameLayout;
import com.example.lee.suesnews.ui.widget.ObservableScrollView;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;
import com.nineoldandroids.view.ViewHelper;

//实现了一个自定义的ScrollView，具有滚动监控
public class NewsContentActivity extends BaseActivity implements ObservableScrollView.OnScrollChangedListener {

    private static final float MAX_TEXT_SCALE_DELTA = 0.3f;
    private static final boolean TOOLBAR_IS_STICKY = true;

    private final int CURRENT_VERSION = Build.VERSION.SDK_INT;
    private final int DP_TRANS_X = 24;      //标题左移的距离

    private final int VERSION_KITKAT = Build.VERSION_CODES.KITKAT;

    private MaterialMenuIconToolbar mMaterialMenu;
    private NewsContent mNewsContent;
    private String mNewsContentUrl;

    private View mImageView;
    private View mOverlayView;
    private ObservableScrollView mScrollView;

    private TextView mTitleTextView;        //文章标题
    private TextView mContextTextView;      //文章内容
    private TextView mTitleDateTextView;      //文章日期

    private int mActionBarSize;
    private int mFlexibleSpaceImageHeight;

    private int mToolbarColor;

    private GestureFrameLayout gestureFrameLayout;  //滑动返回

    private NewsItemBiz mNewsItemBiz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_content);

        init();
        mNewsItemBiz = new NewsItemBiz(this);
        //通过bundle获取文章内容的url
        mNewsContentUrl = this.getIntent().getBundleExtra("key").getString("url");
        LoadNewsContentTask loadNewsContentTask = new LoadNewsContentTask();
        //根据URL获取新闻内容，再加载进各个view中显示出来
        loadNewsContentTask.execute(mNewsContentUrl);
    }

    private void init() {
        mToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        mToolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_black_18dp));
        ////取代表原本的 actionbar
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NewsContentActivity.this.finish();
            }
        });

        mImageView = findViewById(R.id.title_image);
        mOverlayView = findViewById(R.id.overlay);
        mTitleTextView = (TextView) findViewById(R.id.title_text_view);
        mContextTextView = (TextView) findViewById(R.id.content_text_view);
        mTitleDateTextView = (TextView) findViewById(R.id.title_date);
        //文本显示能超过其显示区域,让超出屏幕的文本自动换行,使用滚动条
        mTitleTextView.setHorizontallyScrolling(true);

        mToolbarColor = getResources().getColor(R.color.primary_color);

        mScrollView = (ObservableScrollView) findViewById(R.id.scrollContent);
        //实现了接口里的实例
        mScrollView.setOnScrollListener(this);



        mActionBarSize = getActionBarSize();
        mFlexibleSpaceImageHeight = getResources().getDimensionPixelSize(R.dimen.flexible_space_image_height);

        gestureFrameLayout = (GestureFrameLayout) findViewById(R.id.news_content_gesture_layout);
        //与Activity绑定
        gestureFrameLayout.attachToActivity(this);

        //PI为19时,因为顶栏透明，要让出顶栏和底栏空间
        if (isNavBarTransparent()) {
            gestureFrameLayout.setPadding(0, getStatusBarHeight(), 0, getNavigationBarHeight());
        }
        ScrollUtils.addOnGlobalLayoutListener(mScrollView, new Runnable() {
            @Override
            public void run() {
                //ScrollView内容滚动时调用，这里是调用ObservableScrollView里的
                //onScrollChanged，在间接调用这里的onScrollChanged
                mScrollView.onScrollChanged(0, 0,0,0);
            }
        });
    }

    //滑动的时候调用
    @Override
    public void onScrollChanged(int scrollX, int scrollY, int oldX, int oldY) {
        // Translate overlay and image
        //可以伸缩的距离
        float flexibleRange = mFlexibleSpaceImageHeight - mActionBarSize;
        //minOverlayTransitionY=-flexibleRange
        int minOverlayTransitionY = mActionBarSize - mOverlayView.getHeight();
        //参数一为要移动的view，参数二为移动距离，相对于0原始位置
        //ScrollUtils.getFloat是返回minOverlayTransitionY, 0之间的一个数字
        //随着滑动，让盖在图片上的view上移，这里是-的，长度是imageView上移的2倍
        //TranslationY是相对于view默认位置的转换
        ViewHelper.setTranslationY(mOverlayView, ScrollUtils.getFloat(-scrollY, minOverlayTransitionY, 0));
        //随着滑动，让图片mImageView上移
        ViewHelper.setTranslationY(mImageView, ScrollUtils.getFloat(-scrollY / 2, minOverlayTransitionY, 0));

        // Change alpha of overlay
        //随着滑动，让盖在图片上的view的alpha值逐渐变大（就是那个渐渐变绿的效果）
        ViewHelper.setAlpha(mOverlayView, ScrollUtils.getFloat((float) scrollY / flexibleRange, 0, 1));
        //而日期显示相反，它是越来越淡
        ViewHelper.setAlpha(mTitleDateTextView, 1 - ScrollUtils.getFloat((float) scrollY / flexibleRange, 0, 1));

        // Scale title text   随着向下滑动，最后一点时刻从1.3变到1
        float scale = 1 + ScrollUtils.getFloat((flexibleRange - scrollY) / flexibleRange, 0, MAX_TEXT_SCALE_DELTA);
        //设置变换中心，以中心为中心
        ViewHelper.setPivotX(mTitleTextView, 0);
        ViewHelper.setPivotY(mTitleTextView, 0);
        //随着滑动，让Title的字体逐渐变小
        ViewHelper.setScaleX(mTitleTextView, scale);
        ViewHelper.setScaleY(mTitleTextView, scale);

        // Translate title text
        //随着滑动，从0到1增大
        float anim = ScrollUtils.getFloat((float) scrollY/flexibleRange,0,1);       //1-0
        //随着滑动,最后一点时刻由大变小，变成可以伸缩的距离
        int maxTitleTranslationY = (int) (mFlexibleSpaceImageHeight - mTitleTextView.getHeight() * scale);
        int maxTitleTranslationX = dp2px(DP_TRANS_X);
        //慢慢变小，最后一下子缩小，但最小不会小于0
        int titleTranslationY = (int) (maxTitleTranslationY - scrollY  - dp2px(12) * (1 - anim));
        //平稳变小，最后变成0
        int titleTranslationX = (int) (maxTitleTranslationX *(1 - anim));
        if (TOOLBAR_IS_STICKY) {
            titleTranslationY = Math.max(0, titleTranslationY);
        }
        //随着滑动，让title上移到ToolBar的位置
        ViewHelper.setTranslationY(mTitleTextView, titleTranslationY );
        ViewHelper.setTranslationX(mTitleTextView, -titleTranslationX);
        ViewHelper.setTranslationY(mTitleDateTextView, titleTranslationY );
        ViewHelper.setTranslationX(mTitleDateTextView, titleTranslationX );

        if (TOOLBAR_IS_STICKY) {
            // Change alpha of toolbar background
            //当滑动到ToolBar的位置时，让ToolBar显示，否则透明
            if (-scrollY + mFlexibleSpaceImageHeight <= mActionBarSize) {
                mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(1, mToolbarColor));
            } else {
                mToolbar.setBackgroundColor(ScrollUtils.getColorWithAlpha(0, mToolbarColor));
            }
        } else {
            // Translate Toolbar
            if (scrollY < mFlexibleSpaceImageHeight) {
                ViewHelper.setTranslationY(mToolbar, 0);
            } else {
                ViewHelper.setTranslationY(mToolbar, -scrollY);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_news_content, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_share) {
            String title = null;
            String url = null;
            if(mNewsContent != null){
                title = mNewsContent.getTitle();
                url = mNewsContent.getUrl();
            }
            //分享该新闻的标题和网页
            showShare(this, title +" 详见：" + url +" \n分享自腾飞新闻： http://fir.im/sues");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * 加载新闻内容的任务
     *根据URL获取新闻内容，再加载进各个view中显示出来
     */
    class LoadNewsContentTask extends AsyncTask<String, Integer,NewsContent > {

        public LoadNewsContentTask() {
            super();

        }

        /**
         *得到当前url的新闻内容
         * @param urls 当前url，由execute提供
         * @return 当前页码的新闻列表,出错返回null
         */
        @Override
        protected NewsContent doInBackground(String... urls) {

            try {
                //根据新闻的url获取新闻内容
                return mNewsItemBiz.getNewsContent(urls[0]);
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("ASD","Content错误： "+ e);
                return null;
            }

        }

        /**
         * 得到新闻内容后将其加载
         * @param newsContent 得到的新闻内容
         */
        @Override
        protected void onPostExecute(NewsContent newsContent) {
            if (newsContent == null) {
                Toast.makeText(NewsContentActivity.this,getResources().getString(R.string.net_unavaiable), Toast.LENGTH_LONG).show();
                return;
            }
            //处理信息缓存
            mNewsContent = newsContent;
            //将内容载入界面
            mTitleTextView.setText(newsContent.getTitle());
            mTitleDateTextView.setText(newsContent.getDate());
            mContextTextView.setText(newsContent.getFormatedContent());
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
    }
}
