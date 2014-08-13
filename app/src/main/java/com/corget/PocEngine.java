package com.corget;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class PocEngine {
	static {
		//int version = android.os.Build.VERSION.SDK_INT;
		//Log.i("poc", "version:" + version);
		//if(version > 8){
		 	System.loadLibrary("poc");
		//}else{
			//System.loadLibrary("pocjav");
		//}
		
	}
	private static Handler handler = null;
	

	public static void setHandler(Handler h) {
		handler = h;
	}

	static boolean spkIsOnMusic = true;
	
	static boolean AutoHangup = true;

	public static boolean isCalling = false;
	public static boolean isOffLine = false;

	public native int Init();

	public native int Destroy();

	public native int Login();

	public native int CancelReLogin();

	public native int CancelLogin();

	public native int AddBuddyUser();

	public native int AddBuddyUser(long userId);

	public native int AddBuddyUser(String account);

	public native int RemoveBuddyUser();

	public native int ModifyPassword(String oldName, String newName);
	public native int CanMonitorGroupByIdx(int aIndex);
	public native int AddMonitorGroupByIdx(int aIndex);
	public native int RemoveMonitorGroupByIdx(int aIndex);
	public native int AddMonitorGroup();
	public native int RemoveMonitorGroup();

	public native int EnterGroup();

	public native int LeaveGroup();

	public native int InviteTmpGroup();

	public native int StartPTT(int aIdx);

	public native int EndPTT();

	public native int LogOut();

	public native int GetUserListCount();

	public native int GetUserListItem(int idx);

	public native int SetUserListItemMask(int idx, boolean mark);

	public native void PreGroupOfMgr();

	public native void NextGroupOfMgr();

	// new interface
	public native void ToGroup(int aIndex);
	public native void ToContactList();

	public native void ToGroupIndex();

	public native int IsGroupIndex();

	public native int CanEnterGroup(int aIndex);

	public native void EnterGroupIdx(int aIndex);

	public native int GetCurrentState();
	public native long  GetOneMarksUserId();

	public native boolean IsCurrentBuddyList();

	public native boolean HasTmpGroup();

	public native boolean IsCurrentTmpGroup();

	public native boolean IsInCurrentGroupList();

	public native boolean IsInGroupList();

	public native String GetCurrentGroupTitle();

	public native String GetActivateGroupTitle();

	public native void ResetCurrentGroupMark();

	public native boolean IsCurrentGroupMaster();

	public native boolean IsMonitorCurrentGroup();

	public native int SetServerIp(String aPttIp);
	public native String GetServerIp();
	public native String GetString(int idx);

	public native String GetName();

	public native String GetAccount();

	public native long GetUserId();

	public native String GetPassword();

	public native boolean GetRmbPwd();

	public native boolean GetAutoLogin();

	public native long GetActiveGroupId();

	public native int GetUserPrivilege();

	public native int GetClientState();

	public native void SetAccount(String act);

	public native void SetUserId(long id);

	public native void SetPassword(String pwd);

	public native void SetRmbPwd(boolean rmb);

	public native void SetAutoLogin(boolean auto);

	public native void SetActiveGroupId(long groupId);
	public native void SaveConfig();
	public native void SetLocation(double lat, double lng);
	

	public native long GetLocationTime();
	
	public native String GetServUrl();
	public native long GetRandom();
	
	public native int  InviteIpTel(long userId);
	public native int  AcceptIpTel();
	public native int  RejustIpTel();
		
	public native void SetLoginType(int aType);

	public native int  SendDataToUser(long aMsgId, long toUserId, String msg);
	public native int  SendDataToGroup(long aMsgId, long toGroupId, String msg);
	public native int  SendMsgAck(long aMsgId, long to, long type);

	public native String GetUserName(long UserId);
	public native void SetMcastIp(String ip);
	
	public native void UpdateUserData();
	
	public native long GetUserIdByIndex(int aIdx);
	
	public native int  InviteTmpGroupById(long UserId);
	public native int  InviteTmpGroupByAct(String account);
	
	public native void SetVolume(int volume);
	public native int  GetVolume();
	
	public native void SetPath(String path);
	
	
	
	static SoundPool soundPool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
	static int SoundId[] = new int[3];

	public static void InitSoundPool(Context context) {
		SoundId[0] = soundPool.load(context, R.raw.end, 0);
		SoundId[1] = soundPool.load(context, R.raw.end, 0);
		SoundId[2] = soundPool.load(context, R.raw.error, 0);
	}

	public static void PlaySound(int idx) {	
		if (idx == 1) {
			soundPool.play(SoundId[idx], 0.5f, 0.5f, 10, 1, 1.2f);
		}else if (idx == 0 || idx == 3) {
			soundPool.play(SoundId[idx], 0.5f, 0.5f, 10, 0, 1);
		}
	}

	// /////////////////////////////////////////////////////////////

	public static UserItem g_item = new UserItem();

	public static void SetUserItemData(String name, int header, boolean mark) {
		if (handler == null) {
			return;
		}
		g_item.Name = name;
		if (g_item.Name == null) {
			g_item.Name = "";
		}
		g_item.Status = header;
		g_item.Checked = mark;
	}

	public static void UI_UpdateUserList() {
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_UpdateUserList);
		handler.sendMessage(msg);
	}

	public static void UI_SetTalker(String spk, boolean aBtnable) {
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_SetTalker, aBtnable ? 1
				: 0, 0, spk);
		handler.sendMessage(msg);
	}

	public static void UI_SetTalkInfo(int aStrId) {
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_SetTalkInfo, aStrId, 0);
		handler.sendMessage(msg);
	}

	public static boolean UI_QueryInviteSession(String aUserName, long UserId,
			byte aSessionId) {
		return isCalling;
	}
    

	public static void UI_ShowUserListView() {
		if (handler == null) {
			return;
		}
		
		Message msg = handler.obtainMessage(Methods.UI_ShowUserListView);
		handler.sendMessage(msg);

	}

	public static void UI_ShowLogoutView() {
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_ShowLogoutView);
		handler.sendMessage(msg);
	}

	public static void UI_RetryLogin() {
		if (!spkIsOnMusic) {
			PocService.SetSpkMode(true);
		}
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_RetryLogin);
		handler.sendMessage(msg);
	}

	public static void UI_ShowError(int aErrorId) {
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_ShowError, aErrorId, 0);
		handler.sendMessage(msg);
	}

	public static void UI_ShowNote(int aStringId) {
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_ShowNote, aStringId, 0);
		handler.sendMessage(msg);
	}


	public static void UI_UpdateTitle() {
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_UpdateTitle);
		handler.sendMessage(msg);

	}


	public static void UI_Login() {
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_Login);
		handler.sendMessage(msg);
	}

	public static void UI_ShowLoginInformation(int aStringId) {
		if(isOffLine){
			return;
		}
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_ShowLoginInformation,
				aStringId, 0);
		handler.sendMessage(msg);
	}

	
	
	public static void UI_NotifyUpdateSoftWare(String aURL) {
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_NotifyUpdateSoftWare,
				aURL);
		handler.sendMessage(msg);
	}
	
	public static void  UI_NotifyVoIPCall(String userName)
	{
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_NotifyVoIPCall, userName);
		handler.sendMessage(msg);
	}
	public static void  UI_NotifyVoIPVoice(String userName)
	{
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_NotifyVoIPVoice,userName);
		handler.sendMessage(msg);
	}
	public static void  UI_NotifyVoIPHangoff()
	{
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_NotifyVoIPHangoff);
		handler.sendMessage(msg);
	}
	public static void  UI_NotifyVoIPCalling(String userName)
	{
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_NotifyVoIPCalling, userName);
		handler.sendMessage(msg);
	}
	public static void  UI_NotifyVoIPRing(String userName)
	{
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_NotifyVoIPRing, userName);
		handler.sendMessage(msg);
	}
	public static void  UI_SetVoIPBtnStatus(int callEnable, int endEnable)
	{
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(Methods.UI_SetVoIPBtnStatus, callEnable, endEnable);
		handler.sendMessage(msg);	
	}
	
	public static void  UI_StartPTT()
	{
		if (handler == null) {
			return;
		}
		Message msg1 = handler.obtainMessage(Methods.POC_StartPtt);
		handler.sendMessage(msg1);	
	}

	public static void  UI_EndPTT()
	{
		if (handler == null) {
			return;
		}
		Message msg1 = handler.obtainMessage(Methods.POC_EndPtt);
		handler.sendMessage(msg1);	
	}
	public static void  UI_NotifyMsg(String msg, int aUserId, int msgId)
	{
		Log.i("poc", "UI_NotifyMsg:" +aUserId + ":" + msgId);
		if (handler == null) {
			return;
		}
		Message msg1 = handler.obtainMessage(Methods.UI_NotifyMsg, (int)aUserId, (int)msgId, msg);
		handler.sendMessage(msg1);	
	}

	
	public static void  UI_NotifyMsgAck(int aUserId, int msgId, int aType)
	{
		if (handler == null) {
			return;
		}
		Message msg1 = handler.obtainMessage(Methods.UI_NotifyMsgAck, (int)aUserId, (int)msgId, String.valueOf(aType));
		handler.sendMessage(msg1);	
	}
	
	
	public static String UI_GetLocalIp() {       
	      try {       
	         for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {       
	             NetworkInterface intf = en.nextElement();       
	              for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {       
	                  InetAddress inetAddress = enumIpAddr.nextElement();       
	                  if (!inetAddress.isLoopbackAddress()) {       
	                     return inetAddress.getHostAddress().toString();       
	                 }        
	              }       
	          }       
	      } catch (Exception ex) {       
	              
	      }       
	     return null;       
	 } 
}
