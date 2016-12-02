package net.callofdroidy.deck;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import dalvik.system.DexClassLoader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    AssetManager assetManager;
    Resources resources;
    DownloadTask downloadTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(0x7f04001a);

        new DownloadTask(this).execute("http://192.168.128.54:8000/plugin.apk");

    }


    // the parameter "path" is the path of apk file, can be accessed by getPackageManager().getApplicationInfo("xxx", 0).sourceDir;
    public void loadRes(String path){
        Log.e(TAG, "loadRes: " + path);
        try{
            assetManager= AssetManager.class.newInstance();
            Method addAssetPath = AssetManager.class.getMethod("addAssetPath", String.class);
            addAssetPath.invoke(assetManager, path);
        }catch (Exception e){
            e.printStackTrace();
        }
        resources = new Resources(assetManager, super.getResources().getDisplayMetrics(), super.getResources().getConfiguration());
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        if(downloadTask != null && downloadTask.getStatus() == AsyncTask.Status.RUNNING)
            downloadTask.cancel(true);
    }

    @Override
    public Resources getResources() {
        return resources == null ? super.getResources() : resources;
    }

    @Override
    public AssetManager getAssets() {
        return assetManager == null ? super.getAssets() : assetManager;
    }

    private static class DownloadTask extends AsyncTask<String, Integer, String> {

        private WeakReference<MainActivity> activityWeakReference;
        private MainActivity mainActivity;
        DexClassLoader dexClassLoader;

        public DownloadTask(MainActivity activity) {
            this.activityWeakReference = new WeakReference<MainActivity>(activity);
            this.mainActivity = activityWeakReference.get();
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                inputStream = connection.getInputStream();

                Log.e(TAG, "doInBackground: IS size: " + inputStream.available());

                if(mainActivity != null){
                    File downloadedApk = new File(mainActivity.getCacheDir().getAbsoluteFile() + File.separator + "plugin.apk");
                    if(!downloadedApk.exists()){
                        try{
                            downloadedApk.createNewFile();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    outputStream = new FileOutputStream(downloadedApk);
                }

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = inputStream.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        inputStream.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    outputStream.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (outputStream != null)
                        outputStream.close();
                    if (inputStream != null)
                        inputStream.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result){
            String pluginPath = mainActivity.getCacheDir().getAbsolutePath() + File.separator + "plugin.apk";

            Log.e(TAG, "pluginPath: " + pluginPath);
            //dexPath = "/data/data/net.callofdroidy.deck/cache/base.apk";
            mainActivity.loadRes(pluginPath);
            Log.e(TAG, "afterDownload: load res success");

            dexClassLoader = new DexClassLoader(pluginPath, mainActivity.getApplicationInfo().dataDir, null, mainActivity.getClass().getClassLoader());
            try{
                Class<?> clazz = dexClassLoader.loadClass("callofdroidy.net.deviceinfo.PluginIds");
                Method getMainLayoutId = clazz.getMethod("getMainLayoutId");
                int mainLayoutId = (int) getMainLayoutId.invoke(clazz);
                mainActivity.setContentView(mainLayoutId); //this is a lint warning, can be ignored
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
}
