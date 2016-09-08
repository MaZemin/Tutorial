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
import android.content.DialogInterface;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;


/**
 * �� activity ��ʾ�û� "�� �豸���ض�Ŀ¼��û���ҵ� ʹ���ض���Ƶ� �̼������ļ�." 
 */
public class NoUpdateImageActivity extends AlertActivity implements DialogInterface.OnClickListener {

    static final String TAG = "NoUpdateImageActivity";

    private static final boolean DEBUG = true;
    // private static final boolean DEBUG = false;

    private static void LOG(String msg) {
        if ( DEBUG ) {
            Log.d(TAG, msg);
        }
    }
    
    /*-------------------------------------------------------*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /* <set up the "dialog".> */
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.NIA_title);
        p.mIconId = R.drawable.ic_dialog_alert;
        String msg = String.format(getString(R.string.NIA_msg_format),
        					SystemUpdateService.FLASH_ROOT, SystemUpdateService.SDCARD_ROOT);
        p.mMessage = msg;
        p.mPositiveButtonText = getString(R.string.NIA_btn_ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = null;
        p.mNegativeButtonListener = null;
        setupAlert();
    }
    
    /**
     * �� DialogInterface.OnClickListener() �ľ���ʵ��. 
     */
    public void onClick(DialogInterface dialog, int which) {
        finish();
    }
}

