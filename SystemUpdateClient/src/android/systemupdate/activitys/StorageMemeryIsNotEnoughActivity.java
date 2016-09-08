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


public class StorageMemeryIsNotEnoughActivity extends AlertActivity implements DialogInterface.OnClickListener {

    static final String TAG = "StorageMemeryIsNotEnoughActivity";

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
       
        Intent intent = getIntent();
        long pack_length  = intent.getLongExtra("pack_length", 0);
        long storage_leave  = intent.getLongExtra("storage_leave", 0);

        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.SNE_title);
        p.mIconId = R.drawable.ic_dialog_alert;
        String msg = String.format(getString(R.string.SNE_msg_format), String.valueOf(pack_length/1024/1024), String.valueOf(storage_leave/1024/1024));
        p.mMessage = msg;
        p.mPositiveButtonText = getString(R.string.SNE_btn_ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = null;
        p.mNegativeButtonListener = null;
        setupAlert();
    }
    
    public void onClick(DialogInterface dialog, int which) {
        finish();
    }
}

