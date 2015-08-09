package com.example.lee.suesnews.ui.fragments;


import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.lee.suesnews.R;
import com.example.lee.suesnews.bean.NewsItem;
import com.example.lee.suesnews.ui.MyRecyclerAdapter;
import com.example.lee.suesnews.ui.NewsContentActivity;
import com.example.lee.suesnews.ui.RecyclerItemClickListener;
import com.example.lee.suesnews.utils.HttpUtils;
import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;

import java.util.ArrayList;
import java.util.List;

import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;
import in.srain.cube.views.ptr.header.MaterialHeader;

/**
 * A simple {@link android.app.Fragment} subclass.
 */
public class NewsListFragment extends BaseFragment {

    private static final String ARG_NEWS_TYPE = "newsType";

    //新闻类型
    private int mNewsType;

    private ObservableRecyclerView mRecyclerView;
    private MyRecyclerAdapter mAdapter;
    private ObservableRecyclerView.LayoutManager mLayoutManager;

    //当前页码
    private int mCurrentPage;

    private PtrFrameLayout frame;
    private MaterialHeader header;

    //是否为第一次加载数据
    private boolean mIsFirstLoad = true;

    //缓存
    private List<NewsItem> mNewsItems = new ArrayList<NewsItem>();

    public NewsListFragment() {
        // Required empty public constructor
    }

    //根据新闻类型构建一个碎片实例
    public static NewsListFragment newInstance(int newsType) {
        NewsListFragment fragment = new NewsListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_NEWS_TYPE, newsType);
        fragment.setArguments(args);
        return fragment;
    }

    //创建碎片的布局View
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_news_list, container, false);
        init(view);

        return view;
    }

    //初始化碎片view,把碎片view里的下拉刷新，RecyclerView设置好(点击事件啦，滚动事件啦等等)
    private void init(View view) {
        Activity parentActivity = getActivity();

        frame = (PtrFrameLayout) view.findViewById(R.id.ptr_frame);
        //Material风格的Header
        header = new MaterialHeader(parentActivity.getBaseContext());

        header.setPadding(0, 20, 0, 20);
        header.setPtrFrameLayout(frame);

        //加载的最小时间，不能小于1s
        frame.setLoadingMinTime(1000);
        //头部回弹时间
        frame.setDurationToCloseHeader(300);
        frame.setHeaderView(header);
        frame.addPtrUIHandler(header);


        frame.setPtrHandler(new PtrHandler() {
            @Override//检查是否可以下拉刷新
            public boolean checkCanDoRefresh(PtrFrameLayout ptrFrameLayout, View view, View view2) {
               //得到当前y卷轴的坐标,如果RecyclerView在顶部
                return mRecyclerView.getCurrentScrollY() == 0;
            }

            @Override//开始刷新
            public void onRefreshBegin(final PtrFrameLayout ptrFrameLayout) {
                //加载当前页的新闻列表并直接刷新出来，更新碎片和适配器的mNewsItems
                getNewsList(mAdapter, mCurrentPage, true);
                //ptrFrameLayout.refreshComplete();
            }
        });


        mRecyclerView = (ObservableRecyclerView) view.findViewById(R.id.my_recycler_view);

        mLayoutManager = new LinearLayoutManager(parentActivity);

        //设置回调,RecyclerView的拉动被监控，同时改变toolBar的动作
        mRecyclerView.setScrollViewCallbacks((ObservableScrollViewCallbacks)parentActivity);
        //设置RecyclerView的布局为LinearLayout，即类似listView
        mRecyclerView.setLayoutManager(mLayoutManager);
        //RecyclerView点击事件,自己另外创建一个RecyclerItemClickListener类，来实现onItemClick的回调
        //进入新活动的时候有一个动画
        mRecyclerView.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(),new RecyclerItemClickListener.OnItemClickListener() {
                    @Override//传入的是点中的view和该view的序列
                    public void onItemClick(View view, int position) {
                        //从适配器中的新闻列表得到被点击的新闻项
                        NewsItem item = mAdapter.getmNewsList().get(position);

                        //打开显示新闻内容的Activity,把新闻的url作为参数传过去
                        Intent startActivityIntent = new Intent(getActivity(),NewsContentActivity.class);

                        view.setDrawingCacheEnabled(true);
                        view.setPressed(false);
                        view.refreshDrawableState();
                        //从view中获取图片
                        Bitmap bitmap = view.getDrawingCache();
                        //能够从屏幕指定的位置和提供的缩略图拉伸一个活劢窗口。
                        ActivityOptions options = ActivityOptions.makeThumbnailScaleUpAnimation(
                                view, bitmap, 0, 0);

                        Bundle urlBundle = new Bundle();
                        urlBundle.putString("url",item.getUrl());
                        startActivityIntent.putExtra("key",urlBundle);
                        ActivityCompat.startActivity(getActivity(), startActivityIntent, options.toBundle());

                    }
                })
            );
        //设置adapter
        mAdapter = new MyRecyclerAdapter(getActivity());
        mRecyclerView.setAdapter(mAdapter);

        //得到数据,默认先刷新第0页的列表
        getNewsList(mAdapter, 0, false);

        //监听list滑动事件既可以上下滚动，又可以左右滚动
        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override//dx为x轴滚动的距离，dy为y轴滚动的距离
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //找到最后一个可见的Item
                int lastVisibleItem = ((LinearLayoutManager)mLayoutManager).findLastVisibleItemPosition();
                //现有的所有item数量
                int totalItem = mLayoutManager.getItemCount();
                //当剩下1个item时加载下一页，即往下滚到见到倒数第2个item时加载下一页
                if(lastVisibleItem > totalItem - 2 && dy > 0){
                    //NewsItems最后一个的页数加1，
                    int loadPage= mNewsItems.get(mNewsItems.size()-1).getPageNumber() + 1;
                    if (mCurrentPage < loadPage) {
                        mCurrentPage = loadPage;
                        getNewsList(mAdapter, mCurrentPage, false);
                    }
                }
            }
        });
}

    @Override //得到碎片的标题类型，设当前页为0
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mNewsType = getArguments().getInt(ARG_NEWS_TYPE);
        }
        //不管NEWS_TYPE是什么，默认当前页数为第一页
        mCurrentPage = 0;
    }


    /**
     * 获取某一页的数据
     * @param adapter
     * @param currentPage 页码
     * @param forced      是否强制刷新
     */
    private void getNewsList(MyRecyclerAdapter adapter,int currentPage,boolean forced) {
        /*按理说没有强制刷新过的话， mNewsItems一定为零*/
        int total = mNewsItems.size();
        //不强制刷新时，如果此页已存在则直接从内存中加载
        if (!forced && total>0 &&//mNewsItems最后一项的页数大于等于当前页数
                (mNewsItems.get(total-1).getPageNumber() >= currentPage) ){
            //给适配器增加新闻列表
            mAdapter.addNews(mNewsItems);
            mAdapter.notifyDataSetChanged();
            return;
        }
        //强制刷新
        if(forced && mNewsItems.size()>0){
            mNewsItems.clear();
        }
        LoadNewsListTask loadDataTask = new LoadNewsListTask(adapter,mNewsType,forced);
        //加载当前页的新闻列表并直接刷新出来，更新碎片和适配器的mNewsItems
        loadDataTask.execute(currentPage);
    }



    /**
     * 加载新闻列表的任务,直接刷新出来，更新当前页碎片和适配器的mNewsItems
     *如果当前是第一次加载，则直接从数据库读取，否则从网络获取根据新闻类型和页码得到新闻列表
     */
    class LoadNewsListTask extends AsyncTask<Integer, Integer,List<NewsItem> >{

        private MyRecyclerAdapter mAdapter;
        private boolean mIsForced;
        private int mNewsType;

        public LoadNewsListTask(MyRecyclerAdapter adapter,int newsType,boolean forced) {
            super();
            mAdapter = adapter;
            mIsForced = forced;
            mNewsType = newsType;
        }

        /**
         *得到当前页码的新闻列表
         * @param currentPage 当前页码
         * @return 当前页码的新闻列表,出错返回null
         */
        @Override
        protected List<NewsItem> doInBackground(Integer... currentPage) {

            try {

                boolean netAvailable = HttpUtils.IsNetAvailable(getActivity());
                //如果当前是第一次加载，则直接从数据库读取
                if (netAvailable && mIsFirstLoad){
                    mIsFirstLoad = false;
                    //获取新闻项的数据库缓存
                    return mNewsItemBiz.getNewsItemCache(mNewsType, currentPage[0], true);
                }
                //从网络获取根据新闻类型和页码得到新闻列表
                return mNewsItemBiz.getNewsItems(mNewsType, currentPage[0],netAvailable);
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("ASDNET","neterror :"+e);
                return null;
            }

        }

        /**
         * 得到新闻列表后将其加载
         * @param newsItems 得到的新闻列表，从doInBackground得到
         */
        @Override
        protected void onPostExecute(List<NewsItem> newsItems) {
            if (newsItems == null) {
                Toast.makeText(getActivity(),getActivity().getResources().getString(R.string.net_unavaiable)
                        ,Toast.LENGTH_LONG).show();
                return;
            }
            //处理强制刷新
            if(mIsForced){
                //把适配器里的NewsList也清空了
                mAdapter.getmNewsList().clear();
            }
            //这是碎片里的NewsItems，和Adapter一同刷新
            //也就是说碎片里的NewsItems只有这一个来源途径
            mNewsItems.addAll(newsItems);
            mAdapter.addNews(newsItems);
            //在修改适配器绑定的数组后，不用重新刷新Activity，通知Activity更新ListView。
            mAdapter.notifyDataSetChanged();
            //下拉刷新也刷新完毕
            frame.refreshComplete();
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
