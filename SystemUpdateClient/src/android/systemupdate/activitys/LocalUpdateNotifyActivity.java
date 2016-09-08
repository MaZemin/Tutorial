// 把本地外部U盘中的升级文件拷贝到内部flash的notify

package android.systemupdate.activitys;

import android.systemupdate.service.*;


import java.io.File;
import java.net.URI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class LocalUpdateNotifyActivity extends Activity{
	private String TAG = "LocalUpdateNotifyActivity";
	private Context mContext;
	private String mSrcPath = null;
	private String mDesPath = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.notify_dialog);
		setFinishOnTouchOutside(false);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                android.R.drawable.ic_dialog_alert);
        Intent startIntent = getIntent();
        mSrcPath = startIntent.getStringExtra("source_path");
        mDesPath = startIntent.getStringExtra("dest_path");

        File file = new File(mSrcPath);
        long packageSize = file.length();
        String packageSize_string = null;
        if(packageSize < 1024) {
        	packageSize_string = String.valueOf(packageSize) + "B";
        }else if(packageSize/1024 > 0 && packageSize/1024/1024 == 0) {
        	packageSize_string = String.valueOf(packageSize/1024) + "K";
        }else if(packageSize/1024/1024 > 0) {
        	packageSize_string = String.valueOf(packageSize/1024/1024) + "M";
        }
        TextView txt = (TextView)this.findViewById(R.id.notify);
        txt.setText(getString(R.string.ota_update) + mSrcPath + getString(R.string.ota_package_size) + packageSize_string);
        
        Button btn_ok = (Button)this.findViewById(R.id.button_ok);
		Button btn_cancel = (Button)this.findViewById(R.id.button_cancel);
		btn_ok.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
                                if (SystemUpdateService.getTmpDirFreeSize() < SystemUpdateService.getFileSize(mSrcPath)){
                                        Intent intent = new Intent(mContext, StorageMemeryIsNotEnoughActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.putExtra("pack_length", SystemUpdateService.getFileSize(mSrcPath));
                                        intent.putExtra("storage_leave", SystemUpdateService.getTmpDirFreeSize());
                                        mContext.startActivity(intent);
                                }else{
					Intent intent = new Intent(mContext, CopyPackageProgressActivity.class);
		    			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    					intent.putExtra("source_path", mSrcPath);
    					intent.putExtra("dest_path", mDesPath);
    					mContext.startActivity(intent);
				}

	    			finish();
			}
		});
		
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	@Override
	protected void onStop() {
		finish();
		super.onStop();
	}
}
