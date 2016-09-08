package android.systemupdate.requestparser;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.Intent;
import android.systemupdate.activitys.OtaUpdateNotifyActivity;
import android.systemupdate.util.CustomerHttpClient;
import android.util.Log;
import android.util.Xml;
import android.os.SystemProperties;

public class PandaOtaRequest implements CusOtaRequestInterface{
	public static URI mRemoteURI = null;
	public static URI mTargetURI = null;
	HttpClient mHttpClient = null;
    private static final boolean DEBUG = true;
    private Context mContext = null;
	private static final String TAG = "PandaOtaRequest";
	private static final int BUFF_SIZE = 4096;
	
	private updateInfo mUpdateInfo = null;
	
    private static void LOG(String msg) {
        if ( DEBUG ) {
            Log.d(TAG, msg);  
        }
    }
    
    public PandaOtaRequest(){
    	try {
    		String uri = getRemoteUri();
    		if (uri != null){
    			mRemoteURI = new URI(getRemoteUri());
    		} else {
    			mRemoteURI = null;
    		}
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static String getRemoteUri() {    	
    	String remoteHost = "http://" + SystemProperties.get("ro.product.ota.host") 
                              + SystemProperties.get("ro.product.ota.path") 
                              + SystemProperties.get("ro.product.ota.filename");
    	LOG(remoteHost);    	
    	return remoteHost;
    } 
    
    public static String getSystemVersion() {
    	String version = SystemProperties.get("ro.product.version");
    	LOG("getSystemVersion:" + version);
    	return version;
    }
    
	@Override
	public int requestRemoteServerForUpdate() throws IOException,
			ClientProtocolException {
		// TODO Auto-generated method stub
		if (mRemoteURI == null)
		{
			return URL_ERR;
		}
				
		int ret = SERVER_ERROR;
  
		HttpClient mHttpClient = CustomerHttpClient.getHttpClient();
		HttpGet httpGet = new HttpGet(mRemoteURI);
		HttpResponse response = mHttpClient.execute(httpGet);    
        int statusCode = response.getStatusLine().getStatusCode(); 
        LOG("statusCode:" + statusCode);
        if(statusCode == 200){    
            InputStream inputStream = response.getEntity().getContent();  
            mUpdateInfo = new updateInfo(inputStream);
            if (mUpdateInfo.getVersion() == null ){
            	ret = SERVER_ERROR;
            	LOG("updateInfo.getVersion() is null");
            } else {
            	LOG("updateInfo.getVersion" + mUpdateInfo.getVersion() + "getSystemVersion()" + getSystemVersion());
                if (!mUpdateInfo.getVersion().equals(getSystemVersion())){
                	ret = NETWORK_SERVER_OK;
                }else{
                	ret = SYSTEM_NEW;
                }
                LOG("ret:" + ret);
            }
        }    
        httpGet.abort();    
		
		return ret;
	}

	public class updateInfo{
		private String version = null;
		private String filename = null;
		private String checksum = null;
		private String url = null;
		private String info = null;
		private String mUpdateFileLength;
		
		public updateInfo(InputStream in){
			if (in == null){
				return ;
			}
			
			LOG("updateInfo CONSTRUCT:");
			
			try {
			    XmlPullParser parser = Xml.newPullParser();
			    parser.setInput(in, "UTF-8");
			    int eventType = parser.getEventType();
			    LOG("First eventType:" + eventType);
			    while (eventType != XmlPullParser.END_DOCUMENT) {
		            switch (eventType) {
				        case XmlPullParser.START_DOCUMENT:
				            break;
				        case XmlPullParser.START_TAG:
				            if (parser.getName().equals("version")) { 
				            	eventType = parser.next();
				        	    version = parser.getText();
				        	    LOG("version:" + version);
				            } else if (parser.getName().equals("filename")) {
				            	eventType = parser.next();
				        	    filename = parser.getText();
				        	    LOG("filename:" + filename);
				            } else if (parser.getName().equals("checksum")) { 
				            	eventType = parser.next();
				        	    checksum = parser.getText();
				        	    LOG("checksum:" + checksum);
				            } else if (parser.getName().equals("url")) { 
				            	eventType = parser.next();
				        	    url = parser.getText();
				        	    LOG("url:" + url);
				            } else if (parser.getName().equals("info")){
				            	eventType = parser.next();
				        	    info = parser.getText();
				        	    LOG("info:" + info);
				            }
				            break;
				         case XmlPullParser.END_TAG:
				            break;
				     }
		            // LOG("updateInfo parser:" + parser.getName() + parser.getText());
				     eventType = parser.next();
			    }
			} catch(Exception e) {
				LOG("updateInfo Exception:");
			}


		}
		
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
			LOG("version:" + version);
		}
		public String getFilename() {
			return filename;
		}
		public void setFilename(String filename) {
			LOG("filename:" + filename);
			this.filename = filename;
		}
		public String getChecksum() {
			return checksum;
		}
		public void setChecksum(String checksum) {
			LOG("checksum:" + checksum);
			this.checksum = checksum;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
			LOG("URL:" + url);
		}
		public String getInfo() {
			return info;
		}
		
		public void setInfo(String info) {
			LOG("info:" + info);
			this.info = info;
		}

		public void setLength(String length) {
			LOG("Length:" + length);
			this.mUpdateFileLength = length;
		}
		
		public String getLength() {
			return mUpdateFileLength;
		}				
	}
	
	private boolean xmlParser(InputStream inputStream) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startUpdate(Context contex) throws IOException, ClientProtocolException {
		// TODO Auto-generated method stub
		mContext = contex;
		
		if (mUpdateInfo == null)
		{
			LOG("startUpdate mUpdateInfo is null");
			throw new IOException();
		}
		
		LOG("URL:" + mUpdateInfo.getUrl());

		mUpdateInfo.setLength(getUpdateFileLength());
		
		if (mUpdateInfo.getLength() != null && !mUpdateInfo.getLength().equals("-1")){
		    Intent intent= new Intent(mContext, OtaUpdateNotifyActivity.class);
		    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    intent.putExtra("uri", mUpdateInfo.getUrl());
		    intent.putExtra("OtaPackageLength", mUpdateInfo.getLength());
		    intent.putExtra("OtaPackageName", mUpdateInfo.getFilename());		
		    intent.putExtra("OtaPackageVersion", mUpdateInfo.getVersion());
		    intent.putExtra("SystemVersion", getSystemVersion());
		
		    mContext.startActivity(intent);		
		}
		
		return ;
	}

	private String getUpdateFileLength() throws IOException, ClientProtocolException {
		LOG("getUpdateFileLength:");
		
    	try {
    		String uri = mUpdateInfo.getUrl();
    		if (uri != null){
    			mTargetURI = new URI(uri);
    		} else {
    			mRemoteURI = null;
    		}
			
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		
		HttpClient mHttpClient = CustomerHttpClient.getHttpClient();
		HttpGet httpGet = new HttpGet(mTargetURI);
		HttpResponse response = mHttpClient.execute(httpGet);  
		int statusCode = response.getStatusLine().getStatusCode();  
		String ret = null;
		
		for(Header header : response.getAllHeaders()){    
            LOG(header.getName()+":"+header.getValue());    
        } 
		
	    if(statusCode != 200) {
	    	ret = null;
	    } else {
	    	Header[] headLength  = response.getHeaders("content-length");
		    if(headLength.length > 0) {
		    	ret = headLength[0].getValue();
		    } else {
		    	ret = null;
		    }
	    }
	    
		httpGet.abort();
		
		
		/*
		URL url = new URL(mUpdateInfo.getUrl());
		HttpURLConnection urlcon=(HttpURLConnection)url.openConnection(); 
		String ret = String.valueOf(urlcon.getContentLength());
		//urlcon.disconnect(); 
		LOG("urlcon.getContentLength():" + urlcon.getContentLength());
		LOG("getUpdateFileLength:" + ret);
		*/
		
		return ret;
		
	}
	
	@Override
	public  void  startUpdate(Context contex, boolean b) throws IOException, ClientProtocolException {
	}

}
