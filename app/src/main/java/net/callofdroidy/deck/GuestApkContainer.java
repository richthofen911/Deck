package net.callofdroidy.deck;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import dalvik.system.DexClassLoader;

/**
 * Created by yli on 05/12/16.
 */

public class GuestApkContainer {
    private final String TAG = "GuestApkContainer";

    // this is a reference of host apk's MainActivity
    private Activity localMainActivity;

    private Class<?> guestActivityClass;
    private Object guestActivityInstance;
    private Class<?> guestResIdProvider;

    private String downloadApkPath = "";

    public GuestApkContainer(Activity localMainActivity, String downloadApkPath){
        this.localMainActivity = localMainActivity;
        this.downloadApkPath = downloadApkPath;
    }


    public void loadApk(){
        try{
            // init host DexClassLoader
            ClassLoader localClassLoader = ClassLoader.getSystemClassLoader();
            DexClassLoader localDexLoader = new DexClassLoader(downloadApkPath, localMainActivity.getApplicationInfo().dataDir, null, localClassLoader);

            // load pluginId provider
            //guestResIdProvider = localDexLoader.loadClass("callofdroidy.net.deviceinfo.PluginIds");

            //Method getMainLayoutId = guestResIdProvider.getMethod("getMainLayoutId");
            //int mainLayoutId = (int) getMainLayoutId.invoke(guestResIdProvider);
            //localMainActivity.setContentView(mainLayoutId);


            // try to construct GuestMainActivity
            PackageInfo guestPackageInfo = localMainActivity.getPackageManager().getPackageArchiveInfo(downloadApkPath, PackageManager.GET_ACTIVITIES);
            if((guestPackageInfo.activities != null) && (guestPackageInfo.activities.length > 0)){
                String activityName = guestPackageInfo.activities[0].name;
                guestActivityClass = localDexLoader.loadClass(activityName);

                Constructor<?> guestConstructor = guestActivityClass.getConstructor(Context.class);

                guestActivityInstance = guestConstructor.newInstance(localMainActivity); // InvocationTargetException here

                // try to start guest activity
                try{
                    startGuestActivity();
                }catch (Exception e){
                    Log.e(TAG, "loadApk: " + e.toString());
                }

            }else {
                Toast.makeText(localMainActivity, "Guest APK's activities are not found", Toast.LENGTH_SHORT).show();
            }
        }catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | InstantiationException e){
                Log.e(TAG, "loadApk: " + e.toString());
        }
    }

    private void startGuestActivity() throws Exception{
        Log.e(TAG, "startGuestActivity: arrive here");
        Method onCreateMethod = guestActivityClass.getDeclaredMethod("onCreate", Bundle.class);
        onCreateMethod.setAccessible(true);
        onCreateMethod.invoke(guestActivityInstance, new Object[]{null});
    }


}
