package android.systemupdate.activitys;

import android.systemupdate.service.*;


import android.content.IntentFilter;
import android.content.Context;
import android.os.Bundle;
import java.util.Formatter;
import java.util.Locale;

import java.lang.StringBuilder;
import android.os.HandlerThread;
import android.os.Handler;
import android.view.WindowManager;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.os.Message;
import android.systemupdate.service.SystemUpdateService;
import android.util.Log;
import android.content.Intent;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Looper;
import android.app.Dialog;
import android.os.Environment;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;


public class SystemUpdatingActivity extends AlertActivity implements DialogInterface.OnClickListener {

    static final String TAG = "SystemUpdatingActivity";

    private static final boolean DEBUG = true;
    // private static final boolean DEBUG = false;

    private static void LOG(String msg) {
        if ( DEBUG ) {
            Log.d(TAG, msg);
        }
    }
    
    /*-------------------------------------------------------*/

    private static final int POSITIVE_BUTTON = AlertDialog.BUTTON1;

    /*-------------------------------------------------------*/
    
    /** �ڲ� flash �洢�豸�� root Ŀ¼. */
    private  String FLASH_ROOT = SystemUpdateService.FLASH_ROOT;

    /** sdcard �� root Ŀ¼. */
    private  String SDCARD_ROOT = SystemUpdateService.SDCARD_ROOT;
    
    /*-------------------------------------------------------*/
    
    /** �����ļ�·���ִ�. */
    private String mImageFilePath;

    /** �����ļ��汾��Ϣ. */
    private String mImageVersion;
    
    /** ��ǰϵͳ�̼��İ汾��Ϣ. */
    private String mCurrentVersion;
    
    /*-------------------------------------------------------*/

    /** ����ʵ�ֶ��ִ� ��ʽ������. */
    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());

    /*-------------------------------------------------------*/

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context,Intent intent) {
            LOG("mReceiver.onReceive() : 'action' =" + intent.getAction() ); 

            if(intent.getAction() == Intent.ACTION_MEDIA_UNMOUNTED) {
                String path = intent.getData().getPath();   /* �ҽӵ�·���ִ�. */
                LOG("mReceiver.onReceive() : original mount point : " + path + "; image file path : " + mImageFilePath);
                /* �� "mImageFilePath" �� "path" ��, ��... */
                if ( mImageFilePath != null && mImageFilePath.contains(path) ) {
                    LOG("mReceiver.onReceive() : Media that img file live in is unmounted, to finish this activity.");
                    /* ���� this. */
                    finish();
                }
            }
        }
    };
    
    /*-------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOG("onCreate() : Entered.");
        super.onCreate(savedInstanceState);
        
        /* ��ȡ ���� �� Activity �� Intent �� extra ��� : �����ļ��� ·���Ͱ汾��Ϣ. */
        Bundle extr = getIntent().getExtras();
        mImageFilePath = extr.getString(SystemUpdateService.EXTRA_IMAGE_PATH);
        mImageVersion = extr.getString(SystemUpdateService.EXTRA_IMAGE_VERSION);
        mCurrentVersion = extr.getString(SystemUpdateService.EXTRA_CURRENT_VERSION);

        /* <set up the "dialog".> */
        final AlertController.AlertParams p = mAlertParams;
        // p.mIconId = com.android.internal.R.drawable.ic_dialog_updating;
        p.mTitle = getString(R.string.updating_title);
        /* ���� message �ִ�. */
        String messageFormat = getString(R.string.updating_message_formate);
        sFormatBuilder.setLength(0);
        sFormatter.format(messageFormat, mImageFilePath);
        p.mMessage = sFormatBuilder.toString();
        /* Ԥ�� buttons �� �ı����¼�����ص�. */
        p.mPositiveButtonText = getString(R.string.updating_button_install);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.updating_button_cancel);
        p.mNegativeButtonListener = this;

        setupAlert();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");
        registerReceiver(mReceiver, filter);
    }


    @Override
    protected void onResume() {
        super.onResume();
 
        if ( false ) {
        /* ��ȡ flash �� mount ״̬. */
        String state = Environment.getExternalStorageState();
        /* ����, �� ������ sdcard ��, �� sdcard ���� "mounted" ���� "mounted_ro" ��״̬, �� ... */
        if ( mImageFilePath.contains(SDCARD_ROOT) 
            && (!(state.equals(Environment.MEDIA_MOUNTED) || state.equals(Environment.MEDIA_MOUNTED_READ_ONLY) ) ) ) {
            LOG("onResume() : Image file was in /sdcard, but sdcard storage is umounted. To finish.");
            finish();
        }
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        LOG("onPause() : Entered.");
        
        // .! : 
        // finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LOG("onDestroy() : Entered.");
        
        mImageFilePath = null;
        mImageVersion = null;
        mCurrentVersion = null;
       
        unregisterReceiver(mReceiver);
    }

    /**
     * �� DialogInterface.OnClickListener() �ľ���ʵ��. 
     */
    public void onClick(DialogInterface dialog, int which) {

        if (which == POSITIVE_BUTTON) {
            /* < ���� "���û������������ʾ�� activity." > */
            Intent intent = new Intent();
            intent.setClass(this, SystemUpdateAndRebootActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(SystemUpdateService.EXTRA_IMAGE_PATH, mImageFilePath);
            startActivity(intent);
        }

        // No matter what, finish the activity
        finish();
    }
}
