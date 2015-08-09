package com.example.lee.suesnews.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * 附带监听滑动的ScrollView
 * Created by Administrator on 2015/3/6.
 */
public class ObservableScrollView extends ScrollView {

    private OnScrollChangedListener mOnScrollChangedListener;
    public ObservableScrollView(Context context) {
        super(context);
    }

    public ObservableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ObservableScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChangedListener != null) {

            mOnScrollChangedListener.onScrollChanged(l, t, oldl, oldt);
        }
    }
 //先要实现这里的监听对象实例
    public void setOnScrollListener(OnScrollChangedListener onScrollChangedListener) {
        this.mOnScrollChangedListener = onScrollChangedListener;
    }

   // 写这个接口就是为了把onScrollChanged暴露出去，可以在活动中使用onScrollChanged
    public interface OnScrollChangedListener{
        public void onScrollChanged(int x, int y, int oldX, int oldY);
    }
}
