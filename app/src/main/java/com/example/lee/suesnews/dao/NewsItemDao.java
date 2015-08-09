package com.example.lee.suesnews.dao;

import android.content.Context;

import com.example.lee.suesnews.bean.NewsItem;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.DeleteBuilder;

import java.sql.SQLException;
import java.util.List;

/**
 * 对新闻列表的数据库操作,每个表一般我们都会单独写个Dao用于操作
 * Created by Administrator on 2015/2/24.
 */
public class NewsItemDao {
    //这里dao里面定义了很多对数据库操作的方法，直接拿来用就好了
    private RuntimeExceptionDao<NewsItem,Integer> mNewsItemDao;
    private DataBaseHelper mDataBaseHelpter;

    public NewsItemDao(Context context) {
        mDataBaseHelpter = DataBaseHelper.getHelper(context);
        //自己得到了NewsItemRuntimeDao，而不用helper里面创建的
        this.mNewsItemDao = mDataBaseHelpter.getNewsItemRuntimeDao();
    }

    /**
     * 更新或添加
     * @param newsItem 需要更新的新闻列表项
     */
    public void createOrUpdate(NewsItem newsItem){
        mNewsItemDao.createOrUpdate(newsItem);
    }


    /**
     * 按照标题删除
     * @param   title 需要删除的新闻项的标题
     * @return
     * @throws SQLException
     */
    public int deleteByTitle(String title) throws SQLException {

        DeleteBuilder<NewsItem, Integer> deleteBuilder = mNewsItemDao.deleteBuilder();
        //根据title列名查找并删除
        deleteBuilder.where().eq("title",title);
        return deleteBuilder.delete();

    }

    /**
     * 按照url删除
     * @param   url 需要删除的新闻项的url
     * @return
     * @throws SQLException
     */
    public int deleteByUrl(String url) throws SQLException {

        DeleteBuilder<NewsItem, Integer> deleteBuilder = mNewsItemDao.deleteBuilder();
        deleteBuilder.where().eq("url", url);
        return deleteBuilder.delete();

    }

    /**
     * 删除数据库中所有的NewsItem,清缓存时用到
     */
    public void deleteAll(){
        mNewsItemDao.delete(queryAll());
    }

    /**
     * 查询数据库中所有的NewsItem,并返回
     */
    public List<NewsItem> queryAll(){
        List<NewsItem> news = mNewsItemDao.queryForAll();
        return news;
    }

    /**
     * 按照标题查询
     * @param title
     * @return
     * @throws SQLException
     */
    public NewsItem searchByTitle(String title) throws SQLException {
        //把相同标题的都查出来
        List<NewsItem> newsItems = mNewsItemDao.queryBuilder().where().eq("title",title).query();
        //查到了就返回第一个
        if (newsItems.size() > 0){
            return newsItems.get(0);
        }
        return null;
    }

    /**
     * 按照页码和类型查询NewsItem,并把找到的返回
     * @param page
     * @param type
     * @return
     * @throws SQLException
     */
    public List<NewsItem> searchByPageAndType(int page,int type) throws SQLException {
        //从表中找出要求的pageNumber与type皆符合的NewsItem组。
        List<NewsItem> newsItems = mNewsItemDao.queryBuilder().where().eq("pageNumber",page).and()
                .eq("type",type).query();
        //查到了就返回list
        if (newsItems.size() > 0){
            return newsItems;
        }
        return null;
    }

    /**
     * 按照url查询
     * @param url
     * @return
     * @throws SQLException
     */
    public NewsItem searchByUrl(String url) throws SQLException {

        List<NewsItem> newsItems = mNewsItemDao.queryBuilder().where().eq("url",url).query();
        if (newsItems.size() > 0){
            return newsItems.get(0);
        }
        return null;

    }

}
