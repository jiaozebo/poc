package com.corget;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Vector;

import com.android.internal.telephony.ITelephony;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class PocService extends Service {

	public PocEngine pocEngine;
	public MainView mainView = null;

    public LocationManager  mLocationManager;

	//static AudioManager     mAudioManager = null;
	static TelephonyManager mTeleManager  = null;

	public double mLat = 0;
	public double mLng = 0;
	
	public LocationListener mLocationListener = new LocationListener() {

        public void onLocationChanged(final Location loc) {
        	if(loc != null){
        		if(loc.getLatitude() != 0 || loc.getLongitude() != 0){
        			Log.i("poc", "onLocationChanged" + loc.getLatitude() + ", " + loc.getLongitude());
        			PocService.this.mLat = loc.getLatitude();
        			PocService.this.mLng = loc.getLongitude();
        			pocEngine.SetLocation(PocService.this.mLat, PocService.this.mLng);
        			
        		}
        		Toast.makeText(PocService.this, "经度:" + loc.getLatitude() + "\n纬度:" + loc.getLongitude(), Toast.LENGTH_SHORT).show();
        	}  	
        }
        public void onProviderDisabled(final String s) {
        	Log.i("poc", "onProviderDisabled");
        }
        public void onProviderEnabled(final String s) {
        	Log.i("poc", "onProviderEnabled");
        }
        public void onStatusChanged(final String s, final int i, final Bundle b) {
        	Log.i("poc", "onStatusChanged" + s + i);
    
        }
        
    };
    
    
	public void setMainView(MainView view) {
		mainView = view;
		pocEngine.setHandler(view.handler());
		//OpenLocation(false);
	}

	public class PocBinder extends Binder {
		PocService getService() {
			return PocService.this;
		}
	}
    
	boolean already_add_notify = false;
	boolean Show_setting = true;
	public void OpenLocation()    {
		if(already_add_notify){
			return;
		}
		
		long SendTime = pocEngine.GetLocationTime();
		if(SendTime <= 0){
		    return;
		}
        Log.i("poc", "Location: " + SendTime);
		if(mLocationManager == null){
			mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);  
		}
		//if(mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
		//	
		//}
		String provider = LocationManager.GPS_PROVIDER;
		if(mLocationManager.isProviderEnabled(provider)){
			Location location = mLocationManager.getLastKnownLocation(provider);
		    mLocationListener.onLocationChanged(location);			
	        mLocationManager.requestLocationUpdates(provider, SendTime * 1000, 10, mLocationListener);
	        already_add_notify = true;
		}else{
			if(Show_setting){
	        	if(mainView != null){
	        		mainView.PostLocationSetting();
	        	}
	        	Show_setting = false;
	        }//else{
	        //	Toast.makeText(PocService.this, "GPS没有打开!", Toast.LENGTH_LONG).show();
	        //}
		} 
	}
	
	
	public void CloseLocation()  {
		if(mLocationManager != null){
			if(already_add_notify){
				already_add_notify = false;
				mLocationManager.removeUpdates(mLocationListener);
			}
			
		}
	}
	
	
	
	private final static int NOTIFICATION_ID = 0x0001;  
    private NotificationManager mNotificationManager; 
	

    public void AddNotify(){
    	Notification mNotification = new Notification(R.drawable.icon, null, 0);  
    	mNotification.flags =  Notification.FLAG_ONGOING_EVENT | mNotification.flags;
        mNotificationManager = (NotificationManager) this   
                .getSystemService(NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent mContentIntent = PendingIntent.getActivity(this,   
                0, intent, 0); 
    	
        mNotification.setLatestEventInfo(this, getString(R.string.app_name), getString(R.string.notify_name), 
                mContentIntent);   
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);  
    }
    
    public void RemoveNotify(){
    	if(mNotificationManager != null){
    		mNotificationManager.cancel(NOTIFICATION_ID);
    	}
    }


	WakeLock mWakeLock = null;
		
	@Override
	public void onCreate() {
		Log.i("poc", "onCreate");
		InitEngine();
        
		//mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		//if (PocEngine.spkIsOnMusic) {
		//	mAudioManager.setMode(AudioManager.MODE_NORMAL);
		//} else {
		//	mAudioManager.setMode(AudioManager.MODE_IN_CALL);
		//}
    	mTeleManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
    	mTeleManager.listen(new PhoneCallListener(), PhoneCallListener.LISTEN_CALL_STATE);
		
		if(mWakeLock == null){
			PowerManager pManager = ((PowerManager) getSystemService(POWER_SERVICE));   
            mWakeLock = pManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "POC");
			
		}
		AddNotify();
		
		//设置音量大下，0 - 1 声音变小，1 不变， 1- 10变大。
		SetVolume(1);

		
		IntentFilter filter = new IntentFilter();  
		  
		filter.addCategory("android.bluetooth.headset.intent.category.companyid.85");
		filter.addAction("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT"); 
		registerReceiver(mBroadcastReceiver, filter);  
		
	}


	
	SMSReceiver mBroadcastReceiver = new SMSReceiver();

	public void onStart(Intent intent, int startId) {
		Log.i("poc", "onStart");
		super.onStart(intent, startId);

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("poc", "Received start id " + startId + ": " + intent);
		return START_STICKY;
	}

	
	@Override
	public void onDestroy() {
		Log.i("poc", "onServiceDestroy");
		unregisterReceiver(mBroadcastReceiver);
    } 

	@Override
	public IBinder onBind(Intent intent) {
		Log.i("poc", "onBind");
		return mBinder;
	}

	public boolean onUnbind(Intent intent) {
		Log.i("poc", "onUnbind");
		return true;
	}

	static public void SetSpkMode(boolean mode) {
		if (mode) {
		//	mAudioManager.setMode(AudioManager.MODE_RINGTONE);
		} else {
		//	mAudioManager.setMode(AudioManager.MODE_IN_CALL);
		}
	}
	
	public void Close(){
		CloseLocation();
		RemoveNotify();
		
		Destroy();

		stopSelf();
	}

	private final IBinder mBinder = new PocBinder();

	// //////////////////////////////////////////////////////
	public void InitEngine() {
		File path = getFilesDir();
		String path_string = path.getPath();
		Log.i("poc", "path:" + path_string);
		File configfile = new File(path_string + "/config.txt");
		// configfile.delete();
		if (!configfile.exists()) {
			CopyConfig(configfile, R.raw.config);
		}
		File stringfile = new File(path_string + "/strings.rs");
		// stringfile.delete();
		if (!stringfile.exists()) {
			CopyConfig(stringfile, R.raw.strings);
		}

		pocEngine = new PocEngine();
		pocEngine.SetPath(path_string);
		pocEngine.Init();
		pocEngine.InitSoundPool(this);

	}

	private void CopyConfig(File configfile, int file) {
		java.io.InputStream dataStream = null;
		java.io.FileOutputStream out = null;
		byte buffer[] = new byte[256];
		int length = 0;
		try {
			dataStream = this.getResources().openRawResource(file);
			if (dataStream == null) {
				return;
			}
			configfile.createNewFile();
			out = new java.io.FileOutputStream(configfile);
			while ((length = dataStream.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}
		} catch (Exception e) {
			Log.i("poc", e.getMessage());
		} finally {
			if (dataStream != null) {
				try {
					dataStream.close();
				} catch (IOException e) {
					Log.i("poc", e.getMessage());
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					Log.i("poc", e.getMessage());
				}
			}
		}
	}
	//////////////////////////////////////////////////////////
	class PlayRingThread extends Thread{
		MediaPlayer mMediaPlayer;
		Context mContext;
	    public PlayRingThread(Context mContext) {
		    mMediaPlayer = new MediaPlayer();
		    this.mContext = mContext;
		}


		@Override
		public void run() {
		    Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		    try {
		        mMediaPlayer.setDataSource(mContext, alert);
		        final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		        //audioManager.setMode(AudioManager.MODE_IN_CALL);
		        if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
		           mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
		           mMediaPlayer.setLooping(true);
		           mMediaPlayer.prepare();
		        }
		    } catch (IllegalStateException e) {
		        e.printStackTrace();
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		    mMediaPlayer.start();
		}


		private void StopAlarmRing() {
		    mMediaPlayer.stop();
		}
	}
    
	PlayRingThread mRingPlay = null;
	
	public void StartPlayRing()
	{
		if(mRingPlay == null){
			mRingPlay = new PlayRingThread(this);
		    mRingPlay.start();
		}
	}
	
	public void StopPlayRing()
	{
		if(mRingPlay != null){
			mRingPlay.StopAlarmRing();
			mRingPlay = null;
		}	
	}
	
	// ////////////////////////////////////////////////////////////////////
	public int Init() {
		return pocEngine.Init();
	}

	public int Destroy() {
		return pocEngine.Destroy();
	}

	public int Login() {
		pocEngine.isOffLine = false;
		if(null != mWakeLock){  
			if(!mWakeLock.isHeld()){
				mWakeLock.acquire();
			}  
	    }
		return pocEngine.Login();
	}

	public int CancelReLogin() {
		pocEngine.isOffLine = true;
		return pocEngine.CancelReLogin();
	}

	public int CancelLogin() {
		if(null != mWakeLock){  
			if(mWakeLock.isHeld()){
				mWakeLock.release();
			}  
	    }
		pocEngine.isOffLine = true;
		return pocEngine.CancelLogin();
	}

	public int AddBuddyUser() {
		return pocEngine.AddBuddyUser();
	}

	public int AddBuddyUser(long userId) {
		return pocEngine.AddBuddyUser(userId);
	}

	public int AddBuddyUser(String account) {
		return pocEngine.AddBuddyUser(account);
	}

	public int RemoveBuddyUser() {
		return pocEngine.RemoveBuddyUser();
	}


	public int ModifyPassword(String oldName, String newName) {
		return pocEngine.ModifyPassword(oldName, newName);
	}

	public int CanMonitorGroupByIdx(int aIndex)
	{
		return pocEngine.CanMonitorGroupByIdx(aIndex);
	}
	
	public int AddMonitorGroupByIdx(int aIndex)
	{
		return pocEngine.AddMonitorGroupByIdx(aIndex);
	}	
	
	public int RemoveMonitorGroupByIdx(int aIndex)
	{
		return pocEngine.RemoveMonitorGroupByIdx(aIndex);
	}
	
	public int AddMonitorGroup() {
		return pocEngine.AddMonitorGroup();
	}

	public int RemoveMonitorGroup() {
		return pocEngine.RemoveMonitorGroup();
	}


	public int EnterGroup() {
		return pocEngine.EnterGroup();
	}

	public int LeaveGroup() {
		return pocEngine.LeaveGroup();
	}

	public int InviteTmpGroup() {
		return pocEngine.InviteTmpGroup();
	}

	public int StartPTT() {
		return pocEngine.StartPTT(0);
	}

	public int EndPTT() {
		return pocEngine.EndPTT();
	}

	public int LogOut() {
		if(null != mWakeLock){  
			if(mWakeLock.isHeld()){
				mWakeLock.release();
			}  
	    }
		CloseLocation();
		pocEngine.isOffLine = true;
		return pocEngine.LogOut();
	}


	public int GetUserListCount() {
		return pocEngine.GetUserListCount();
	}

	public UserItem GetUserListItem(int idx) {
		if (pocEngine.GetUserListItem(idx) < 0) {
			return null;
		}
		return pocEngine.g_item;
	}

	public int SetUserListItemMask(int idx, boolean mark) {
		return pocEngine.SetUserListItemMask(idx, mark);
	}

	public void PreGroupOfMgr() {
		pocEngine.PreGroupOfMgr();
	}

	public void NextGroupOfMgr() {
		pocEngine.NextGroupOfMgr();
	}

	public void ToGroup(int aIndex) {
		pocEngine.ToGroup(aIndex);
	}


	public void ToContactList() {
		pocEngine.ToContactList();
	}
	public void ToGroupIndex() {
		pocEngine.ToGroupIndex();
	}

	public int IsGroupIndex() {
		return pocEngine.IsGroupIndex();
	}

	public int CanEnterGroup(int aIndex) {
		return pocEngine.CanEnterGroup(aIndex);
	}

	public void EnterGroupIdx(int aIndex) {
		pocEngine.EnterGroupIdx(aIndex);
	}

	public int GetCurrentState() {
		return pocEngine.GetCurrentState();
	}

	public long  GetOneMarksUserId(){
		return pocEngine.GetOneMarksUserId();
	}
	
	public boolean IsCurrentBuddyList() {
		return pocEngine.IsCurrentBuddyList();
	}

	public boolean HasTmpGroup() {
		return pocEngine.HasTmpGroup();
	}

	public boolean IsCurrentTmpGroup() {
		return pocEngine.IsCurrentTmpGroup();
	}

	public boolean IsInCurrentGroupList() {
		return pocEngine.IsInCurrentGroupList();
	}

	public boolean IsInGroupList() {
		return pocEngine.IsInGroupList();
	}

	public String GetCurrentGroupTitle() {
		return pocEngine.GetCurrentGroupTitle().trim();
	}

	public String GetActivateGroupTitle() {
		return pocEngine.GetActivateGroupTitle().trim();
	}

	public void ResetCurrentGroupMark() {
		pocEngine.ResetCurrentGroupMark();
	}

	public boolean IsCurrentGroupMaster() {
		return pocEngine.IsCurrentGroupMaster();
	}

	public boolean IsMonitorCurrentGroup() {
		return pocEngine.IsMonitorCurrentGroup();
	}

	public int SetServerIp(String aPttIp) {
		return pocEngine.SetServerIp(aPttIp);
	}

	public String GetServerIp() {
		return pocEngine.GetServerIp().trim();
	}


	public String GetString(int idx) {
		return pocEngine.GetString(idx).trim();
	}

	public String GetName() {
		return pocEngine.GetName().trim();
	}

	public String GetAccount() {
		return pocEngine.GetAccount();
	}

	public long GetUserId() {
		return pocEngine.GetUserId();
	}

	public String GetPassword() {
		return pocEngine.GetPassword().trim();
	}

	public boolean GetRmbPwd() {
		return pocEngine.GetRmbPwd();
	}

	public boolean GetAutoLogin() {
		return pocEngine.GetAutoLogin();
	}

	public long GetActiveGroupId() {
		return pocEngine.GetActiveGroupId();
	}

	public int GetUserPrivilege() {
		return pocEngine.GetUserPrivilege();
	}

	public int GetClientState() {
		return pocEngine.GetClientState();
	}


	public void SetAccount(String act) {
		pocEngine.SetAccount(act);
	}

	public void SetUserId(long id) {
		pocEngine.SetUserId(id);
	}

	public void SetPassword(String pwd) {
		pocEngine.SetPassword(pwd);
	}

	public void SetRmbPwd(boolean rmb) {
		pocEngine.SetRmbPwd(rmb);
	}

	public void SetAutoLogin(boolean auto) {
		pocEngine.SetAutoLogin(auto);
	}

	public void SetActiveGroupId(long groupId) {
		pocEngine.SetActiveGroupId(groupId);
	}

	public void SaveConfig() {
		pocEngine.SaveConfig();
	}
	
	
	public String GetServUrl() {
		return pocEngine.GetServUrl();
	}
	public long GetRandom() {
		return pocEngine.GetRandom();
	}
	

	public long GetLocationTime()
	{
		return pocEngine.GetLocationTime();
	}
	public int  InviteIpTel(long userId)
	{	
		return pocEngine.InviteIpTel(userId);
	}
	public int  AcceptIpTel()
	{
		return pocEngine.AcceptIpTel();
	}
	public int  RejustIpTel()
	{
		return pocEngine.RejustIpTel();
	}
	
	public int  SendDataToUser(long aMsgId, long toUserId, String msg)
	{
		return pocEngine.SendDataToUser( aMsgId, toUserId, msg);
	}
	
	public int  SendDataToGroup(long aMsgId, long toGroupId, String msg)
	{
		return pocEngine.SendDataToGroup( aMsgId, toGroupId, msg);
	}

	
	public int  SendMsgAck(long aMsgId, long to, long type)
	{
		return pocEngine.SendMsgAck( aMsgId, to, type);
	}
	
	public void SetLoginType(int aType)
	{
		pocEngine.SetLoginType(aType);
	}
	
	public String GetUserName(long UserId)
	{
		return pocEngine.GetUserName(UserId);
	}
	public void SetMcastIp(String ip)
	{
		pocEngine.SetMcastIp(ip);
	}
	
	public long GetUserIdByIndex(int aIdx)
	{
		return pocEngine.GetUserIdByIndex(aIdx);
	}
	
	public void UpdateUserData()
	{
		pocEngine.UpdateUserData();
	}
	
	public int  InviteTmpGroupById(long UserId)
	{
		return pocEngine.InviteTmpGroupById(UserId);
	}
	
	public int  InviteTmpGroupByAct(String account)
	{
		return pocEngine.InviteTmpGroupByAct(account);
	}
	
	public void SetVolume(int volume)
	{
		pocEngine.SetVolume(volume);
	}
	public int  GetVolume()
	{
		return pocEngine.GetVolume();
	}

	public class PhoneCallListener extends PhoneStateListener {
		// 回调调用设备时呼叫状态的变化。
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
			case TelephonyManager.CALL_STATE_RINGING:
				pocEngine.isCalling = false;
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK: 	
				pocEngine.isCalling = true;			
				break;
			default:
				break;
			}	
		}
	}
	

}