package android.systemupdate.activitys;

import android.systemupdate.service.*;


import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.IntentFilter;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import java.util.Formatter;
import java.util.Locale;
import android.systemupdate.*;
import android.systemupdate.service.SystemUpdateService;
import android.systemupdate.service.SystemUpdateService.LocalBinder;

import java.io.File;
import java.lang.StringBuilder;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.IBinder;
import android.view.WindowManager;
import android.content.BroadcastReceiver;
import android.os.Message;
import android.util.Log;
import android.content.Intent;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.os.Looper;
import android.app.Dialog;
import android.os.Environment;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;


/**
 * �� activity ��ʾ�û� "device �� reboot ����� update, �� ...", �� reboot ֮ǰ�� ���� UI. 
 */
public class SystemUpdateAndRebootActivity extends AlertActivity {

    static final String TAG = "SystemUpdateAndRebootActivity";
    private Context mContext;
    private static final boolean DEBUG = true;
    // private static final boolean DEBUG = false;

    private static void LOG(String msg) {
        if ( DEBUG ) {
            Log.d(TAG, msg);
        }
    }
    /*-------------------------------------------------------*/
    
    /** �����ļ�·���ִ�. */
    private String mImageFilePath;

    /*-------------------------------------------------------*/

    /** ִ���� ������ work thread �е� work handler ʵ��. */
    private WorkHandler mWorkHandler;
    private UiHandler mUiHandler;
    private SystemUpdateService.LocalBinder mBinder;
    /*-------------------------------------------------------*/
    
    private ServiceConnection mConnection = new ServiceConnection() { 
        public void onServiceConnected(ComponentName className, IBinder service) { 
        	mBinder = (SystemUpdateService.LocalBinder)service;
        	/* ���� �ӳٵ� cmd msg. */
            mWorkHandler.sendEmptyMessageDelayed(WorkHandler.COMMAND_START_UPDATING , 3000);
        } 

        public void onServiceDisconnected(ComponentName className) { 
        	mBinder = null;
        } 
}; 


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        /* ��ȡ ���� �� Activity �� Intent �� extra ��� : �����ļ��� ·��. */
        Bundle extr = getIntent().getExtras();
        mImageFilePath = extr.getString(SystemUpdateService.EXTRA_IMAGE_PATH);
        
        /* <set up the "dialog".> */
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.updating_title);
        p.mIconId = R.drawable.ic_dialog_alert;
        String msg = getText(R.string.updating_prompt).toString();
        /* �� �����ļ��� SD card ��, �� ... */
        if ( mImageFilePath.contains(SystemUpdateService.SDCARD_ROOT)) {
            /* �� "p.mMessage" ����� "����γ� SD card" ����ʾ. */
            msg += getText(R.string.updating_prompt_sdcard).toString();
        }
        p.mMessage = msg;
        p.mPositiveButtonText = null;
        p.mPositiveButtonListener = null;
        p.mNegativeButtonText = null;
        p.mNegativeButtonListener = null;

        setupAlert();
        
        /*-----------------------------------*/
        LOG("onCreate() : start 'work thread'.");
        /* ���� work thread. */
        HandlerThread workThread = new HandlerThread("SystemUpdateAndRebootActivity : work thread");
        /* ����. */
        workThread.start();
        /* ���� "mWorkHandler". */
        mWorkHandler = new WorkHandler(workThread.getLooper() );
        mUiHandler = new UiHandler();      
        mContext.bindService(new Intent(mContext, SystemUpdateService.class), mConnection, Context.BIND_AUTO_CREATE); 
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        LOG("onPause() : Entered.");
    }

    private class UiHandler extends Handler {
    	private static final int COMMAND_START_CHECK_FAILD = 1;
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case COMMAND_START_CHECK_FAILD:
				dialog();
				break;
			}
		}
    	
    }
    
    /*-------------------------------------------------------*/
    
    /* @see mWorkHandler. */
    private class WorkHandler extends Handler {
        
        /** �������߳�(UI ���߳�) ��������, "����ײ�ִ�� updating ����" ������ msg. */
        private static final int COMMAND_START_UPDATING = 1;
        
        public WorkHandler(Looper looper) {
            super(looper);
        }
        
        public void handleMessage(Message msg) {       

            switch (msg.what) {
                case COMMAND_START_UPDATING:
                    LOG("WorkHandler::handleMessage() : To perform 'COMMAND_START_UPDATING'.");

                    if(mBinder != null){
                    	if(mImageFilePath.endsWith("img")){
                    		//rkimge update mode
                    		mBinder.updateFirmware(mImageFilePath, SystemUpdateService.RKUPDATE_MODE);
                    	}else{
                    		//ota update mode
                    		if(!(mBinder.doesOtaPackageMatchProduct(mImageFilePath))){
                    			mUiHandler.sendEmptyMessage(UiHandler.COMMAND_START_CHECK_FAILD);
                    		}else{
                    			mBinder.updateFirmware(mImageFilePath, SystemUpdateService.OTAUPDATE_MODE);
                    		}
                    	}
                    }else {
                    	Log.d(TAG, "service have not connected!");
                    }
                    break;

                /*---------------*/
                default:
                    break;
            }
        }
    }
    
    protected void dialog() {
    	  AlertDialog.Builder builder = new Builder(mContext);
    	  builder.setMessage("not a valid update package !");

    	  builder.setTitle("error");

    	  builder.setPositiveButton("OK", new OnClickListener() {

    	   public void onClick(DialogInterface dialog, int which) {
    	    dialog.dismiss();
    	    
            LOG("onClick() : User desided to delete the invalid image file.");
            /* ɾ����Ч�ľ����ļ�. */
            if ( !( (new File(mImageFilePath) ).delete() ) ) {
                Log.w(TAG, "onClick() : Failed to delete invalid image file : " + mImageFilePath);
            }

    	    finish();
    	   }
    	  });

    	  builder.create().show();
    }

}
