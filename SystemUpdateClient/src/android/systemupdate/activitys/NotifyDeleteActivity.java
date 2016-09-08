package android.systemupdate.activitys;

import java.io.File;

import android.systemupdate.service.*;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.systemupdate.service.SystemUpdateService;
import android.systemupdate.service.SystemUpdateService.LocalBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class NotifyDeleteActivity extends Activity {
	private static String TAG = "NotifyDeleteActivity";
	private Context mContext;
	private SystemUpdateService.LocalBinder mBinder;
	private boolean mIfDelete = true;
	private String mPath;
    	private static final boolean DEBUG = true;
    
    private ServiceConnection mConnection = new ServiceConnection() { 
        public void onServiceConnected(ComponentName className, IBinder service) { 
        	mBinder = (SystemUpdateService.LocalBinder)service;
        	if(mIfDelete) {
        		mBinder.deletePackage(mPath);
        	}
        	mBinder.unLockWorkHandler();
        	finish();
        } 

        public void onServiceDisconnected(ComponentName className) { 
        	mBinder = null;
        } 
    }; 
	
	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestory.........");
		mContext.unbindService(mConnection);
		super.onDestroy();
	}
	
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "onKeyDown...........");
		return super.onKeyDown(keyCode, event);
	}



	@Override
	protected void onStop() {
		mContext.bindService(new Intent(mContext, SystemUpdateService.class), mConnection, Context.BIND_AUTO_CREATE);
		super.onStop();
	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
        setContentView(R.layout.notify_dialog);
        //getWindow().setTitle("This is just a test");
        //getWindow().addFlags(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
        //getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
         //       android.R.drawable.ic_dialog_alert);
        Intent startIntent = getIntent();
        TextView text= (TextView)this.findViewById(R.id.notify);
        String flag = startIntent.getStringExtra("flag");
        mPath = startIntent.getStringExtra("path");
        
        Log.d(TAG, "flag = " + flag);
		if(flag.equals(SystemUpdateService.UPDATE_FLAG_SUCCESS)) {
        	text.setText(getString(R.string.update_success) + getString(R.string.ask_delete_package));
	        }else{
        	text.setText(getString(R.string.update_failed) + getString(R.string.ask_delete_package));
        }
        
		Button btn_ok = (Button)this.findViewById(R.id.button_ok);
		Button btn_cancel = (Button)this.findViewById(R.id.button_cancel);
		btn_ok.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				mIfDelete = true;
				mContext.bindService(new Intent(mContext, SystemUpdateService.class), mConnection, Context.BIND_AUTO_CREATE);
			}
		});
		
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				mIfDelete = false;
				mContext.bindService(new Intent(mContext, SystemUpdateService.class), mConnection, Context.BIND_AUTO_CREATE);
			}
		});
	}
	
}

