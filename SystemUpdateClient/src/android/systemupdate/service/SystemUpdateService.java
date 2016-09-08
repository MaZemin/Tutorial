package android.systemupdate.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.systemupdate.activitys.*;
import android.systemupdate.requestparser.CusOtaRequestInterface;
import android.systemupdate.requestparser.KtcOtaRequest;
import android.systemupdate.requestparser.PandaOtaRequest;
import android.systemupdate.util.CustomerHttpClient;
import android.systemupdate.util.RecoverySystem;
import android.util.Log;
import android.os.SystemProperties;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import android.os.PowerManager;
import android.os.StatFs;
import android.widget.Toast;

public class SystemUpdateService extends Service {
	public static final String VERSION = "1.3.0";
	private static final String TAG = "SystemUpdateService";
    private static final boolean DEBUG = true;
    private Context mContext;
    private boolean mIsFirstStartUp = true;
    private boolean isByHand = false;
    private boolean isByBoot = false;
    private static void LOG(String msg) {
        if ( DEBUG ) {
            Log.d(TAG, msg);  
        }
    }
    
    static {
        /*
         * Load the library.  If it's already loaded, this does nothing.
         */
        System.loadLibrary("system_update_jni");
    }
    
    public static final String OTA_PACKAGE_FILE = "update.zip";
	public static final String RKIMAGE_FILE = "update.img";	
	public static final String UPDATE_HELPER_FILE_NAME = "update_helper.tmp";
	public static final int RKUPDATE_MODE = 1;
	public static final int OTAUPDATE_MODE = 2;
	private static volatile boolean mWorkHandleLocked = false; 
	
	public static final String EXTRA_IMAGE_PATH = "android.systemupdate.extra.IMAGE_PATH";
    public static final String EXTRA_IMAGE_VERSION = "android.systemupdate.extra.IMAGE_VERSION";
    public static final String EXTRA_CURRENT_VERSION = "android.systemupdate.extra.CURRENT_VERSION";


    public static final String EXTRA_ERR_MESSAGE = "android.systemupdate.extra.ERR_MESSAGE"; 
    /** ���� "��Ӧ��" �� ����¼� �жϵĲ���. */
    private static PowerManager.WakeLock sWakeLock;					
    private static final String WAKELOCK_TAG = "SystemUpdateService";		

   // public static final String FLASH_ROOT = "/data/tmp";20160217
     public static final String FLASH_ROOT = "/cache";
    public static final String SDCARD_ROOT = "/mnt/external_sd";
    public static final String CACHE_ROOT = Environment.getDownloadCacheDirectory().getAbsolutePath();

    /** remote image �ڱ��صĴ洢 ·���ִ�. */
    private static final String UPDATE_IMG_FILE = FLASH_ROOT + "/" + RKIMAGE_FILE;		
    private static final String UPDATE_OTA_FILE = FLASH_ROOT + "/" + OTA_PACKAGE_FILE;	

    private final int BUFFERE_SIZE = 1024*8;  
	
    public static final int COMMAND_NULL = 0;
    public static final int COMMAND_CHECK_LOCAL_INER_UPDATING = 1;
    public static final int COMMAND_CHECK_LOCAL_EXT_UPDATING = 2;
    public static final int COMMAND_CHECK_REMOTE_UPDATING = 3;
    public static final int COMMAND_CHECK_REMOTE_UPDATING_BY_HAND = 4;
    public static final int COMMAND_CHECK_REMOTE_UPDATING_BY_BOOT = 5;  //add zjd20150513
    
    public static final String UPDATE_FLAG_SUCCESS = "success";
    public static final String UPDATE_FLAG_FAILD = "faild";
    
    private static final String COMMAND_FLAG_SUCCESS = "1";
    
    public static final int UPDATE_SUCCESS = 1;
    public static final int UPDATE_FAILED = 2;
    
    private static final String[] IMAGE_FILE_DIRS = {
        //"/mnt/sdcard/", 
        FLASH_ROOT + "/",
        //"/mnt/external_sdcard/", 
        SDCARD_ROOT + "/",
        // "/cache/",
    };
    
    private WorkHandler mWorkHandler;
    private SharedPreferences mAutoCheckSet;
    //zjd20150731
    private SharedPreferences mOtaTip;
    //zjd20150731
   
    /*----------------------------------------------------------------------------------------------------*/
    // mRemoteURI is local request url,and mTargetURI is the update package file's url
    public static URI mRemoteURI = null;
    private String mTargetURI = null;
    private String mOtaPackageVersion = null;
    private String mSystemVersion = null;
    private String mOtaPackageName = null;
    private String mOtaPackageLength = null;
    private String mOtaUpdateLevel = null;
    
    CusOtaRequestInterface mCusOtaRequest = null;
    
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}
	
	private final LocalBinder mBinder = new LocalBinder();
	
	public class LocalBinder extends Binder {
		public void updateFirmware(String imagePath, int mode) {
			LOG("updateFirmware(): imagePath = " + imagePath);
	        try {       
				mWorkHandleLocked = true;
				if(mode == OTAUPDATE_MODE){
					RecoverySystem.installPackage(mContext, new File(imagePath));
				}else if(mode == RKUPDATE_MODE){
					RecoverySystem.installRKimage(mContext, imagePath);
				}
	        } catch (IOException e) {
	            Log.e(TAG, "updateFirmware() : Reboot for updateFirmware() failed", e);
	        }
	    }
		
		public boolean doesOtaPackageMatchProduct(String imagePath) {
	      	LOG("doesImageMatchProduct(): start verify package , imagePath = " + imagePath);
			
			try{
				RecoverySystem.verifyPackage(new File(imagePath), null, null);
			}catch(GeneralSecurityException e){
				LOG("doesImageMatchProduct(): verifaPackage faild!");	
				return false;	
			}catch(IOException exc) {
	            LOG("doesImageMatchProduct(): verifaPackage faild!");
				return false;
	        }
	        return true;
	    }
		
		public void deletePackage(String path) {
			LOG("try to deletePackage...");
			File f = new File(path);
			if(f.exists()) {
				f.delete();
				LOG("delete complete! path=" + path);
			}else {
				LOG("path=" + path + " ,file not exists!");
			}
		}
		
		public void unLockWorkHandler() {
			LOG("unLockWorkHandler...");
			mWorkHandleLocked = false;
		}
		
		public void LockWorkHandler() {
			mWorkHandleLocked = true;
			LOG("LockWorkHandler...!");
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
        /*-----------------------------------*/
        Log.d(TAG, "starting SystemUpdateService, version is " + VERSION);
        try {
        	mRemoteURI = new URI(getRemoteUri());
        	LOG("remote uri is " + mRemoteURI.toString());
        }catch(URISyntaxException e) {
        	e.printStackTrace();
        }        
        
        String productName = SystemProperties.get("ro.product.name");
        LOG("productName:" + productName);
        if ( productName != null ){
        	if (productName.equals("panda_emmc") || productName.equals("panda_mlc")){
        		LOG("mCusOtaRequest = new PandaOtaRequest()");
        		mCusOtaRequest = new PandaOtaRequest();
        	} else if (productName.equals("full_ktc_4k2k") || productName.equals("full_ktc")){
        		LOG("mCusOtaRequest = new KtcOtaRequest()");
        		mCusOtaRequest = new KtcOtaRequest();
        	} else {
        		LOG("mCusOtaRequest = new KtcOtaRequest()");
        		mCusOtaRequest = new KtcOtaRequest();
        	}
        } else {
        	LOG("mCusOtaRequest = new KtcOtaRequest()");
        	mCusOtaRequest = new KtcOtaRequest();
        }
        
        mAutoCheckSet = getSharedPreferences("auto_check", MODE_PRIVATE);
        
        mOtaTip = getSharedPreferences("ota_tip", MODE_PRIVATE);//zjd20150731

        PowerManager powerManager = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        sWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);
		
        HandlerThread workThread = new HandlerThread("UpdateService : work thread");
        workThread.start();
        mWorkHandler = new WorkHandler(workThread.getLooper());
        
        if(mIsFirstStartUp) {
			Log.d(TAG, "first startup!!!");
			mIsFirstStartUp = false;
			String command = RecoverySystem.readFlagCommand();	
			LOG("command = " + command);	
			if(command != null) {						
				String path;
				String flag;
				int nIndex = command.indexOf('\n');
				path = command.substring(0,nIndex);				
				flag = command.substring(nIndex + 1, nIndex + 2);				
				
				if ((path != null) && (flag != null))
					LOG("last_install: path = " + path);
					LOG("last_install: flag = " + flag);
					LOG("last_install: flag size = " + flag.length());
					LOG("last_install: COMMAND_FLAG_SUCCESS size = " + COMMAND_FLAG_SUCCESS.length());
					
					File file = new File(path);
					if (!file.exists())
					{
						LOG("last_install file is not exist!!!");
						return ;
					}
				
					LOG("flag.equals = " + flag.equals(COMMAND_FLAG_SUCCESS));
					if(flag.equals(COMMAND_FLAG_SUCCESS)) {
					//	Intent intent = new Intent(mContext, NotifyDeleteActivity.class);
						Intent intent = new Intent(mContext, NotifyUpdateResultAcitivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra("flag", UPDATE_FLAG_SUCCESS);
						intent.putExtra("path", path);
						startActivity(intent);

						mWorkHandleLocked = true;    //需要删除内部升级用过的固件包,不会检测U盘和网络
					} else {
					//	Intent intent = new Intent(mContext, NotifyDeleteActivity.class);
						Intent intent = new Intent(mContext, NotifyUpdateResultAcitivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra("flag", UPDATE_FLAG_FAILD);
						intent.putExtra("path", path);
						startActivity(intent);
						mWorkHandleLocked = true;  //需要删除内部升级用过的固件包,不会检测U盘和网络
					}
					
					return ;
				}
			}
		
	}

	@Override
	public void onDestroy() {
		LOG("onDestroy.......");
		super.onDestroy();
	        //add by xubilv : begin
		if(mWorkHandleLocked){
                    mWorkHandleLocked = false;
		}
		 //add by xubilv : end
	}

	@Override
	public void onStart(Intent intent, int startId) {
		LOG("onStart.......");
		super.onStart(intent, startId);
	}


        private void doSetAutoCheck(Intent intent){
                String autoCheck = intent.getStringExtra("auto_check");
                if(null == autoCheck){
			return ;
		}

		SharedPreferences autoCheckSet = getSharedPreferences("auto_check", MODE_PRIVATE);
		Editor e = mAutoCheckSet.edit();

                if(autoCheck.equals("true")){
			e.putBoolean("auto_check", true);
			e.commit();
                }
                
                if(autoCheck.equals("false")){
                        e.putBoolean("auto_check", false);
                        e.commit();
                }
    
                return ;
        }

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LOG("onStartCommand.......");
		if(intent == null) {
			return Service.START_NOT_STICKY;
		}	

		int command = intent.getIntExtra("command", COMMAND_NULL);
		int delayTime = intent.getIntExtra("delay", 1000);
		String path = intent.getStringExtra("path");		//add by xubilv
		LOG("command = " + command + " --delaytime = " + delayTime + " --path = " + path);

		if(command == COMMAND_NULL) {
                        doSetAutoCheck(intent);
			return Service.START_NOT_STICKY;
		}
		
		if(command == COMMAND_CHECK_REMOTE_UPDATING) {
			if(!mAutoCheckSet.getBoolean("auto_check", false)) {
				LOG("user set not auto check!");
				return Service.START_NOT_STICKY;
			}
		}
		
		if(command == COMMAND_CHECK_REMOTE_UPDATING_BY_HAND) {
			isByHand = true;
			command = COMMAND_CHECK_REMOTE_UPDATING;
		}
		
		if(command == COMMAND_CHECK_REMOTE_UPDATING_BY_BOOT) {
			isByBoot = true;
			command = COMMAND_CHECK_REMOTE_UPDATING;
		}

		Message msg = new Message();
		msg.what = command;
		msg.obj   = path;		//add by xubilv
		msg.arg1 = WorkHandler.NOT_NOTIFY_IF_NO_IMG;
		mWorkHandler.sendMessageDelayed(msg, delayTime);

		return Service.START_STICKY;
	}
   
    
    /** @see mWorkHandler. */
    private class WorkHandler extends Handler {
        private static final int NOTIFY_IF_NO_IMG = 1;
        private static final int NOT_NOTIFY_IF_NO_IMG = 0;
        
        /*-----------------------------------*/
        
        public WorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {       

            String searchResult = null;       
            switch (msg.what) {

                case COMMAND_CHECK_LOCAL_INER_UPDATING:
                    LOG("WorkHandler::handleMessage() : To perform 'COMMAND_CHECK_LOCAL_INER_UPDATING'.");
                    if(mWorkHandleLocked){
                    	LOG("WorkHandler::handleMessage() : locked !!!");
                    	return;
                    }
			               
                    searchResult = getInerValidFirmwareImageFile();
                    if ( searchResult != null) {	                    
                            String path = searchResult;      
                            String imageFileVersion = null;
                            String currentVersion = null;

                            //if it is rkimage, check the image
                            if(path.endsWith("img")){
                            	if(!checkRKimage(path)){
                            		LOG("WorkHandler::handleMessage() : not a valid rkimage !!");
                            		return;	
                            	}

                            	imageFileVersion = getImageVersion(path);

                            	LOG("WorkHandler::handleMessage() : Find a VALID image file : '" + path 
                            			+ "'. imageFileVersion is '" + imageFileVersion);
                             
                            	currentVersion = getCurrentFirmwareVersion();
                            	LOG("WorkHandler::handleMessage() : Current system firmware version : '" + currentVersion + "'.");
							}
                            startLocalInerImageNotify(path, imageFileVersion, currentVersion);
                            return;
                  
                    }/*else {
                        boolean notifyIfNoImg = (NOTIFY_IF_NO_IMG == msg.arg1) ? true : false;
                        LOG("WorkHandler::handleMessage() : Can not find image file; " + ( (!notifyIfNoImg) ? "Abort..." : "Show 'NoUpdateImageActivity' to user.") );
                        if ( notifyIfNoImg ) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("android.systemupdate.service", "android.systemupdate.activitys.NoUpdateImageActivity") );
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(intent);
                        }
                    }*/

                    break; 
                    
                case COMMAND_CHECK_LOCAL_EXT_UPDATING:
                    LOG("WorkHandler::handleMessage() : To perform 'COMMAND_CHECK_LOCAL_EXT_UPDATING'.");
                    if(mWorkHandleLocked){
                    	LOG("WorkHandler::handleMessage() : locked !!!");
                    	return;
                    }
			        
                    searchResult = getExtValidFirmwareImageFile((String)(msg.obj));
                    if ( searchResult != null) {	                    
                            String srcPath = searchResult;     
                            String desPath = null;                           

                            //if it is rkimage, check the image
                            if(srcPath.endsWith("img")){
                            	if(!checkRKimage(srcPath)){
                            		LOG("WorkHandler::handleMessage() : not a valid rkimage !!");
                            		return;	
                            	}
                            }
                            
                            File file = new File(srcPath);
                            if(file.getName().equals(OTA_PACKAGE_FILE))
                            {
                            	desPath = UPDATE_OTA_FILE;
                            }
                            else if(file.getName().equals(RKIMAGE_FILE))
                            {
                            	desPath = UPDATE_IMG_FILE;
                            }
                            
                            startLocalExtImageNotify(srcPath, desPath);
                            return;
                  
                    }/*else {
                        boolean notifyIfNoImg = (NOTIFY_IF_NO_IMG == msg.arg1) ? true : false;
                        LOG("WorkHandler::handleMessage() : Can not find image file; " + ( (!notifyIfNoImg) ? "Abort..." : "Show 'NoUpdateImageActivity' to user.") );
                        if ( notifyIfNoImg ) {
                            Intent intent = new Intent();
                            intent.setComponent(new ComponentName("android.systemupdate.service", "android.systemupdate.activitys.NoUpdateImageActivity") );
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(intent);
                        }
                    }*/

                    break;                     
                case COMMAND_CHECK_REMOTE_UPDATING:
                	if(mWorkHandleLocked){
						LOG("WorkHandler::handleMessage() : locked !!!");
						return;
					}
                	
                	
                	for(int i = 0; i < CusOtaRequestInterface.REQUEST_NUM; i++) {

	                	try {	
                			/*if(requestRemoteServerForUpdate()) {
                    				LOG("find a remote update package, now start PackageDownloadActivity...");
	                    			Intent intent = new Intent(mContext, OtaUpdateNotifyActivity.class);
        	            			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                	    			intent.putExtra("uri", mTargetURI);
                    				intent.putExtra("OtaPackageLength", mOtaPackageLength);
                    				intent.putExtra("OtaPackageName", mOtaPackageName);
                    				intent.putExtra("OtaPackageVersion", mOtaPackageVersion);
	                    			intent.putExtra("SystemVersion", mSystemVersion);
        	            			mContext.startActivity(intent);
                	    	}else {
                    			LOG("no find remote update package...");
						        if (isByHand){
						            Toast.makeText(getApplicationContext(), getString(R.string.no_package), Toast.LENGTH_SHORT).show();		
						        }
        	            	} */
	                		
	                		LOG("requestRemoteServerForUpdate:" + i);
	                		
	                		int ret = mCusOtaRequest.requestRemoteServerForUpdate();
	                		switch(ret){
	                			case CusOtaRequestInterface.NETWORK_SERVER_OK:
	                				if(isByBoot){
	                					SystemProperties.set("persist.sys.ota.available", "true");
	                					int count = mOtaTip.getInt("count", 0);
	                					if(count < 3){
								    	Log.v(TAG, "[shawn]----mCusOtaRequest.startUpdate true");				
		                					mCusOtaRequest.startUpdate(mContext, true);
		                					Editor e = mOtaTip.edit();
		                					count++;
		                					e.putInt("count", count);
		                					e.commit();
	                					}
	                				}else{
								    	Log.v(TAG, "[shawn]----mCusOtaRequest.startUpdate failure");		// shawn 201617 check in here			                				
	                					mCusOtaRequest.startUpdate(mContext, true);			// shawn 20160617 change to true for test 
	                				}
	                				break;
	                			case CusOtaRequestInterface.SYSTEM_NEW:
	                				LOG("system is newer...");
	                				if (isByHand){
	                					Toast.makeText(getApplicationContext(), getString(R.string.system_newer), Toast.LENGTH_SHORT).show();		
	                				}
	                				if(isByBoot){
	                					SystemProperties.set("persist.sys.ota.available", "false");
	                				}
	                				break;
	                			case CusOtaRequestInterface.SERVER_ERROR:
	                				LOG("no find remote update package...");
	                				if (isByHand){
	                					Toast.makeText(getApplicationContext(), getString(R.string.no_package), Toast.LENGTH_SHORT).show();		
	                				}
	                				if(isByBoot){
	                					SystemProperties.set("persist.sys.ota.available", "false");
	                				}
	                				break;
	                			default:
	                				LOG("no statas defined...");
	                				break;
	                		}

                			break;
	                	}catch(IOException e) {
	                		e.printStackTrace();
	                		LOG("request remote server error..." + i);
	                		if (i == CusOtaRequestInterface.REQUEST_NUM - 1)
	                		{
	                			new Thread() { 
	                				@Override
	                				public void run() { 
	                					Looper.prepare(); 
	                					if(isByHand){
	                						Toast.makeText(getApplicationContext(), getString(R.string.no_package), Toast.LENGTH_SHORT).show();
	                					}
	                					Looper.loop(); 
	                				} 
	                			}.start(); 
	                		}
	                	}

	                	try{
	                		Thread.sleep(500);
	                	}catch(InterruptedException e) {
	                		e.printStackTrace();
	                	}
                	}
			
			isByHand = false;
			isByBoot = false;
                	break;
                default:
                    break; 
            }
        }

    }  


    private static void acquireWakeLock() {
        sWakeLock.acquire();
    }
    
    private static void releaseWakeLock() {
        sWakeLock.release();
    }


     /*�����ض���activity��֪ͨ�û��������ļ�����*/
    private void notifyErrorMessage(String msgString){
        Intent intent = new Intent();
	LOG("notifyErrorMessage() : "+msgString);
        /* Ԥ��Ŀ�� Activity. */
        intent.setComponent(new ComponentName("android.systemupdate.service", "android.systemupdate.activitys.CopyUpdateImageErrorActivity") );
        /* flags. */
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        /* Ԥ�� extra ���. */
        intent.putExtra(EXTRA_ERR_MESSAGE, msgString);

        /* ��������Ŀ�� activity. */
        mContext.startActivity(intent);
    }
   
    private boolean checkIsExternStorage(String path){
	StorageManager mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
    	StorageVolume[] mVolemes = mStorageManager.getVolumeList();
        for (int i = 0; i < mVolemes.length; i++) {
        	// 排除内部存储和外部SD卡
        	if(mVolemes[i].getPath().equals(FLASH_ROOT + "/") || mVolemes[i].getPath().equals(SDCARD_ROOT + "/")){
        		continue ;
        	}
        	
        	if(mVolemes[i].getPath().equals(path)){
        		return true;
        	}        	            
        }
        
    	return false;
    }
    private String getInerValidFirmwareImageFile() {
    	{ // 查找FLASH_ROOT是否含有升级文件
    		String dirPath = FLASH_ROOT + "/";
    		String otafilePath = dirPath + OTA_PACKAGE_FILE;    
    		LOG("getValidFirmwareImageFile() : Target image file path : " + otafilePath); 
    		if ((new File(otafilePath)).exists()) {
    			return otafilePath;
    		}

    		String imgfilePath = dirPath + RKIMAGE_FILE;
    		LOG("getValidFirmwareImageFile() : Target image file path : " + imgfilePath);   
    		if ( (new File(imgfilePath) ).exists() ) {
    			return imgfilePath;
    		}
    	}
    	
    	{ // 查找SDCARD_ROOT是否含有升级文件
    		String dirPath = SDCARD_ROOT + "/";
    		String otafilePath = dirPath + OTA_PACKAGE_FILE;    
    		LOG("getValidFirmwareImageFile() : Target image file path : " + otafilePath); 
    		if ((new File(otafilePath)).exists()) {
    			return otafilePath;
    		}

    		String imgfilePath = dirPath + RKIMAGE_FILE;
    		LOG("getValidFirmwareImageFile() : Target image file path : " + imgfilePath);   
    		if ( (new File(imgfilePath) ).exists() ) {
    			return imgfilePath;
    		}        
    	}              
    	
        LOG(" getValidFirmwareImageFile() ; return null");
        return null;    	
    }
    
    private String getExtValidFirmwareImageFile(String searchPaths) {
    	if ( null == searchPaths){
    		return null;
    	}
    	
    	String filePath = null;
 	   if (checkIsExternStorage(searchPaths))
 	   {
           File pfile = new File(searchPaths);
           File[] pfiles = pfile.listFiles();
 	   
   	       for(int i = 0; i < pfiles.length; i ++)
 	       {	
                    if(pfiles[i].isFile())
                    {
                        if(pfiles[i].getName().equals(OTA_PACKAGE_FILE))
                        {
                        	filePath = pfiles[i].getPath();
                        	break;
                        }
                        else if(pfiles[i].getName().equals(RKIMAGE_FILE))
                        {
                        	filePath = pfiles[i].getPath();
                        	break;
                        }
                    }
 	       }    	
 	   }
 	   
 	   return filePath;
   }
       
    native private static String getImageVersion(String path);

    native private static String getImageProductName(String path);

    private void startLocalInerImageNotify(String path, String imageVersion, String currentVersion) {
        Intent intent = new Intent();

        intent.setComponent(new ComponentName("android.systemupdate.service", "android.systemupdate.activitys.SystemUpdatingActivity") );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_IMAGE_PATH, path);
        intent.putExtra(EXTRA_IMAGE_VERSION, imageVersion);
        intent.putExtra(EXTRA_CURRENT_VERSION, currentVersion);

        mContext.startActivity(intent);
    }
    
    
    private void startLocalExtImageNotify(String srcPath, String desPath) {
        Intent intent = new Intent();

        intent.setComponent(new ComponentName("android.systemupdate.service", "android.systemupdate.activitys.LocalUpdateNotifyActivity") );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("source_path", srcPath);
        intent.putExtra("dest_path", desPath);

        mContext.startActivity(intent);
    }    
    
	private boolean checkRKimage(String path){
		String imageProductName = getImageProductName(path);
		LOG("checkRKimage() : imageProductName = " + imageProductName);
		if(imageProductName.equals(getProductName())){
			return true;
		}else{
			return false;
		}
	} 

    private String getCurrentFirmwareVersion() {
        if ( true ) { 
            return SystemProperties.get("ro.firmware.version");
        }
        else {
            return "1.0.0";
        }
    }
    
    private static String getProductName() { 
    	return SystemProperties.get("ro.product.model");        
    }
    
    private boolean isUpdateDoneAlready(String path) {
    	LOG("isUpdateDoneAlready(): path = " + path);
    	String composePath = composeFilePath(path);
    	File f = new File(composePath);
    	File updateFile = new File(path);
    	LOG("isUpdateDoneAlready() : composePath = " + composePath);
    	if(f.exists()) {
    		//so we delete the package, Prevent repeat upgrade
    		f.delete();
    		updateFile.delete();
    		LOG("delete updatefile !");
    		return true;
    	}else {
    		return false;
    	}
    }
    

	//compose the updat_helper_file path
	private static String composeFilePath(String imagePath) {
        
        LOG("composeFilePath() : 'imagePath' = " + imagePath);
        
        if ( imagePath.equals(FLASH_ROOT + "/" + OTA_PACKAGE_FILE ) || imagePath.equals(FLASH_ROOT + "/" + RKIMAGE_FILE) ) {
            return new String(FLASH_ROOT + "/" + UPDATE_HELPER_FILE_NAME);
        }
        else if ( imagePath.equals(SDCARD_ROOT + "/" + OTA_PACKAGE_FILE ) || imagePath.equals(SDCARD_ROOT + "/" + RKIMAGE_FILE)) {
            return new String(SDCARD_ROOT + "/" + UPDATE_HELPER_FILE_NAME);
        }
        else if ( imagePath.equals(CACHE_ROOT + "/" + OTA_PACKAGE_FILE ) || imagePath.equals(CACHE_ROOT + "/" + RKIMAGE_FILE)) {
            return new String(CACHE_ROOT + "/" + UPDATE_HELPER_FILE_NAME);
        }
        else {
            LOG("composeFilePath() : Not valid image file path.");
            return "";
        }
    }
    
    private void notifyInvalidImage(String path) {
        Intent intent = new Intent();

        intent.setComponent(new ComponentName("android.systemupdate.service", "android.systemupdate.activitys.InvalidUpdateImageActivity") );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_IMAGE_PATH, path); 

        mContext.startActivity(intent);
    }
    
    /**********************************************************************************************************************
    											ota update
    ***********************************************************************************************************************/
    public static String getRemoteUri() {
    	return "http://" + getRemoteHost() + "/otaservlet?product=" + getOtaProductName() 
    			+ "&serial=" + getSerialName() + "&version=" + getSystemVersion();
    } 
    
    public static String getRemoteHost() {
    	String remoteHost = SystemProperties.get("ro.product.ota.host");
    	if(remoteHost == null || remoteHost.length() == 0) {
    		remoteHost = "172.16.9.24:2300";
    	}
    	return remoteHost;
    }
    
    public static String getOtaProductName() {
    	String productName = SystemProperties.get("ro.product.model");
    	if(productName.contains(" ")) {
    		productName = productName.replaceAll(" ", "");
    	}
    	
    	return productName;
    }
    
    
    public static String getSerialName() {
    	String serial = SystemProperties.get("ro.product.serial");
    	if(serial.contains(" ")) {
    		serial = serial.replaceAll(" ", "");
    	}
    	
    	return serial;
    }    
    
    private boolean requestRemoteServerForUpdate() throws IOException, ClientProtocolException{
    	HttpClient httpClient = CustomerHttpClient.getHttpClient();
    	HttpHead httpHead = new HttpHead(mRemoteURI); 
    	
	    HttpResponse response = httpClient.execute(httpHead);       
	    int statusCode = response.getStatusLine().getStatusCode();    
	    
	    if(statusCode != 200) {
	    	return false;    
	    }
	    if(DEBUG){    
	        for(Header header : response.getAllHeaders()){    
	            LOG(header.getName()+":"+header.getValue());    
	        }    
	    }
	    
	    Header[] headLength = response.getHeaders("OtaPackageLength");
	    if(headLength.length > 0) {
	    	mOtaPackageLength = headLength[0].getValue();
	    }
	    
	    Header[] headName = response.getHeaders("OtaPackageName");
	    if(headName.length > 0) {
	    	mOtaPackageName = headName[0].getValue();
	    }
	    
	    Header[] headVersion = response.getHeaders("OtaPackageVersion");
	    if(headVersion.length > 0) {
	    	mOtaPackageVersion = headVersion[0].getValue();
	    }
	    
	    Header[] headTargetURI = response.getHeaders("OtaPackageUri");
	    if(headTargetURI.length > 0) {
	    	mTargetURI = headTargetURI[0].getValue();
	    }
	    
	    Header[] headOtaUpdateLevel = response.getHeaders("OtaUpdateLevel");
	    if(headTargetURI.length > 0) {
	    	mOtaUpdateLevel = headTargetURI[0].getValue();
	    }	    
	    
	    mTargetURI = "http://" + getRemoteHost() + (mTargetURI.startsWith("/") ? mTargetURI : ("/" + mTargetURI));
	    
	    mSystemVersion = getSystemVersion();
	    
	    LOG("OtaPackageName = " + mOtaPackageName + " OtaPackageVersion = " + mOtaPackageVersion 
	    			+ " OtaPackageLength = " + mOtaPackageLength + " SystemVersion = " + mSystemVersion
	    			+ "OtaPackageUri = " + mTargetURI);
	    return true;
    }
    
    public static String getSystemVersion() {
    	String version = SystemProperties.get("ro.product.version");
    	if(version == null || version.length() == 0) {
    		version = "1.0.0";
    	}
    	
    	return version;
    }
	
    public static long getTmpDirFreeSize() {
		
	StatFs sf = new StatFs("/data");
		
	long blockSize = sf.getBlockSize();
	long availCount = sf.getAvailableBlocks();

	return availCount*blockSize;
    }    

    public static long getFileSize(String file_name) {

	File f = new File(file_name);

        return f.length();
    }

}
