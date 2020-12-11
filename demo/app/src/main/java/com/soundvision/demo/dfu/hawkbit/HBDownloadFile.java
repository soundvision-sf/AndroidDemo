package com.soundvision.demo.dfu.hawkbit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.soundvision.demo.utils.SVHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class HBDownloadFile extends AsyncTask<String, String, String> {

    private static final String NEW_UPGRADE_FILE = "Hawkbit.NEW_UPGRADE_FILE";
    private static final String NEW_UPGRADE_FILE_MD5 = "Hawkbit.NEW_UPGRADE_FILE_MD5";

    public interface OnFirmwareUpdateCallback
    {
        void OnNewUpdate(String filename, long size);
    }

    OnFirmwareUpdateCallback callback;
    private Context context;
    private String lastMD5 = "";
    private long fileSize = 0;
    private String lastUpdateFile = "";

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //showDialog(progress_bar_type);
    }

    public HBDownloadFile setContext(Context c, OnFirmwareUpdateCallback callback)
    {
        context = c;
        this.callback = callback;
        return this;
    }

    private String downloadFile(String downloadUrl, String dstFile) {
        try {

            MessageDigest md = MessageDigest.getInstance("MD5");

            URL url = new URL(downloadUrl);
            String outputFile = dstFile;
            URLConnection connection = url.openConnection();
            connection.connect();

            int lenghtOfFile = connection.getContentLength();

            InputStream input = new BufferedInputStream(url.openStream(),
                    1024);

            // Output stream
            OutputStream output = new FileOutputStream(outputFile);

            byte data[] = new byte[1024];
            DigestInputStream dis = new DigestInputStream(input, md);
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                output.write(data, 0, count);
            }

            // flushing output
            output.flush();
            fileSize = total;

            // closing streams
            output.close();
            input.close();
            return SVHelper.ByteArrayToHash(md.digest());
        }
        catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }
        return "";
    }

    @Override
    protected String doInBackground(String... f_urls) {

        String upgradeFile = f_urls[0];
        String upgradeMD5  = f_urls[1];
        lastUpdateFile =  Environment.getExternalStorageDirectory().toString()
                + "/" + f_urls[2];

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        lastMD5 = preferences.getString(NEW_UPGRADE_FILE_MD5, "");

        Log.i("MD5: upgradeMD5 - ", upgradeMD5); // last saved MD5

        // is new upgrade
        if (!lastMD5.toLowerCase().equals(upgradeMD5.toLowerCase())) {
            downloadFile(upgradeFile, lastUpdateFile);

            lastMD5 = SVHelper.calculateMD5(lastUpdateFile);

            Log.i("MD5: lastMD5    - ", lastMD5);
            if (lastMD5.toLowerCase().equals(upgradeMD5.toLowerCase())) {
                return lastUpdateFile;
            } else {
                return null; // Error MD5 check
            }
        } else {
            File f = new File(lastUpdateFile);
            fileSize = f.length();
            return lastUpdateFile;
        }
    }

    protected void onProgressUpdate(String... progress) {
        // setting progress percentage
        //.setProgress(Integer.parseInt(progress[0]));
    }

    @Override
    protected void onPostExecute(String file_url) {

        if (file_url != null) {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(NEW_UPGRADE_FILE, file_url);
            editor.putString(NEW_UPGRADE_FILE_MD5, lastMD5);
            editor.apply();
            if (callback != null)
                callback.OnNewUpdate(file_url, fileSize);
        }


    }

}