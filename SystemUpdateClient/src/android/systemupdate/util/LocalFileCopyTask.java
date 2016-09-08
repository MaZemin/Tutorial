package android.systemupdate.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParserException;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.systemupdate.service.SystemUpdateService;
import android.systemupdate.util.FileInfo.Piece;
import android.util.Log;

public class LocalFileCopyTask extends Thread{
	private String TAG = "LocalFileCopyTask";
	private String mSrcPath;
	private String mDesPath;
	private long  mFileLength;
	private long  mWriteLength;
	private boolean mDebug = true;
	private Handler mProgressHandler;
	private volatile int err = ERR_NOERR;
	private boolean requestStop = false;
	private static final int BUFF_SIZE = 4096;
	public static final int ERR_CONNECT_TIMEOUT = 1;
	public static final int ERR_NOERR = 0;
	public static final int ERR_FILELENGTH_NOMATCH = 2;
	public static final int ERR_REQUEST_STOP = 3;
	public static final int ERR_NOT_EXISTS = 4;
        public static final int PROGRESS_ERR_WARRING = 5;
	
	//message
	public static final int PROGRESS_UPDATE = 1;
	public static final int PROGRESS_STOP_COMPLETE = 2;
	public static final int PROGRESS_START_COMPLETE = 3;
	public static final int PROGRESS_COPY_COMPLETE = 4;
	
	
	public LocalFileCopyTask(String srcPath, String desPath) {
		mSrcPath = srcPath;
		mDesPath = desPath;
		Log.d(TAG, "LocalFileCopyTask create !!");
		Log.d(TAG, "LocalFileCopyTask mSrcPath = " + mSrcPath);
		Log.d(TAG, "LocalFileCopyTask create = mDesPath " + mDesPath);
	}	
	
	public void setProgressHandler(Handler progressHandler) {
		mProgressHandler = progressHandler;
	}
	
	@Override
	public void run() {
		try {
		    startTask();
		} catch (Exception e){
 		    e.printStackTrace();
		    onProgressStopComplete(err);		    
		}
	}

	private void startTask() throws Exception {
		File srcfile = new File(mSrcPath);
		
		Log.d(TAG, "mSrcPath = " + mSrcPath);
		Log.d(TAG, "mDesPath = " + mDesPath);
	    if(! srcfile.exists()){
	            Log.d(TAG, "source file is not exists!!!");
		    return;
		}
	    
	    if(!srcfile.isFile()){
        	    Log.d(TAG, "source fiel is not a file!!!");
		    return;
	    }

		mFileLength = srcfile.length();
		mWriteLength = 0;
		Log.d(TAG, "mFileLength = "+mFileLength);
         
 
                File desfile = new File(mDesPath);
		if (!new File(desfile.getParent()).exists()){
			if (!new File(desfile.getParent()).mkdirs()){
                            Log.d(TAG, "can't create destfile's parent's dir!!");
                            throw new Exception();
                        }
                }
		Log.d(TAG, "dest file dir is exist!!");
		
		StatFs stat = new StatFs(desfile.getParent());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		long availableBytes = blockSize * availableBlocks;       // int �� Max (4G) ������. 
		Log.d(TAG, "downloadImg() : 'blockSize' = " + blockSize + ", 'availableBlocks' = " + availableBlocks + "; Available FS space in desfile.getParent() :" + desfile.getParent() + " " + availableBytes + " bytes");
		/* �� FS �ռ� ����, �� ... */    
		if ( availableBytes < mFileLength ) {
		    Log.e(TAG, " No enough space in cache directory, Copy update.img fail!!");
		    //notifyErrorMessage("No enough space in cache directory, Copy update.img fail!!");
       		    onErrWarring(" No enough space in cache directory, Copy update.img fail!!");             
		    return ;
		}
               
		try{
		    // �½��ļ���������������л��� 
        	    FileInputStream input = new FileInputStream(srcfile);
        	    BufferedInputStream inBuff=new BufferedInputStream(input);

		    // �½��ļ��������������л��� 
        	    FileOutputStream output = new FileOutputStream(desfile);         // 默认会覆盖源文件,如果是追加的话FileOutputStream(desfile, true);
        	    BufferedOutputStream outBuff=new BufferedOutputStream(output);

		    // ��������
        	    byte[] b = new byte[BUFF_SIZE];
        	    int len; 
        	    int step = (int) ((mFileLength/BUFF_SIZE + 1)/100)*10;
        	    int loop = 0;
        	    int percent =0;
        	    while ((len =inBuff.read(b)) != -1){
        	    	outBuff.write(b, 0, len);
        	    	loop++;
        	    	if (loop == step)
        	    	{
        	    		loop = 0;
        	    		percent += 10;
        	    		onProgressUpdate(percent);
        	    	}
        	    }

        	    onProgressUpdate(100);
		    // ˢ�´˻��������� 
		    outBuff.flush();

		    //�ر��� 
		    inBuff.close(); 
		    outBuff.close(); 
		    output.close(); 
		    input.close();
		    stat = null;
		}catch(IOException exc){
		    Log.d(TAG, "copy update.img or update.zip fail!!");
		    onErrWarring("IO exception,copy udate.img or update.zip fail!!");
		    //broadcastAsyncReturn(UpdateManager.DOWNLOAD_IMG_RETURNED_ASYNC, UpdateManager.ERR_IO_ERROR);
		    return ;
		}

		Log.d(TAG, "copy complete!!");
		onProgressCopyComplete();
		return ;
	}
	
	public void stopCopy() {
		err = ERR_REQUEST_STOP;
		requestStop = true;
	}
	
	private void onProgressUpdate(int percent) {
		if(mProgressHandler != null) {
			Message m = new Message();
			m.what = PROGRESS_UPDATE;
			Bundle b = new Bundle();
			b.putInt("percent", percent);
			m.setData(b);
			
			mProgressHandler.sendMessage(m);
			Log.d(TAG, "send ProgressUpdate");
		}
	}
	
       private void onErrWarring(String errInfo){
                if(mProgressHandler != null) {
                        Message m = new Message();
                        m.what = PROGRESS_ERR_WARRING;
                        Bundle b = new Bundle();
                        b.putString("errInfo", errInfo);
                        m.setData(b);

                        mProgressHandler.sendMessage(m);
                        Log.d(TAG, "send onErrWarring!!");
                }
		
		return ;
       }
	
	private void onProgressStopComplete(int errCode) {
		if(mProgressHandler != null) {
			Message m = new Message();
			m.what = PROGRESS_STOP_COMPLETE;
			Bundle b = new Bundle();
			b.putInt("err", errCode);
			m.setData(b);
			
			mProgressHandler.sendMessage(m);
			Log.d(TAG, "send ProgressStopComplete");
		}
	}
	
	private void onProgressStartComplete() {
		if(mProgressHandler != null) {
			Message m = new Message();
			m.what = PROGRESS_START_COMPLETE;
			
			mProgressHandler.sendMessage(m);
			Log.d(TAG, "send ProgressStartComplete");
		}
	}
	
	private void onProgressCopyComplete() {
		if(mProgressHandler != null) {
			Message m = new Message();
			m.what = PROGRESS_COPY_COMPLETE;
			
			mProgressHandler.sendMessage(m);
			Log.d(TAG, "send onProgressCopyComplete");
		}
	}
}
