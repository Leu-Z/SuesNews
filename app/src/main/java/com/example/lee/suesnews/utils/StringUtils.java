package com.example.lee.suesnews.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 利用正则表达式，从字符串中取得需要的字符
 * Created by Administrator on 2015/2/15.
 */
public class StringUtils {

    /**
     * 利用正则表达式提取字符串中的数字,得到字符串中的所有数字
     * @param str 被提取的字符串
     * @return 提取的数字
     */
    public static int getIntFromString(String str){
        //先编译你的正则表达式，生成Pattern，把想要检索的字符串传入matcher方法。
        //字符类，除了从0到9之外的任何字符
        String regEx="[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        //将所有匹配的部分都替换成你传入的参数，即把除了从0到9之外的任何字符都用空白来代替
        return Integer.parseInt(m.replaceAll("").trim());
    }

    /**
     * 利用正则表达式提取字符串中的日期
     * 进来的string是时间：2015-07-29，只取出其中的2015-07-29
     * @param str 被提取的字符串
     * @return 提取的日期，格式为YYYY-MM-DD
     */
    public static String getDateFromString(String str){
        //{}内是恰好n次X。格式：4个数字-2个数字-2个数字,-不是元字符，表示他本身
        String regEx="[0-9]{4}-[0-9]{2}-[0-9]{2}";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        //从字符串遍历，一个一个匹配过来，有匹配就返回true。然后可以从group()中获取。
        //这里只获得第一个
        if (m.find()){
            return m.group();
        }
        return null;
    }


}
