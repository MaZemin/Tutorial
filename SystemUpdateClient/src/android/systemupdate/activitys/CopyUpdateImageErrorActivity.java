package android.systemupdate.activitys;

import android.systemupdate.service.*;
import android.content.DialogInterface;
import android.os.Bundle;
import android.systemupdate.service.SystemUpdateService;
import android.util.Log;
import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;


/**
 * 本 activity 提示用户 在特定路径发现无效 固件镜像文件, 询问用户是否删除该文件. 
 * @see UpdateService
 */
public class CopyUpdateImageErrorActivity extends AlertActivity implements DialogInterface.OnClickListener {

    static final String TAG = "CopyUpdateImageErrorActivity";

    private static final boolean DEBUG = true;
    // private static final boolean DEBUG = false;

    private  String mErrorMsg;
    private static void LOG(String msg) {
        if ( DEBUG ) {
            Log.d(TAG, msg);
        }
    }
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LOG("--------CopyUpdateImageErrorActivity : onCreate() : Entered.");
        
        /* 获取 启动 本 Activity 的 Intent 的 extra 数据 : 无效的镜像文件的 路径和版本信息. */
        Bundle extr = getIntent().getExtras();
        mErrorMsg = extr.getString(SystemUpdateService.EXTRA_ERR_MESSAGE);

        /* <set up the "dialog".> */
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = R.drawable.ic_dialog_alert;
        p.mTitle = getString(R.string.CIEA_title);
        p.mMessage = mErrorMsg;
        /* 预置 buttons 的 文本和事件处理回调. */
        p.mPositiveButtonText = getString(R.string.NIA_btn_ok);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = null;//getString(R.string.IFIA_btn_no);
        p.mNegativeButtonListener = null;//this;

        setupAlert();
    }


    /**
     * 对 DialogInterface.OnClickListener() 的具体实现. 
     */
    public void onClick(DialogInterface dialog, int which) {
        // No matter what, finish the activity
        finish();
    }
}
