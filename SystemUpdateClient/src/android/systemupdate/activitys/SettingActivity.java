/**be added to the setting app **/
package android.systemupdate.activitys;

import cn.ktc.library.update.Update;
import cn.ktc.library.update.Version;
import android.systemupdate.service.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class SettingActivity extends Activity {
	private static final String TAG = "SystemUpdateService.Setting";
	private Switch mSwh_AutoCheck;
	private Button mBtn_CheckNow, mBtn_download, mBtn_exit;
	private SharedPreferences mAutoCheckSet;
	private TextView mText_ota;
    //ota apk update, zjd20150729,start
  	private Boolean isPass=false;
  	private CheckNewVersionTask mCheckNewVersionTask=null;
  	private AlertDialog alertDialog=null;
  	private Context mContext;
  	//ota apk update, zjd20150729,end
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting);
		mContext = this;
		mSwh_AutoCheck = (Switch)this.findViewById(R.id.swh_auto_check);
		//mSwh_AutoCheck.setVisibility(View.GONE);//zjd20150527
		//mBtn_CheckNow = (Button)this.findViewById(R.id.btn_check_now);
		mBtn_download = (Button)this.findViewById(R.id.btn_download);
		mBtn_exit = (Button)this.findViewById(R.id.btn_exit);
		mText_ota = (TextView)this.findViewById(R.id.text_ota_avalibe);
		boolean isAvalible = SystemProperties.getBoolean("persist.sys.ota.available", false);
		if(isAvalible)
			mText_ota.setText(R.string.str_ota_new);
		else
			mText_ota.setText(R.string.str_ota_no_new);
		
		mAutoCheckSet = getSharedPreferences("auto_check", MODE_PRIVATE);
		mSwh_AutoCheck.setChecked(mAutoCheckSet.getBoolean("auto_check", false));//true->false,zjd20150522
		mSwh_AutoCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor e = mAutoCheckSet.edit();
				e.putBoolean("auto_check", isChecked);
				e.commit();
			}

		});
		
		/*mBtn_CheckNow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent serviceIntent;
				serviceIntent = new Intent("android.systemupdate.service");
                serviceIntent.putExtra("command", SystemUpdateService.COMMAND_CHECK_REMOTE_UPDATING_BY_HAND);
                mContext.startService(serviceIntent);
			}
			
		});*/
		
		mBtn_download.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent serviceIntent;
				serviceIntent = new Intent("android.systemupdate.service");
                serviceIntent.putExtra("command", SystemUpdateService.COMMAND_CHECK_REMOTE_UPDATING_BY_HAND);
                mContext.startService(serviceIntent);
			}
		});
		
		mBtn_exit.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}			
		});
	}
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		//ota apk update, zjd20150729,start
		IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mUpdateReceiver, filter);
        //ota apk update, zjd20150729,end
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		//ota apk update, zjd20150729,start
		unregisterReceiver(mUpdateReceiver);
		//ota apk update, zjd20150729,end
		super.onDestroy();
	}
	
	private final BroadcastReceiver mUpdateReceiver=new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {	
			String action=intent.getAction();
			if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)){
            	if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false)) {
            		Log.i(TAG, "EXTRA_NO_CONNECTIVITY:");
        		} else {
        			updateApk();
        		}
			}
		}
	};
	
	//ota apk update, zjd20150729,start
	public void updateApk(){
        if (mCheckNewVersionTask != null 
                && mCheckNewVersionTask.getStatus() != AsyncTask.Status.FINISHED) {
            mCheckNewVersionTask.cancel(true);
        }
        mCheckNewVersionTask = new CheckNewVersionTask();
        mCheckNewVersionTask.execute();
    }
    
       /**
     * @author yejb
     *
     */
    private class CheckNewVersionTask extends AsyncTask<Void, Void, Version>  {
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected Version doInBackground(Void... params) {
        	Update update=new Update(getApplicationContext());//getApplicationContext(),mContext
            return update.hasNewVersion();
        }

        @Override
        protected void onPostExecute(Version result) {
            if (result != null) {
                if (result != null) {
                    showNewVersionDialog(result);
                }
            }
            super.onPostExecute(result);
        }
    }
    
    /**
     * @param version
     */
    
    private void showNewVersionDialog(final Version version) {
        if(alertDialog==null){
            alertDialog=new AlertDialog.Builder(getApplicationContext())
            .setTitle(R.string.new_version_text)
            .setMessage(version.getIntroduction())
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    Update update = new Update(mContext);
                    update.setVersion(version);
                    update.checkUpdate();
                }
            })
            .setNeutralButton(R.string.skip_this_version, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    isPass=true;
                }
            })
            .create();
        }
        if(!alertDialog.isShowing()){
            //alertDialog.show();  
        }
        
        Update update = new Update(mContext);
        update.setVersion(version);
        update.checkUpdate();
    }
	//ota apk update, zjd20150729,end

	
}
