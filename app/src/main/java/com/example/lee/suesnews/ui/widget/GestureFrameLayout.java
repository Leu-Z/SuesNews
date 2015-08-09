package com.example.lee.suesnews.ui.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Scroller;

import com.example.lee.suesnews.R;

import java.util.LinkedList;
import java.util.List;

/**
 * 增加滑动返回手势的FrameLayout
 * Created by Administrator on 2015/2/20.
 */
public class GestureFrameLayout extends FrameLayout  {
    ////得到类的简写名称,即GestureFrameLayout
    private static final String TAG = GestureFrameLayout.class.getSimpleName();
    private View mContentView;
    private int mTouchSlop;
    private int downX;
    private int downY;
    private int tempX;
    private Scroller mScroller;
    private int viewWidth;
    private boolean isSilding;
    private boolean isFinish;
    private Drawable mShadowDrawable;
    private Activity mActivity;
    private GestureDetector mGesture;    //判断左右滑动手势
    private List<ViewPager> mViewPagers = new LinkedList<ViewPager>();

    //默认调用两位的构造器
    public GestureFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        /*getScaledTouchSlop是一个距离，表示滑动的时候，
        手的移动要大于这个距离才开始移动控件。如果小于这个距离就不触发移动控件*/
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScroller = new Scroller(context);

        mShadowDrawable = getResources().getDrawable(R.drawable.shadow_left);
        //手势识别器，转发次MotionEvent对象至OnGestureListener
        mGesture = new GestureDetector(this.getContext(),onGestureListener);
    }

    /**
     * 设置手势监听器监听左右滑动手势,消耗了这个动作
     */
    private GestureDetector.OnGestureListener onGestureListener=
            new GestureDetector.SimpleOnGestureListener(){
                //获得瞬间滑动后回调onFling(),即抛掷动作.
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    //计算向量的水平长度,因为是向量，所以开始-结束才是正的
                    float startX = e1.getX();
                    float endX = e2.getX();
                    float x = endX - startX;
                    //向右划
                    if( x > -5 ){
                        scrollRight();
                        isFinish = true;
                    }else{
                        scrollOrigin();
                        isFinish = false;
                    }
                    Log.d("eee","使用了onFling");
                    return true;
                }
            };

    /**
     * 与Activity绑定
     * @param activity 要绑定的activity
     */
    public void attachToActivity(Activity activity) {
        mActivity = activity;

        mContentView = (View) this.getParent();
//        TypedArray a = activity.getTheme().obtainStyledAttributes(
//                new int[] { android.R.attr.windowBackground });
//        int background = a.getResourceId(0, 0);
//        a.recycle();
//
//        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
//        ViewGroup decorChild = (ViewGroup) decor.getChildAt(0);
//        decorChild.setBackgroundResource(background);
//        decor.removeView(decorChild);
//        addView(decorChild);
//        setContentView(decorChild);
//        decor.addView(this);
    }

    private void setContentView(View decorChild) {
        mContentView = (View) decorChild.getParent();
    }

    /**
     * 事件拦截操作,有了触摸事件后就触发了
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        //处理ViewPager冲突问题
        ViewPager mViewPager = getTouchViewPager(mViewPagers, ev);
        Log.i(TAG, "mViewPager = " + mViewPager);
        Log.d("eee","使用了onInterceptTouchEvent") ;
        if(mViewPager != null && mViewPager.getCurrentItem() != 0){
            return super.onInterceptTouchEvent(ev);
        }

        //如果OnGestureListener消耗了这个动作，则不将事件传递给子view，直接由这里的onTouchEvent动作
        if(mGesture.onTouchEvent(ev)){
            return true;
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //距离左屏幕的位置
                downX = tempX = (int) ev.getRawX();
                //距离上屏幕的位置
                downY = (int) ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) ev.getRawX();
                // 满足此条件屏蔽SildingFinishLayout里面子类的touch事件
                //右划的距离满足条件与水平滑动小于满足距离，那么就让onTouchEvent来执行
                if (moveX - downX > mTouchSlop
                        && Math.abs((int) ev.getRawY() - downY) < mTouchSlop) {
                    return true;
                }
                break;
        }

        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //如果OnGestureListener消耗了这个动作,就退出
        if(mGesture.onTouchEvent(event)){
            return true;
        }
        switch (event.getAction()) {

            case MotionEvent.ACTION_MOVE:
                int moveX = (int) event.getRawX();
                int deltaX = tempX - moveX;
                tempX = moveX;
                if (moveX - downX > mTouchSlop
                        && Math.abs((int) event.getRawY() - downY) < mTouchSlop) {
                    isSilding = true;
                }

                if (moveX - downX >= 0 && isSilding) {
                    //滑动view,手指移动多少就滑动多少
                    mContentView.scrollBy(deltaX, 0);
                }
                break;
            case MotionEvent.ACTION_UP:
                isSilding = false;
                //移动距离超过屏幕的二分之一就关活动，否则复原
                if (mContentView.getScrollX() <= -viewWidth / 2) {
                    isFinish = true;
                    scrollRight();
                } else {
                    scrollOrigin();
                    isFinish = false;
                }
                break;
        }

        return true;
    }

    /**
     * 获取SwipeBackLayout里面的ViewPager的集合
     * @param mViewPagers
     * @param parent
     */
    private void getAlLViewPager(List<ViewPager> mViewPagers, ViewGroup parent){
        int childCount = parent.getChildCount();
        for(int i=0; i<childCount; i++){
            View child = parent.getChildAt(i);
            if(child instanceof ViewPager){
                mViewPagers.add((ViewPager)child);
            }else if(child instanceof ViewGroup){
                getAlLViewPager(mViewPagers, (ViewGroup)child);
            }
        }
    }


    /**
     * 返回我们touch的ViewPager
     * @param mViewPagers
     * @param ev
     * @return
     */
    private ViewPager getTouchViewPager(List<ViewPager> mViewPagers, MotionEvent ev){
        if(mViewPagers == null || mViewPagers.size() == 0){
            return null;
        }
        Rect mRect = new Rect();
        for(ViewPager v : mViewPagers){
            v.getHitRect(mRect);

            if(mRect.contains((int)ev.getX(), (int)ev.getY())){
                return v;
            }
        }
        return null;
    }

    /**
     * 对其所有childView进行定位（设置childView的绘制区域）
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            //得到view的宽度
            viewWidth = this.getWidth();
            getAlLViewPager(mViewPagers, this);
            Log.i(TAG, "ViewPager size = " + mViewPagers.size());
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mShadowDrawable != null && mContentView != null) {

            int left = mContentView.getLeft()
                    - mShadowDrawable.getIntrinsicWidth();
            int right = left + mShadowDrawable.getIntrinsicWidth();
            int top = mContentView.getTop();
            int bottom = mContentView.getBottom();
            //这里
            mShadowDrawable.setBounds(left, top, right, bottom);
            mShadowDrawable.draw(canvas);
        }
    }


    /**
     * 滚动出界面，从手指拖动view抬起来位置开始滚动
     */
    private void scrollRight() {
        //这里需要移动的距离
        final int delta = (viewWidth + mContentView.getScrollX());
        //滚动，startX, startY为开始滚动的位置，dx,dy为滚动的偏移量, duration为完成滚动的时间
        //设置mScroller的滚动偏移量,负的就是往右滚
        mScroller.startScroll(mContentView.getScrollX(), 0, -delta + 1, 0,
                Math.abs(delta));
        //这里必须调用才能保证computeScroll()会被调用，否则不一定会刷新界面，看不到滚动效果
        postInvalidate();
    }

    /**
     * 滚动到起始位置
     */
    private void scrollOrigin() {

        int delta = mContentView.getScrollX();
        mScroller.startScroll(mContentView.getScrollX(), 0, -delta, 0,
                Math.abs(delta));
        postInvalidate();
    }

    /**
     * 完成实际的滚动
     */
    @Override
    public void computeScroll() {
        // 调用startScroll的时候scroller.computeScrollOffset()返回true，
        //用来判断是否滚动是否结束,true说明滚动尚未完成
        if (mScroller.computeScrollOffset()) {
            //这里调用View的scrollTo()完成实际的滚动
            mContentView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            //必须调用该方法，否则不一定能看到滚动效果
            postInvalidate();
            //滚动完成退出活动
            if (mScroller.isFinished() && isFinish) {
                mActivity.finish();
            }
        }
    }
}
