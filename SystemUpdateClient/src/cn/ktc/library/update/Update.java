package cn.ktc.library.update;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import cn.ktc.library.update.Version;
import cn.ktc.library.utils.MD5;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.systemupdate.service.R;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


/**
 * 软件升级类 使用方法：1、升级当前的应用程序：new Update(context).checkUpdate(); 2、升级指定包名的应用程序：new
 * Update(context， pkgName).checkUpdate();
 * 
 * 在AndroidManifest.xml中添加以下两个权限 <uses-permission
 * android:name="android.permission.INTERNET"/> <uses-permission
 * android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
 * 
 * @author yejb
 * 
 */
public class Update {

	private static final String LOG_TAG = Update.class.getSimpleName();

	private static final boolean DEBUG = true;

	private static final String NOT_USB_DEVICE = "notusbdevicektc";

	private static final String DEFAULT_SERVER_VERSION_URL = "http://update.ktc.cn:2300/6a628/philips/launcher/version.json";

	private String mServerVersionUrl;
	private String mCurrentPkg;

	private Context mContext;

	private AlertDialog mUpdateDialog;
	private DownloadTask mDownloadTask;

	private LinearLayout llChecking;
	private LinearLayout llChecked;
	private TextView tvMsg;

	private ProgressDialog mProgressDialog;

	private Version mVersion = null;

	/**
	 * 默认升级调用该类的应用程序
	 * 
	 * @param context
	 */
	public Update(Context context) {
		this(context, context.getApplicationInfo().packageName);
	}

	/**
	 * 升级指定包名的应用程序
	 * 
	 * @param context
	 * @param pkgName
	 *            包名
	 */
	public Update(Context context, String pkgName) {
		this.mContext = context;
		this.mCurrentPkg = pkgName;
		this.mServerVersionUrl =SystemProperties.get("ro.product.ota.update","");
	}

	/**
	 * 设置升级的服务器地址
	 * 
	 * @param url
	 */
	public void setServerUrl(String url) {
		mServerVersionUrl = url;
		mServerVersionUrl =SystemProperties.get("ro.product.ota.update","");
	}

	/**
	 * 设置版本信息
	 * 
	 * @param version
	 */
	public void setVersion(Version version) {
		this.mVersion = version;
	}
	/**
	 * 判断是否有新版本 注意：该方法需要访问网络，在UI中需要异步调用，否则会造成UI线程阻塞
	 * 
	 * @return 有则返回新版本信息，否则返回null
	 */
	public Version hasNewVersion() {
		Version lastVersion = getUpdateVersion();
		if (lastVersion != null) {
			PackageVersion currentVersion = new PackageVersion(mContext,
					mCurrentPkg);
			int lastVerCode = lastVersion.getVerCode();
			int currentCode = currentVersion.getVerCode();
			if (currentCode > 0 && lastVerCode > currentCode) {
				return lastVersion;
			}
		}
		return null;
	}

	public void checkUpdate() {
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mProgressDialog = new ProgressDialog(mContext);  
	     mProgressDialog.setTitle("进度条窗口");  
	     mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);  
	     mProgressDialog.setMax(100);  
	     mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,"取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                if (DEBUG)
                  Log.d(LOG_TAG, "dialog dismiss");
              if (mDownloadTask != null
                      && mDownloadTask.getStatus() != AsyncTask.Status.FINISHED) {
                  mDownloadTask.cancel(true);
              }
            }
        });
	     //mProgressDialog.show();


		// 不显示正在检测界面，直接显示下载界面
		if (mVersion != null) {
			if (mDownloadTask != null
					&& mDownloadTask.getStatus() != AsyncTask.Status.FINISHED) {
				mDownloadTask.cancel(true);
			}
			mDownloadTask = new DownloadTask();
			mDownloadTask.execute(mVersion.getUrl());
		}
		// if (mCheckUpdateTask != null
		// && mCheckUpdateTask.getStatus() != AsyncTask.Status.FINISHED) {
		// mCheckUpdateTask.cancel(true);
		// }
		// mCheckUpdateTask = new CheckUpdateTask();
		// mCheckUpdateTask.execute();
	}


    public static String exec(String[] args) {
        String result = "";
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        InputStream errIs = null;
        InputStream inIs = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = -1;
            process = processBuilder.start();
            errIs = process.getErrorStream();
            while ((read = errIs.read()) != -1) {
                baos.write(read);
            }
            baos.write('n');
            inIs = process.getInputStream();
            while ((read = inIs.read()) != -1) {
                baos.write(read);
            }
            byte[] data = baos.toByteArray();
            result = new String(data);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (errIs != null) {
                    errIs.close();
                }
                if (inIs != null) {
                    inIs.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (process != null) {
                process.destroy();
            }
        }
        System.out.println(result);
        return result;
    }

	/**
	 * 文件下载异步任务
	 * 
	 * @author yejb
	 * 
	 */
	private class DownloadTask extends AsyncTask<String, Integer, String> {

		private boolean taskCancelled = false;

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
		}

		@TargetApi(Build.VERSION_CODES.FROYO)
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub

			String fileUrl = params[0];
			int retryCount = 3;

			int retriedCount;
			for (retriedCount = 0; retriedCount < retryCount; retriedCount++) {
				RandomAccessFile savedFile = null;
				try {
                    File file;
                    boolean usedSD;
                    // 系统为2.2及以上版本并且系统已挂载SD卡，将文件存放到SD中
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        file = new File(mContext.getFilesDir()+"/"+
                                mVersion.getFileName());
						usedSD = true;
						long offset = 0;
						if (file.exists()) {
							try {
								// 判断APK文件是否完整
								int apkVersion = getApkVersion(file.getPath());
								if (DEBUG)
									Log.d(LOG_TAG, "apkVersion: " + apkVersion);
								// 完整，再判断是否和服务器更新文件相同
								String md5 = MD5.md5sum(file.getPath());
								if (DEBUG)
									Log.d(LOG_TAG, "md5: " + md5);
								if (md5.equals(mVersion.getMD5())) {
									return file.getPath();
								} else {
									file.delete();
								}
							} catch (Exception e) {
								// TODO: handle exception
								e.printStackTrace();
							}

							offset = file.length();
						}
			            String[] args = { "chmod", "604",file.getAbsolutePath()};

						if (DEBUG)
							Log.d(LOG_TAG, "offset: " + offset);
						URL url = new URL(fileUrl);
						URLConnection con = url.openConnection();
						con.setConnectTimeout(5000);
						con.setReadTimeout(5000);
						con.setRequestProperty("RANGE", "bytes=" + offset + "-");
						// if
						// ("bytes".equals(con.getHeaderField("Accept-Ranges")))
						// {
						// con.setRequestProperty("RANGE","bytes=" + offset +
						// "-");
						// }
						con.connect();
						long fileSize = con.getContentLength();

						if (DEBUG)
							Log.d(LOG_TAG, "fileSize: " + fileSize);
						fileSize += offset;
						if (usedSD) {
							double sdFree = freeSpaceOnSd();
							if (sdFree <= fileSize) {
								if (DEBUG)
									Log.e(LOG_TAG,
											"SD card is not enough free space");
								return null;
							}
						}
						InputStream raw = con.getInputStream();
						InputStream is = new BufferedInputStream(raw);
						savedFile = new RandomAccessFile(file, "rw");
						// 从文件尾开始追加
						savedFile.seek(offset);
						int bytesRead = -1;
						byte[] buf = new byte[1024 * 5];
						int percent = 0;
						while (!taskCancelled) {
							bytesRead = is.read(buf);
							if (bytesRead < 0) {
								break;
							}
							savedFile.write(buf, 0, bytesRead);
							offset += bytesRead;
							percent = (int) (offset * 100.0 / fileSize);
							publishProgress(percent);
							if (DEBUG)
								Log.d(LOG_TAG, "bytesRead: " + bytesRead);
						}
						if (offset == fileSize) {
		                      String result=exec(args);
							return file.getPath();
						} else {
							return null;
						}
					} else {
						return NOT_USB_DEVICE;
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
					break;
				} catch (UnsupportedEncodingException e) {
					// TODO: handle exception
					e.printStackTrace();
					break;
				} catch (SocketTimeoutException e) {
					if (DEBUG)
						Log.e(LOG_TAG, "downImageView:timeout");
				} catch (IOException e) {
					e.printStackTrace();
					break;
				} finally {
					if (savedFile != null) {
						try {
							savedFile.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			}

			return null;
		}

		@Override
		protected void onCancelled() {
			// TODO Auto-generated method stub
			super.onCancelled();
			taskCancelled = true;
			if (DEBUG)
				Log.d(LOG_TAG, "download cancelled");
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub
			setProgressPercent(values[0]);
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub

		    mProgressDialog.dismiss();
			if (NOT_USB_DEVICE.equals(result)) {
				Toast.makeText(mContext, R.string.download_failed,
						Toast.LENGTH_SHORT).show();
			} else {
				if (!TextUtils.isEmpty(result)) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.setDataAndType(Uri.fromFile(new File(result)),
							"application/vnd.android.package-archive");
					//mContext.startActivity(intent);
					
					installSlient(mContext, result);
				} else {
					Toast.makeText(mContext, R.string.download_failed,
							Toast.LENGTH_SHORT).show();
				}
			}

			super.onPostExecute(result);
		}
	}

	/**
	 * 计算SD卡剩余空间
	 * 
	 * @return
	 */
	private double freeSpaceOnSd() {
	    StatFs stat = new StatFs(Environment.getDataDirectory()
                .getPath());
//	    Log.i(Constants.TAG, "feee:"+(double) stat.getAvailableBlocks()
//                * (double) stat.getBlockSize());
        return (double) stat.getAvailableBlocks()
                * (double) stat.getBlockSize();
	}

	/**
	 * 获取Apk文件的版本
	 * 
	 * @param path
	 *            Apk文件路径
	 * @return
	 */
	private int getApkVersion(String path) {
		PackageManager pm = mContext.getPackageManager();
		PackageInfo info = pm.getPackageArchiveInfo(path,
				PackageManager.GET_ACTIVITIES);
		return info.versionCode;
	}

	/**
	 * 设置进度百分百
	 * 
	 * @param percent
	 */
	private void setProgressPercent(int percent) {
	    mProgressDialog.setProgress(percent);
//		btnProgress.setText(mContext.getResources().getString(
//				R.string.downloading_package)
//				+ percent + "%");
	}

	/**
	 * 从服务器获取远程当前程序版本信息
	 * 
	 * @param serverUrl
	 * @param pkgName
	 * @return
	 */
	public Version getUpdateVersion() {
		DefaultHttpClient http = new DefaultHttpClient();
		HttpResponse response = null;
		try {
			if(!mServerVersionUrl.equals("")){
			HttpGet method = new HttpGet(mServerVersionUrl);
			response = http.execute(method);
			InputStream data = response.getEntity().getContent();

			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(data));
			StringBuffer sb = new StringBuffer();
			String line =bufferedReader.readLine();
			while (line != null) {
				sb.append(line).append("\r\n");
				line =bufferedReader.readLine();
			}
			bufferedReader.close();
			String rlt = sb.toString();
			List<Version> versions = Version.constructVersion(rlt);
			if(versions!=null){
				for (Version version : versions) {
					if (mCurrentPkg.equals(version.getPkgName())) {
						return version;
					}
				}
			}
		}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 根据包名获取指定应用程序的版本信息
	 * 
	 * @author yejb
	 * 
	 */
	private static class PackageVersion {

		private Context mContext;
		private String mPkgName;
		private int mVerCode;
		private String mVerName;

		public PackageVersion(Context context, String pkgName) {
			this.mContext = context;
			this.mPkgName = pkgName;

			init();
		}

		private void init() {
			try {
				mVerCode = mContext.getPackageManager().getPackageInfo(
						mPkgName, 0).versionCode;
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				mVerCode = -1;
			}

			try {
				mVerName = mContext.getPackageManager().getPackageInfo(
						mPkgName, 0).versionName;
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				mVerName = null;
			}
		}

		/**
		 * 获取应用程序包名
		 * 
		 * @return
		 */
		public String getPkgName() {
			return this.mPkgName;
		}

		/**
		 * 获取版本号
		 * 
		 * @return
		 */
		public int getVerCode() {
			return this.mVerCode;
		}

		/**
		 * 获取版本名
		 * 
		 * @return
		 */
		public String getVerName() {
			return this.mVerName;
		}
	}
	
	/**
     * install slient
     * 
     * @param context
     * @param filePath
     * @return 0 means normal, 1 means file not exist, 2 means other exception error
     */
    public static int installSlient(Context context, String filePath) {
    	File file = new File(filePath);
    	if (filePath == null || filePath.length() == 0 || (file = new File(filePath)) == null || file.length() <= 0
    		|| !file.exists() || !file.isFile()) {
    		return 1;
    	}

    	String[] args = { "pm", "install", "-r", filePath };
    	ProcessBuilder processBuilder = new ProcessBuilder(args);

    	Process process = null;
    	BufferedReader successResult = null;
    	BufferedReader errorResult = null;
    	StringBuilder successMsg = new StringBuilder();
    	StringBuilder errorMsg = new StringBuilder();
    	int result;
    	try {
    		process = processBuilder.start();
    		successResult = new BufferedReader(new InputStreamReader(process.getInputStream()));
    		errorResult = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    		String s;

    		while ((s = successResult.readLine()) != null) {
    			successMsg.append(s);
    		}

    		while ((s = errorResult.readLine()) != null) {
    			errorMsg.append(s);
    		}
    	} catch (IOException e) {
    		e.printStackTrace();
    		result = 2;
    	} catch (Exception e) {
    		e.printStackTrace();
    		result = 2;
    	} finally {
    		try {
    			if (successResult != null) {
    				successResult.close();
    			}
    			if (errorResult != null) {
    				errorResult.close();
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		if (process != null) {
    			process.destroy();
    		}
    	}

    	// TODO should add memory is not enough here
    	if (successMsg.toString().contains("Success") || successMsg.toString().contains("success")) {
    		result = 0;
    	} else {
    		result = 2;
    	}
    	Log.d("installSlient", "successMsg:" + successMsg + ", ErrorMsg:" + errorMsg);
    	return result;
    }

}
