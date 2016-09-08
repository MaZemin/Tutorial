package android.systemupdate.activitys;

import android.systemupdate.service.*;

import android.content.IntentFilter;
import android.content.Context;
import android.os.Bundle;
import java.util.Formatter;
import java.util.Locale;

import java.lang.StringBuilder;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.systemupdate.service.SystemUpdateService;
import android.util.Log;
import android.content.Intent;
import android.app.AlertDialog;
import java.io.File;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;


/**
 * �� activity ��ʾ�û� ���ض�·��������Ч �̼������ļ�, ѯ���û��Ƿ�ɾ����ļ�. 
 * @see UpdateService
 */
public class InvalidUpdateImageActivity extends AlertActivity implements DialogInterface.OnClickListener {

    static final String TAG = "InvalidUpdateImageActivity";

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
    
    /** sdcard �� root Ŀ¼. */
    private String SDCARD_ROOT = SystemUpdateService.SDCARD_ROOT;
    
    /*-------------------------------------------------------*/

    /** ��Ч�ľ����ļ� ·���ִ�. */
    private String mImageFilePath;

    /*-------------------------------------------------------*/

    /** �û�ʵ�ֶ��ִ� ��ʽ������. */
    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());

    /*-------------------------------------------------------*/

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context,Intent intent) {
            LOG("onReceive() : 'action' = " + intent.getAction() );
            
            if(intent.getAction() == Intent.ACTION_MEDIA_UNMOUNTED) {
                String path = intent.getData().getPath();   /* �ҽӵ�·���ִ�. */
                LOG("mReceiver.onReceive() : original mount point : " + path + "; image file path : " + mImageFilePath);
                /* �� "mImageFilePath" �� "path" ��, ��... */
                if ( mImageFilePath != null && mImageFilePath.contains(path) ) {
                    LOG("mReceiver.onReceive() : Media that image file lives in is unmounted, to finish this activity.");
                    /* ���� this. */
                    finish();
                }
            }
        }
    };
    
    /*-------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LOG("onCreate() : Entered.");
        
        /* ��ȡ ���� �� Activity �� Intent �� extra ��� : ��Ч�ľ����ļ��� ·���Ͱ汾��Ϣ. */
        Bundle extr = getIntent().getExtras();
        mImageFilePath = extr.getString(SystemUpdateService.EXTRA_IMAGE_PATH);

        /* <set up the "dialog".> */
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = R.drawable.ic_dialog_alert;
        p.mTitle = getString(R.string.IFIA_title);
        /* ���� message �ִ�. */
        String messageFormat = getString(R.string.IFIA_msg);
        sFormatBuilder.setLength(0);
        sFormatter.format(messageFormat, mImageFilePath);
        p.mMessage = sFormatBuilder.toString();
        /* Ԥ�� buttons �� �ı����¼�����ص�. */
        p.mPositiveButtonText = getString(R.string.IFIA_btn_yes);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.IFIA_btn_no);
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
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LOG("onDestroy() : Entered.");
        
        mImageFilePath = null;
       
        unregisterReceiver(mReceiver);
    }

    /**
     * �� DialogInterface.OnClickListener() �ľ���ʵ��. 
     */
    public void onClick(DialogInterface dialog, int which) {

        if (which == POSITIVE_BUTTON) {
            LOG("onClick() : User desided to delete the invalid image file.");
            /* ɾ����Ч�ľ����ļ�. */
            if ( !( (new File(mImageFilePath) ).delete() ) ) {
                Log.w(TAG, "onClick() : Failed to delete invalid image file : " + mImageFilePath);
            }
        }

        // No matter what, finish the activity
        finish();
    }
}
