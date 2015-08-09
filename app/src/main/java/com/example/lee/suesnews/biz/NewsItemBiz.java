package com.example.lee.suesnews.biz;

import android.content.Context;
import android.util.Log;

import com.example.lee.suesnews.bean.NewsContent;
import com.example.lee.suesnews.bean.NewsItem;
import com.example.lee.suesnews.common.NewsTypes;
import com.example.lee.suesnews.dao.NewsContentDao;
import com.example.lee.suesnews.dao.NewsItemDao;
import com.example.lee.suesnews.utils.HttpUtils;
import com.example.lee.suesnews.utils.StringUtils;
import com.example.lee.suesnews.utils.SuesApiUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 处理新闻的业务逻辑类，根据新闻类型和页码得到新闻列表，根据新闻的url获取新闻内容
 * Created by Administrator on 2015/1/18.
 */
public class NewsItemBiz {

    //新闻列表页相关class
    private static final String BASE_TABLE_CLASS = "border1";  //包含新闻列表的table的class
    private static final String COLUMN_TABLE_CLASS = "columnStyle";//包含新闻条目的table的class
    private static final String POST_TIME_CLASS = "posttime";//包含新闻时间的class
    private static final String NEWS_SOURCE_CLASS = "derivation";//包含新闻来源媒体的class

    //新闻内容页相关class

    private static final String NEWS_TITLE_CLASS = "biaoti";//包含新闻标题td的class
    private static final String NEWS_META_CLASS = "postmeta";//包含新闻相关信息的p标签的class
    private static final String NEWS_META_ITEM_CLASS = "meta_item";//包含新闻相关信息条目的class
    private static final String NEWS_ARTICLE_CLASS = "article";//包含新闻内容td的class

    private static final int OUTOFTIME_MINUTE = 31;             //新闻过期时间（分钟）

    private List<NewsItem> mNewsItemCache;                  //NewsItem缓存

    private Context mContext;

    private NewsItemDao mNewsItemDao;
    private NewsContentDao mNewsContentDao;

    public NewsItemBiz(Context context) {
        mContext = context;
        mNewsItemDao = new NewsItemDao(context);
        mNewsContentDao = new NewsContentDao(context);
    }

    /**
     * 查看对象是否过期,作用于NewsItem，通过比较更新时间
     * 当前时间30分钟之前的NewsItem就算过期了
     * @param t
     * @param <T>
     * @return 如果未过期则返回大于0的数，如果过期则返回小与0的数
     */
    public <T> int isOutOfTime(T t){


        if (t instanceof NewsItem ){
            return ((NewsItem) t).getUpdateTime().compareTo(getUnOutOfTimeDate());
        }

        return -1;
    }

    /**
     * 得到未过期的最迟时间（即修改时间小于此时间为过期）
     * @return 过期时间
     */
    public Date getUnOutOfTimeDate(){
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        //把分钟往前调30分
        calendar.add(Calendar.MINUTE, - OUTOFTIME_MINUTE);
        return calendar.getTime();
    }

    /**
     * 获取新闻项的数据库缓存,从数据库中获取NewsItem，缓存为空或需要强制刷新时
     * @param newsType  新闻类型
     * @param currentPage   当前页
     * @param isNeedRefresh 是否强制刷新
     * @return  新闻项列表缓存
     * @throws SQLException
     */
    public List<NewsItem> getNewsItemCache(int newsType,int currentPage,
                                           boolean isNeedRefresh) throws SQLException {
        //如果缓存为空或需要强制刷新缓存时重新从数据库提取数据
        if (mNewsItemCache == null || isNeedRefresh){
            //通过页码和新闻类型从数据库得到符合要求的NewsItem
            mNewsItemCache = mNewsItemDao.searchByPageAndType(currentPage,newsType);
        }
        return mNewsItemCache;
    }

    /**
     * 设置缓存,设置这个类里的NewsItemCache,从网络获取后
     * @param mNewsItemCache
     */
    public void setNewsItemCache(List<NewsItem> mNewsItemCache) {
        this.mNewsItemCache = mNewsItemCache;
    }

//    /**
//     * 获取新闻内容的数据库缓存
//     * @param url  地址
//     * @return  新闻内容缓存
//     * @throws SQLException
//     */
//    public List<NewsContent> getNewsContentCache(String url) throws SQLException {
//        //如果缓存为空或需要刷新缓存时重新从数据库提取数据
//
//        return mNewsContentCache;
//    }
//
//    /**
//     * 设置缓存
//     * @param mNewsContentCache
//     */
//    public void setNewsContentCache(List<NewsContent> mNewsContentCache) {
//        this.mNewsContentCache = mNewsContentCache;
//    }


    /**
     * 根据新闻类型和页码得到新闻列表,如果没网络或者缓存NewsItem里面有且没过期，就返回数据库里缓存的
     * 否则就从网页获取,获取后保存进数据库并添加进缓存
     * @param newsType      新闻类型
     * @param currentPage   页码
     * @param netAvailable  当前是否有网络
     * @return              新闻列表
     * @throws Exception
     */
    public List<NewsItem> getNewsItems(int newsType,int currentPage,boolean netAvailable) throws Exception {

        //当无网络时加载数据库中数据
        Log.i("ASDNET","netAvailable:"+netAvailable);

        if (!netAvailable ){
            return getNewsItemCache(newsType,currentPage,false);
        }
        //有网络时如果数据库里有缓存且数据未过期则也返回缓存数据
        if (getNewsItemCache(newsType,currentPage,false) != null
                && this.isOutOfTime(getNewsItemCache(newsType,currentPage,false).get(0)) > 0){
            return getNewsItemCache(newsType,currentPage,true);
        }
        //若数据已过期，则重新获取
        //组合得到指定新闻类型和页码的页面url
        String url = SuesApiUtils.getNewsUrl(newsType, currentPage);
        String htmlStr = null;
        //如果服务器未返回数据,则返回数据库中的数据
        try {
            //用Get方法返回该链接地址的html数据
            htmlStr = HttpUtils.doGet(url);
        }catch (Exception ex){
            return getNewsItemCache(newsType,currentPage,true);
        }
        List<NewsItem> newsItems = new ArrayList<NewsItem>();

        NewsItem newsItem;
        Document document = Jsoup.parse(htmlStr);
        //新闻列表,每个HTML标签是一个元素节点，而getElementsByClass是通过Class属性从整个HTML文档中
        // 查找符合要求的任何HTML元素
        //Jsoup以使用DOM方法           这里是有border1的标签list的第一个元素,是一个table标签
        Element itemTable = document.getElementsByClass(BASE_TABLE_CLASS).get(0);
        //table标签下的第一个tr元素的子元素集,一共有两个td
        //这里的items应该是多个table下的tr,但是，还是不符合逻辑、、、、
        Elements items = itemTable.child(0).children();
        for (int i=0;i<items.size();i++){
            newsItem = new NewsItem();
            newsItem.setType(newsType);
            //新闻条目   找到含有columnStyle的标签,是第二个td里的table
            Element columnTable = items.get(i).getElementsByClass(COLUMN_TABLE_CLASS).get(0);
            //链接,查找得到a标签
            Element link = columnTable.getElementsByTag("a").get(0);
            //<a>标签里的href属性，得到新闻条目的链接
            String contentUrl = link.attr("href");  //新闻内容链接
            //进行组合后就是该新闻条目的Url地址
            newsItem.setUrl(SuesApiUtils.NEWS_URL_MAIN + contentUrl);
            //得到a标签里的font标签元素的文本
            newsItem.setTitle(link.child(0).text()); //设置新闻标题

            //媒体聚焦页面有来源无时间
            if (newsType != NewsTypes.NEWS_TPYE_MTJJ){
                //posttime，在table里找到第三个含有posttime的td
                Element postTime = columnTable.getElementsByClass(POST_TIME_CLASS).get(0);
                //进行文本设置，格式如2015-07-29
                newsItem.setDate(postTime.text());
            }else{
                //否则是derivation，来源，类似
                Element source = columnTable.getElementsByClass(NEWS_SOURCE_CLASS).get(0);
                newsItem.setSource(source.text());
            }

            newsItem.setPageNumber(currentPage);


            newsItem.setUpdateTime(new Date());
            newsItems.add(newsItem);

        }

        //将数据添加进数据库
        for(NewsItem item : newsItems) {
            mNewsItemDao.createOrUpdate(item);
        }
        //将数据添加进缓存
        setNewsItemCache(newsItems);

        return newsItems;

    }

    /**
     * 根据新闻的url获取新闻内容。先从数据库中查找是否有新闻内容，有的话用数据库中的
     * 没有就从网页获得，再将数据添加进数据库
     * @param url 新闻url
     * @return
     */
    public NewsContent getNewsContent(String url) throws Exception {
         //先从数据库中查找是否有新闻内容，有的话用数据库中的
        NewsContent content = mNewsContentDao.searchByUrl(url);
        if (content != null){
            return content;
        }
        //获取html网页内容
        String htmlStr = HttpUtils.doGet(url);
//        Log.i("ASD","html"+htmlStr);
        NewsContent news = new NewsContent();

        Document document = Jsoup.parse(htmlStr);
//        Log.i("ASD","html"+htmlStr);
        //新闻url
        news.setUrl(url);

        //新闻标题 ，含有biaoti的class的一个td
        Element titleElement = document.getElementsByClass(NEWS_TITLE_CLASS).get(0);
        Log.i("ASD","Title: "+titleElement.text());
        news.setTitle(titleElement.text());

        //找到包含新闻信息的p标签，postmeta
        Element metaElement = document.getElementsByClass(NEWS_META_CLASS).get(0);
        Log.i("ASD","metaElement"+metaElement.text());
        //新闻时间，利用正则表达式提取字符串中的日期
        news.setDate(StringUtils.getDateFromString(metaElement.text()));
        Log.i("ASDDATE","date:  "+StringUtils.getDateFromString(metaElement.text()));

        //新闻作者，含有meta_item的span标签共有4个，第一个作者
        Element authorElement = document.getElementsByClass(NEWS_META_ITEM_CLASS).get(0);
        Log.i("ASD","authorElement"+authorElement.text());
        news.setAuthor(authorElement.text());

        //新闻来源，第三个为来源
        Element sourceElement = document.getElementsByClass(NEWS_META_ITEM_CLASS).get(2);
        Log.i("ASD","sourceElement"+sourceElement.text());
        news.setSource(sourceElement.text());

        //新闻内容，含有class的article一共就一个td标签
        Element contentElement = document.getElementsByClass(NEWS_ARTICLE_CLASS).get(0);
        //得到所有td标签的子标签，都是一些p标签
        Elements contentItems = contentElement.children();
        //新闻内容都在p标签内，其中某些是图片
        for(Element contentItem : contentItems){
            //有些p标签有图片，有些没有
            Elements images = contentItem.getElementsByTag("img");
            //获取图片
            if (images.size() > 0){
                for (Element image : images){
//                    news.addImgUrl(image.attr("src"));
                }
                //跳出循环，因为有图片的p标签里没内容
                continue;
            }
            //p标签里的文字内容少于1个，就跳出循环
            if(contentItem.text().trim().length()<=1){
                continue;
            }
            Log.i("ASD","contentText"+contentItem.text() + " length: " + contentItem.text().trim().length());
            //text()得到该元素文本和和其子孙文本的结合，中间有空一个空格
            news.addContent(contentItem.text());

        }

        //将数据添加进数据库
        mNewsContentDao.createOrUpdate(news);

        return news;
    }

    /**
     * 清除缓存数据库
     */
    public void clearCache(){
        mNewsContentDao.deleteAll();
        mNewsItemDao.deleteAll();
    }
}
