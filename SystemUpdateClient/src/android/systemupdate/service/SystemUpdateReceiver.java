/* 
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package android.systemupdate.service;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Bundle;
import android.os.Message;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.systemupdate.activitys.SettingActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.content.Intent;
import android.view.View;
import android.hardware.usb.UsbManager;
import android.app.Activity;
import java.io.File;

import cn.ktc.library.update.Update;
import cn.ktc.library.update.Version;
import android.util.Log;

public class SystemUpdateReceiver extends BroadcastReceiver{
    private final static String TAG = "SystemUpdateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "action = " + action);
        Intent serviceIntent, bootCheckIntent;
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "SystemUpdateReceiver recv ACTION_BOOT_COMPLETED.");
            //serviceIntent = new Intent("android.systemupdate.service");
            serviceIntent = new Intent();
            serviceIntent.setAction("android.systemupdate.service");
            serviceIntent.setPackage("android.systemupdate.service");
            serviceIntent.putExtra("command", SystemUpdateService.COMMAND_CHECK_LOCAL_INER_UPDATING);
            serviceIntent.putExtra("delay", 1000);
            context.startService(serviceIntent);
            
            //bootCheckIntent = new Intent("android.systemupdate.service");
            bootCheckIntent = new Intent();
            bootCheckIntent.setAction("android.systemupdate.service");
            bootCheckIntent.setPackage("android.systemupdate.service");
            bootCheckIntent.putExtra("command", SystemUpdateService.COMMAND_CHECK_REMOTE_UPDATING_BY_BOOT);
            bootCheckIntent.putExtra("delay", 1000);
            context.startService(bootCheckIntent);
        }else if( action.equals(Intent.ACTION_MEDIA_MOUNTED) ) {
            Log.d(TAG,"systemUpdateReceiver recv ACTION_MEDIA_MOUNTED.");
            String path = (String) intent.getData().getPath();
            //serviceIntent = new Intent("android.systemupdate.service");
            serviceIntent = new Intent();
            serviceIntent.setAction("android.systemupdate.service");
            serviceIntent.setPackage("android.systemupdate.service");
            serviceIntent.putExtra("command", SystemUpdateService.COMMAND_CHECK_LOCAL_EXT_UPDATING);
            serviceIntent.putExtra("delay", 1000);
	        serviceIntent.putExtra("path", path);	
            context.startService(serviceIntent);
            Log.d(TAG, "media is mounted to '" + path + "'. To check local update." );
 
        }else if(action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
        	ConnectivityManager cmanger = (ConnectivityManager)context.getSystemService(context.CONNECTIVITY_SERVICE);
        	NetworkInfo netInfo = cmanger.getActiveNetworkInfo();
        	if(netInfo != null) {
        		//if(netInfo.getType() == ConnectivityManager.TYPE_WIFI && netInfo.isConnected()) {
        		if(netInfo.isConnected()){
	        		//serviceIntent = new Intent("android.systemupdate.service");
	        		serviceIntent = new Intent();
            	serviceIntent.setAction("android.systemupdate.service");
            	serviceIntent.setPackage("android.systemupdate.service");
	                serviceIntent.putExtra("command", SystemUpdateService.COMMAND_CHECK_REMOTE_UPDATING);
	                serviceIntent.putExtra("delay", 3000);
	                context.startService(serviceIntent);
        		}
        	}
        }
    }
}


