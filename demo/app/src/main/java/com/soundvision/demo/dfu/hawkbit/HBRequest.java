package com.soundvision.demo.dfu.hawkbit;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import com.google.gson.Gson;

public class HBRequest extends AsyncTask<String, String, String> {


    private Object ret;
    private String mUrl;
    private OnReceive callback;
    private Class _class;

    public interface OnReceive {
        void OnResult(Object response);
    }

    public <T> HBRequest(Context context, String url, Class<T> c, OnReceive cb) {
        //mContext = context;
        mUrl = url;
        callback = cb;
        _class = c;
        execute();
    }


    @Override
    protected String doInBackground(String... uri) {

        String responseString = "";

        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(mUrl).openConnection();
            connection.setDoOutput(false); // Triggers POST true.
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", Config.TOKEN);
            connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
            connection.setRequestProperty("Accept","*/*");
            connection.setRequestProperty("Content-Type", "application/json");

            boolean isRedirect;
            do {
                if (connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM || connection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                    isRedirect = true;
                    String newURL = connection.getHeaderField("Location");
                    connection.disconnect();
                    connection = (HttpURLConnection) new URL(newURL).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Authorization", Config.TOKEN);
                    connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
                    connection.setRequestProperty("Accept","*/*");
                    connection.setRequestProperty("Content-Type", "application/json");
                } else {
                    isRedirect = false;
                }
            } while (isRedirect);

            String json;
            if ((connection.getResponseCode() < 200) || (connection.getResponseCode() >= 300))
            {
                final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                final StringBuilder builder = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                {
                    builder.append(inputLine);
                }
                json = builder.toString();
            } else {

                InputStream response = connection.getInputStream();
                Reader reader = new InputStreamReader(response, StandardCharsets.UTF_8);

                StringBuilder sb = new StringBuilder();
                int ch;
                while ((ch = response.read()) != -1) sb.append((char) ch);
                json = sb.toString();
            }

            Gson g = new Gson();
            ret = g.fromJson(json, _class);

        } catch (Exception e) {
            e.printStackTrace();
            ret = null;
        }
        return responseString;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (callback != null) {
            callback.OnResult(ret);
        }
    }
}