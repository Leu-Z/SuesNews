package com.example.lee.suesnews.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Http辅助工具类,返回对应链接地址的html数据
 * Created by Administrator on 2015/1/18.
 */
public class HttpUtils {

    private static final int TIMEOUT_IN_MILLIONS = 5000;

    /**
     * 用Get方法返回该链接地址的html数据
     *
     * @param urlStr    URL地址
     * @return  服务器返回的数据
     */
    public static String doGet(String urlStr) throws Exception
    {
        if (urlStr == ""){
            return null;
        }
        URL url;
        HttpURLConnection conn = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        try
        {   //根据输入的string新建一个url
            url = new URL(urlStr);
            //使用URL打开一个链接
            conn = (HttpURLConnection) url.openConnection();
            //设置从主机读取数据超时（单位：毫秒）
            conn.setReadTimeout(TIMEOUT_IN_MILLIONS);
            //设置连接主机超时（单位：毫秒）
            conn.setConnectTimeout(TIMEOUT_IN_MILLIONS);
            //使用get请求
            conn.setRequestMethod("GET");
            ////设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            //请求长连接
            conn.setRequestProperty("connection", "Keep-Alive");
            //检查是否正常返回请求数据
            if (conn.getResponseCode() == 200)
            {   //获取输入流，此时才真正建立链接
                is = conn.getInputStream();
                baos = new ByteArrayOutputStream();
                int len = -1;
                //128个字节，可以显示64个中文
                byte[] buf = new byte[128];
                //len为返回实际读取的字节数
                while ((len = is.read(buf)) != -1)
                {
                    baos.write(buf, 0, len);
                }
                //强制将缓冲区中的数据发送出去,不必等到缓冲区满
                baos.flush();
                return baos.toString();
            } else
            {
                throw new RuntimeException(" responseCode is not 200 ... ");
            }

        } catch (Exception e)
        {
            e.printStackTrace();

        } finally
        {
            try
            {
                if (is != null)
                    is.close();
            } catch (IOException e)
            {
            }
            try
            {
                if (baos != null)
                    baos.close();
            } catch (IOException e)
            {
            }
            //每次连接后，都要将连接给关闭，执行disconnect操作
            conn.disconnect();
        }

        return null ;
    }


    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url
     *            发送请求的 URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果
     * @throws Exception
     */
    public static String doPost(String url, String param)
    {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try
        {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            HttpURLConnection conn = (HttpURLConnection) realUrl
                    .openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            conn.setUseCaches(false);
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setReadTimeout(TIMEOUT_IN_MILLIONS);
            conn.setConnectTimeout(TIMEOUT_IN_MILLIONS);

            if (param != null && !param.trim().equals(""))
            {
                // 获取URLConnection对象对应的输出流
                out = new PrintWriter(conn.getOutputStream());
                // 发送请求参数
                out.print(param);
                // flush输出流的缓冲
                out.flush();
            }
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null)
            {
                result += line;
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        // 使用finally块来关闭输出流、输入流
        finally
        {
            try
            {
                if (out != null)
                {
                    out.close();
                }
                if (in != null)
                {
                    in.close();
                }
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 当前是否有网络连接
     * @return
     */
    public static boolean IsNetAvailable(Context context){
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        return (info != null && info.isConnected());

    }

}
