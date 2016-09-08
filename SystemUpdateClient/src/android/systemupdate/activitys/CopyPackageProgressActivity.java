package android.systemupdate.activitys;

import android.systemupdate.service.*;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.systemupdate.service.SystemUpdateService;
import android.systemupdate.util.LocalFileCopyTask;
import android.systemupdate.component.RoundProgressBar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RemoteViews;

public class CopyPackageProgressActivity extends Activity {
	private String TAG = "CopyPackageProgressActivity";
	private Context mContext;
	private static PowerManager.WakeLock mWakeLock;
	private String WAKELOCK_KEY = "myLocalCopy";
	private RoundProgressBar mProgressBar;
	private ProgressHandler mProgressHandler;
	private Button mBtnControl;
	private Button mBtnCancel;
	private int mState = STATE_IDLE;
	//private TextView mTxtState;
	private ResolveInfo homeInfo;
	private NotificationManager mNotifyManager;
	private Notification mNotify;
	private int notification_id = 20110921;
	private LocalFileCopyTask mTask;
	private String mSrcPath;
	private String mDesPath;
	private SystemUpdateService.LocalBinder mBinder;
	
	public static final int STATE_IDLE = 0;
	public static final int STATE_STARTING = 1;
	public static final int STATE_STARTED = 2;
	public static final int STATE_STOPING = 3;
	public static final int STATE_STOPED = 4;
	public static final int STATE_ERROR = 5;


	private ServiceConnection mConnection = new ServiceConnection() { 
        public void onServiceConnected(ComponentName className, IBinder service) { 
        	mBinder = (SystemUpdateService.LocalBinder)service;
        	mBinder.LockWorkHandler();
        } 

        public void onServiceDisconnected(ComponentName className) { 
        	mBinder = null;
        } 
        
        
    }; 
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.package_download);
        setFinishOnTouchOutside(false);
        
        mContext = this;
        mContext.bindService(new Intent(mContext, SystemUpdateService.class), mConnection, Context.BIND_AUTO_CREATE);
        
        Intent intent = getIntent();
        mSrcPath = intent.getStringExtra("source_path");
        mDesPath = intent.getStringExtra("dest_path");
		
        //not finish activity
        PackageManager pm = getPackageManager();  
        homeInfo = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
        
        mNotifyManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mNotify = new Notification(R.drawable.ota_update, getString(R.string.app_name), System.currentTimeMillis());
        mNotify.contentView = new RemoteViews(getPackageName(), R.layout.download_notify); 
        mNotify.contentView.setProgressBar(R.id.pb_download, 100, 0, false);
        Intent notificationIntent = new Intent(this, PackageDownloadActivity.class); 
    	PendingIntent pIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0); 
    	mNotify.contentIntent = pIntent;    
    	
    	PowerManager powerManager = (PowerManager) this.getSystemService(this.POWER_SERVICE);
    	mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);
        mProgressBar = (RoundProgressBar)findViewById(R.id.progress_horizontal);
        mBtnControl = (Button)findViewById(R.id.btn_control);
        mBtnCancel = (Button)findViewById(R.id.button_cancel);
        //mTxtState = (TextView)findViewById(R.id.txt_state);
        
        //mTxtState.setText("");       
        mBtnControl.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(mState == STATE_IDLE || mState == STATE_STOPED) {
					//try to start
					mTask = new LocalFileCopyTask(mSrcPath, mDesPath);
					mTask.setProgressHandler(mProgressHandler);
					mTask.start();
					mBtnControl.setText(getString(R.string.starting));
					mBtnControl.setClickable(false);
					mBtnControl.setFocusable(false);
				}else if(mState == STATE_STARTED) {
					//try to stop
					mTask.stopCopy();					
					mBtnControl.setText(getString(R.string.stoping));
					mBtnControl.setClickable(false);
					mBtnControl.setFocusable(false);
				}
			}
		});
        
        mBtnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
        
        //mProgressBar.setIndeterminate(false);
        mProgressBar.setProgress(0);
        mProgressHandler = new ProgressHandler();
		
        //try to start
		mTask = new LocalFileCopyTask(mSrcPath, mDesPath);
		mTask.setProgressHandler(mProgressHandler);
		mTask.start();
		mBtnControl.setText(getString(R.string.starting));
		mBtnControl.setClickable(false);
		mBtnControl.setFocusable(false);
		
    }
    
    private class ProgressHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			int whatMassage = msg.what;
			switch(whatMassage) {
			case LocalFileCopyTask.PROGRESS_UPDATE : {
					Bundle b = msg.getData();
					int percent = b.getInt("percent", 0);
					Log.d(TAG, "percent = " + percent);
					mProgressBar.setProgress(percent);
					setNotificationProgress(percent);
					showNotification();
				}
				break;
			case LocalFileCopyTask.PROGRESS_COPY_COMPLETE : {
					//mTxtState.setText("State: download complete");
					mState = STATE_IDLE;
					mBtnControl.setText(getString(R.string.start));
					mBtnControl.setClickable(true);
					mBtnControl.setFocusable(true);
					Intent intent = new Intent();
		            intent.setClass(mContext, SystemUpdateAndRebootActivity.class);
		            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		            intent.putExtra(SystemUpdateService.EXTRA_IMAGE_PATH, mDesPath);
		            startActivity(intent);
		            finish();
				}
				break;
			case LocalFileCopyTask.PROGRESS_START_COMPLETE : {
					//mTxtState.setText("");
					mState = STATE_STARTED;
					mBtnControl.setText(getString(R.string.pause));
					mBtnControl.setClickable(true);
					mBtnControl.setFocusable(true);
					showNotification();
					mWakeLock.acquire();
				}
				break;
			case LocalFileCopyTask.PROGRESS_STOP_COMPLETE : {
					Bundle b  = msg.getData();
					int errCode = b.getInt("err", LocalFileCopyTask.ERR_NOERR);
					if(errCode == LocalFileCopyTask.ERR_CONNECT_TIMEOUT) {
						//mTxtState.setText("State: ERR_CONNECT_TIMEOUT");
					}else if(errCode == LocalFileCopyTask.ERR_FILELENGTH_NOMATCH) {
						//mTxtState.setText("State: ERR_FILELENGTH_NOMATCH");
					}else if(errCode == LocalFileCopyTask.ERR_NOT_EXISTS) {
						//mTxtState.setText("State: ERR_NOT_EXISTS");
					}else if(errCode == LocalFileCopyTask.ERR_REQUEST_STOP) {
						//mTxtState.setText("State: ERR_REQUEST_STOP");
					}
					
					mState = STATE_STOPED;
					mBtnControl.setText(getString(R.string.retry));
					mBtnControl.setClickable(true);
					mBtnControl.setFocusable(true);
					if(mWakeLock.isHeld()){
						mWakeLock.release();
					}
				}
				break;
                         case LocalFileCopyTask.PROGRESS_ERR_WARRING : {
                                        Bundle b  = msg.getData();
                                        String errInfo = b.getString("errInfo", " no messeage warring !!");
                                        notifyErrorMessage(errInfo);
				}
			default:
				break;
			}
		}  	
    }

       private void notifyErrorMessage(String msgString){
        Intent intent = new Intent(mContext, CopyUpdateImageErrorActivity.class);
        Log.d(TAG, "notifyErrorMessage() : "+msgString);
        /* Ԥ��Ŀ�� Activity. */
       // intent.setComponent(new ComponentName("android.systemupdate.service", "android.systemupdate.activitys.CopyUpdateImageErrorActivity") );
        /* flags. */
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        /* Ԥ�� extra ���. */
        intent.putExtra(SystemUpdateService.EXTRA_ERR_MESSAGE, msgString);

        /* ��������Ŀ�� activity. */
        mContext.startActivity(intent);
    }

    
    private void showNotification() {
    	mNotifyManager.notify(notification_id, mNotify);
    }
    
    private void clearNotification() {
    	mNotifyManager.cancel(notification_id);
    }
    
    private void setNotificationProgress(int percent) {
    	mNotify.contentView.setProgressBar(R.id.pb_download, 100, percent, false);
    }
    
    @Override
	protected void onDestroy() {
		Log.d(TAG, "ondestroy");
		if(mTask != null) {
			mTask.stopCopy();
		}
		if(mWakeLock.isHeld()){
			mWakeLock.release();
		}
		clearNotification();
		if(mBinder != null) {
			mBinder.unLockWorkHandler();
		}
		mContext.unbindService(mConnection);
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
	}

	@Override
	protected void onRestart() {
		Log.d(TAG, "onRestart");
		super.onRestart();
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart");
		super.onStart();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");	
		super.onStop();
	}
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {  
    	if (keyCode == KeyEvent.KEYCODE_BACK) {  
	    	ActivityInfo ai = homeInfo.activityInfo;  
	    	Intent startIntent = new Intent(Intent.ACTION_MAIN);  
	    	startIntent.addCategory(Intent.CATEGORY_LAUNCHER);  
	    	startIntent.setComponent(new ComponentName(ai.packageName, ai.name));  
	    	startActivitySafely(startIntent);  
	    	return true;  
    	} else { 
    		return super.onKeyDown(keyCode, event);  
    	}  
    }
    
    void startActivitySafely(Intent intent) {  
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
        try {  
            startActivity(intent);  
        } catch (ActivityNotFoundException e) {    
        	
        } catch (SecurityException e) {  
        
        }  
    } 
}
