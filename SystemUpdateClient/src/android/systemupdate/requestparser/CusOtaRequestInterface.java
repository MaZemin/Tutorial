package android.systemupdate.requestparser;


import java.io.IOException;
import org.apache.http.client.ClientProtocolException;
import android.content.Context;
import android.content.Intent;

public interface CusOtaRequestInterface {
	public final static int NETWORK_SERVER_OK = 0;
	public final static int NETWORK_ERROR = 1;
	public final static int SERVER_ERROR = 2;
	public final static int SERVER_NO_PACKAGE = 3;
	public final static int SYSTEM_NEW = 4;
	public final static int URL_ERR = 5;
	
	public final static int REQUEST_NUM = 100;
	
	public  int requestRemoteServerForUpdate() throws IOException, ClientProtocolException ;
	public  void  startUpdate(Context contex) throws IOException, ClientProtocolException;	
	public  void  startUpdate(Context contex, boolean b) throws IOException, ClientProtocolException;
}
