package android.systemupdate.requestparser;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;

import android.content.Context;
import android.content.Intent;
import android.os.StatFs;
import android.systemupdate.activitys.OtaUpdateNotifyActivity;
import android.systemupdate.activitys.PackageDownloadActivity;
import android.systemupdate.activitys.StorageMemeryIsNotEnoughActivity;
import android.systemupdate.service.SystemUpdateService;
import android.systemupdate.util.CustomerHttpClient;
import android.util.Log;
import android.os.SystemProperties;

public class KtcOtaRequest implements CusOtaRequestInterface{
    // mRemoteURI is local request url,and mTargetURI is the update package file's url
    public static URI mRemoteURI = null;
    private String mTargetURI = null;
    private String mOtaPackageVersion = null;
    private String mSystemVersion = null;
    private String mOtaPackageName = null;
    private String mOtaPackageLength = null;
    private String mOtaUpdateLevel = null;
    private static final boolean DEBUG = true;
    private Context mContext = null;
	private static final String TAG = "KtcRequestParser";
	
    private static void LOG(String msg) {
        if ( DEBUG ) {
            Log.d(TAG, msg);  
        }
    }
    
    
    public KtcOtaRequest(){
    	try {
			mRemoteURI = new URI(getRemoteUri());
			Log.v(TAG, "mRemoteURI=" + mRemoteURI);
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static String getRemoteUri() {
    	return "http://" + getRemoteHost() + "/otaservlet?product=" + getOtaProductName() 
    			+ "&serial=" + getSerialName() + "&version=" + getSystemVersion();
    } 
    
    public static String getRemoteHost() {
    	String remoteHost = SystemProperties.get("update.ktc.cn:3300");			//("ro.product.ota.host");
    	if(remoteHost == null || remoteHost.length() == 0) {
    		remoteHost = "update.ktc.cn:3300";	//"172.16.9.24:3300";
    	}
    	return remoteHost;
    }
    
    public static String getBakRemoteUri() {
    	String remoteHost = SystemProperties.get("ro.product.ota.host2");
    	if(remoteHost == null || remoteHost.length() == 0) {
    		remoteHost = "update.ktc.cn:3300";	//"172.16.9.24:3300";
    	}

    	return "http://" + remoteHost + "/otaservlet?product=" + getOtaProductName() 
    			+ "&serial=" + getSerialName() + "&version=" + getSystemVersion();
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
    	if(serial == null || serial.length() == 0) {
    		serial = "MSTAR20160908";
    	}
    	return serial;
    }   
    
    public static String getSystemVersion() {
    	String version = SystemProperties.get("ro.product.version");
    	if(version == null || version.length() == 0) {
    		version = "V1.0.0";
    	}
    	
    	return version;
    }
	
	
	public  int requestRemoteServerForUpdate() throws IOException, ClientProtocolException {
/*		try {
			mRemoteURI = new URI("http://update.ktc.cn:3300/otaservlet?product=TV628_T2&serial=SDA1231231231230000&version=V1.0.1");
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
    	HttpClient httpClient = CustomerHttpClient.getHttpClient();
    	HttpHead httpHead = new HttpHead(mRemoteURI); 
    	Log.v(TAG, "mRemoteURI=" + mRemoteURI); 
    	HttpResponse response = null;
    	int statusCode;
    	try{
		    response = httpClient.execute(httpHead);  
		    Log.v(TAG, "httpHead=" + httpHead);     
		    statusCode = response.getStatusLine().getStatusCode();    
		    Log.v(TAG, "statuscode=" + statusCode);
    	}catch(Exception e){
    		statusCode=404;
    	}
	    
	    if(statusCode != 200) {
	    	try {
				mRemoteURI = new URI(getBakRemoteUri());
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	httpHead = new HttpHead(mRemoteURI);
	    	response = httpClient.execute(httpHead); 
	    	statusCode = response.getStatusLine().getStatusCode(); 
	    	Log.v(TAG, "statuscode=" + statusCode);
	    	if(statusCode != 200){
	    		return SERVER_ERROR; 
	    	}
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
	    	Log.v(TAG, "download url=" + mTargetURI);
	    }
	    Header[] headOtaUpdateLevel = response.getHeaders("OtaUpdateLevel");
	    if(headTargetURI.length > 0) {
	    	mOtaUpdateLevel = headTargetURI[0].getValue();
	    }	    
	    //mTargetURI = "http://" + getRemoteHost() + (mTargetURI.startsWith("/") ? mTargetURI : ("/" + mTargetURI));
	    mSystemVersion = getSystemVersion();
	    LOG("OtaPackageName = " + mOtaPackageName + " OtaPackageVersion = " + mOtaPackageVersion 
	    			+ " OtaPackageLength = " + mOtaPackageLength + " SystemVersion = " + mSystemVersion
	    			+ "OtaPackageUri = " + mTargetURI);
	    return NETWORK_SERVER_OK;
	}
	
	@Override
	public  void  startUpdate(Context contex) throws IOException, ClientProtocolException {	
	}
	
	@Override
	public  void  startUpdate(Context contex, boolean b) throws IOException, ClientProtocolException {	
		mContext = contex;
		if(b){
			Intent intent= new Intent(mContext, OtaUpdateNotifyActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.putExtra("uri", mTargetURI);
			intent.putExtra("OtaPackageLength", mOtaPackageLength);
			intent.putExtra("OtaPackageName", mOtaPackageName);
			intent.putExtra("OtaPackageVersion", mOtaPackageVersion);
			intent.putExtra("SystemVersion", mSystemVersion);
			mContext.startActivity(intent);
		}else{
			//zjd20150605, start
			String packageSize_string = null;  //ota size
			if (mOtaPackageLength != null){
	            long packageSize = Long.valueOf(mOtaPackageLength);            
	            if(packageSize < 1024) {
	        	    packageSize_string = String.valueOf(packageSize) + "B";
	            }else if(packageSize/1024 > 0 && packageSize/1024/1024 == 0) {
	        	    packageSize_string = String.valueOf(packageSize/1024) + "K";
	            }else if(packageSize/1024/1024 > 0) {
	        	    packageSize_string = String.valueOf(packageSize/1024/1024) + "M";
	            }
	        } else {
	        	packageSize_string = null;
	        }
			
			
			if (SystemUpdateService.getTmpDirFreeSize() <= Integer.parseInt(mOtaPackageLength)){
				Intent intent = new Intent(mContext, StorageMemeryIsNotEnoughActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            intent.putExtra("pack_length", Long.parseLong(mOtaPackageLength));
	            intent.putExtra("storage_leave", SystemUpdateService.getTmpDirFreeSize());
	            mContext.startActivity(intent);
	        }else{
				Intent intent = new Intent(mContext, PackageDownloadActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra("uri", mTargetURI);
				intent.putExtra("OtaPackageLength", mOtaPackageLength);
				intent.putExtra("OtaPackageName", mOtaPackageName);
				intent.putExtra("OtaPackageVersion", mOtaPackageVersion);
				intent.putExtra("SystemVersion", mSystemVersion);
				intent.putExtra("PackageSize", packageSize_string);
				mContext.startActivity(intent);
	        }
			//zjd20150605, end
		}
	}
}
