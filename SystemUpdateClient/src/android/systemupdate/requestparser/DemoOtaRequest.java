package android.systemupdate.requestparser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;

import android.content.Context;
import android.content.Intent;
import android.systemupdate.activitys.OtaUpdateNotifyActivity;
import android.util.Log;

public class DemoOtaRequest implements CusOtaRequestInterface{
	public static URI mRemoteURI = null;
	private String mTargetURI = null;
	HttpClient mHttpClient = null;
    private static final boolean DEBUG = true;
    private Context mContext = null;
	private static final String TAG = "DemoOtaRequest";
	
    private static void LOG(String msg) {
        if ( DEBUG ) {
            Log.d(TAG, msg);  
        }
    }
    
    public DemoOtaRequest(){
    	try {
			mRemoteURI = new URI(getRemoteUri());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public static String getRemoteUri() {
    	return null;
    } 
    
	@Override
	public int requestRemoteServerForUpdate() throws IOException,
			ClientProtocolException {
		// TODO Auto-generated method stub
		mTargetURI = null;
		return URL_ERR;
	}

	@Override
	public void startUpdate(Context contex) {
		// TODO Auto-generated method stub
		mContext = contex;
		
		Intent intent= new Intent(mContext, OtaUpdateNotifyActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("uri", mTargetURI);
		
		return ;
	}
	
	@Override
	public  void  startUpdate(Context contex, boolean b) throws IOException, ClientProtocolException {	
	}

}
