package com.corget;



import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;


public class MainView extends Activity {

	private static final int ENoMarkItem = 0;
	private static final int EMarkOfflineUser = 1;
	private static final int EMarkOffOnlineUser = 2;
	private static final int EMarkOnlineUser = 3;
	private static final int EMarkSessionUser = 4;

	private static final int PRIVILEGE_BUDDY = 0x00010001;
	private static final int PRIVILEGE_INVITE_TMPGROUP = 0x00010002;
	private static final int PRIVILEGE_CHANGE_GROUP = 0x00010004;
	private static final int PRIVILEGE_MODIFY_NAME = 0x00010008;

	private static final int PRIVILEGE_HISTORY_LOOK = 0x00010010;
	private static final int PRIVILEGE_GROUP_EDIT = 0x00010020;
	private static final int PRIVILEGE_MONITOR_GROUP = 0x00010040;

	private static final int PRIVILEGE_ALERT_ONLINE = 0x00010000;

	private static final String default_pwd = "^^^^^^";

	public static final int LOGIN_STATUS = 0;
	public static final int LOGINING_STATUS = 1;
	public static final int USERLIST_STATUS = 2;
	public static final int LOGOUTING_STATUS = 3;

	static final int DIALOG_NOTE_ID = 0;
	static final int DIALOG_QUERY_ID = 1;
	static final int DIALOG_INPUT_ID = 2;
	static final int DIALOG_INPUT2_ID = 3;

	static final String SPK_LABEL = "在讲话。";

	public static final String PocSettingFile = "PocSettingFile";


	public int ViewStatus = -1;

	// for login view
	private Button btnLogin;
	private EditText AccountEditer;
	private EditText PwdEditer;
	private CheckBox RmbPwdCheck;
	private CheckBox AutoLoginCheck;
	private TextView Information;

	// for cancel login view
	private Button btnCancelLogin;

	// for user list view
	private ListView userList;
	private TextView statusPane;
	private TextView groupPane;
	private TextView titlePane;
	//private TextView spkPane;
	//@+id/InviteTmpGroup
	private Button InviteBtn;
	private Button SpkBtn;
	private Button BackBtn;
	
	private PopupWindow SpkPopWindow;
	private PopupWindow InfoPopWindow;
	private View SpkPopView;
	private View InfoPopView;

	private boolean showGroupList = true;

	private  Handler handler;
	private Vector<Method> MethodArray = new Vector<Method>();
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i("poc", "MainView.onCreate()");
		super.onCreate(savedInstanceState);
		
		ConfigAutoLogin = true;
		// AutoLogin = savedInstanceState.getBoolean("login", false);
		// Log.i("poc", "MainView.onCreate(AutoLogin:" + AutoLogin);
		InitMethod();

		SharedPreferences settings = getSharedPreferences(PocSettingFile, 0);
		PocEngine.spkIsOnMusic = settings.getBoolean("SpkIsOnMusic", true);
		
		handler = new Handler(Looper.myLooper()) {
			public void handleMessage(Message msg) {
				handleMessage_inner(msg);
			}
		};
		startService(new Intent(this, PocService.class));
		
		LayoutInflater inflater = getLayoutInflater();
		SpkPopView = inflater.inflate(R.layout.note, null);
		InfoPopView = inflater.inflate(R.layout.note, null);
		
		SpkPopWindow = new PopupWindow(SpkPopView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);  
		InfoPopWindow = new PopupWindow(InfoPopView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false);  
		
	}

	protected void onStart() {
		Log.i("poc", "MainView.onStart()");
		super.onStart();

		Bundle bunde = this.getIntent().getExtras();
		if (bunde != null) {
			AutoLogin = bunde.getBoolean("login", false);
			Log.i("poc", "MainView.onCreate(AutoLogin:" + AutoLogin);
			
			//int ddd = bunde.getInt("PTT_KEY", -1);
			//Log.i("poc", "MainView.onCreate(PTT_KEYMainView:" + ddd);
			//finish();
		}
		doBindService();
	}

	protected void onRestart() {
		Log.i("poc", "MainView.onRestart()");
		super.onRestart();
		doBindService();
	}
	

	protected void onResume() {
		Log.i("poc", "MainView.onResume()");
		super.onResume();
		doBindService();
	}

	protected void onPause() {
		Log.i("poc", "MainView.onPause()");
		doUnbindService();
		super.onPause();
	}

	protected void onStop() {
		Log.i("poc", "MainView.onStop()");
		doUnbindService();
		super.onStop();
	}	

	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}

	private void ConstructView(int status) {
		switch (status) {
		case LOGIN_STATUS:
			ConstructLoginView();
			break;
		case LOGINING_STATUS:
			ConstructLoginingView();
			break;
		case USERLIST_STATUS:
			ConstructUserListView();
			
			break;
		case LOGOUTING_STATUS:
			ConstructLogoutingView();
			break;
		default:

		}
		AutoLogin = false;
	}

	public abstract class DlgClickListener implements DialogInterface.OnClickListener, Serializable {
	}

	public void showNote(String note) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(note)
				.setCancelable(false)
				.setPositiveButton(
						mBoundService.GetString(ResString.EStirngButtonOK),
						null);
		builder.create().show();
	}

	public void showQuery(String messge, DlgClickListener cancel,
			DlgClickListener ok) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(messge)
				.setCancelable(false)
				.setPositiveButton(
						mBoundService.GetString(ResString.EStirngButtonOK), ok)
				.setNegativeButton(
						mBoundService.GetString(ResString.EStirngButtonCancel),
						cancel);
		builder.create().show();
	}

	public void showTextQuery(String messge, String text,
			DlgClickListener cancel, DlgClickListener ok) {
		EditText inp = new EditText(this);
		inp.setId(1);
		inp.setText(text);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(messge)
				.setCancelable(false)
				.setView(inp)
				.setPositiveButton(
						mBoundService.GetString(ResString.EStirngButtonOK), ok)
				.setNegativeButton(
						mBoundService.GetString(ResString.EStirngButtonCancel),
						cancel);
		builder.create().show();
	}

	public void showText2Query(String messge, DlgClickListener cancel,
			DlgClickListener ok) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.inputs,
				(ViewGroup) findViewById(R.id.dialog_inputs));

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(messge)
				.setCancelable(false)
				.setView(layout)
				.setPositiveButton(
						mBoundService.GetString(ResString.EStirngButtonOK), ok)
				.setNegativeButton(
						mBoundService.GetString(ResString.EStirngButtonCancel),
						cancel);
		builder.create().show();
	}

	public void showSetting(String messge, DlgClickListener cancel,
			DlgClickListener ok) {
		LayoutInflater inflater = getLayoutInflater();
		View layout = inflater.inflate(R.layout.setting,
				(ViewGroup) findViewById(R.id.dialog_setting));
		EditText ServEditer = (EditText) layout
				.findViewById(R.id.SeverIpEditor);
		if (ServEditer != null) {
			ServEditer.setText(mBoundService.GetServerIp());
		}

		CheckBox SpkCheck = (CheckBox) layout.findViewById(R.id.LoudSpkCheck);
		if (SpkCheck != null) {
			SpkCheck.setChecked(PocEngine.spkIsOnMusic);
		}
		

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(messge)
				.setCancelable(false)
				.setView(layout)
				.setPositiveButton(
						mBoundService.GetString(ResString.EStirngButtonOK), ok)
				.setNegativeButton(
						mBoundService.GetString(ResString.EStirngButtonCancel),
						cancel);
		builder.create().show();
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		if (ViewStatus == LOGIN_STATUS) {
			mBoundService.CancelReLogin();
			menu.add(0, ResString.EStirngMenuLogin, 0,
					mBoundService.GetString(ResString.EStirngMenuLogin));
			menu.add(0, ResString.EStirngMenuSetting, 0,
					mBoundService.GetString(ResString.EStirngMenuSetting));
			menu.add(0, ResString.EStirngMenuExit, 0,
			        mBoundService.GetString(ResString.EStirngMenuExit));
		} else if (ViewStatus == LOGINING_STATUS) {
			mBoundService.CancelReLogin();
			menu.add(0, ResString.EStirngMenuCancel, 0,
					mBoundService.GetString(ResString.EStirngMenuCancel));
		} else if (ViewStatus == LOGOUTING_STATUS) {
			mBoundService.CancelReLogin();
			menu.add(0, ResString.EStirngMenuExit, 0,
					mBoundService.GetString(ResString.EStirngMenuExit));
		} else if (ViewStatus == USERLIST_STATUS) {
			return CreateUserListMenu(menu);
		}
		return true;
	}
	

	/* Handles item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		if (ViewStatus == USERLIST_STATUS) {
			return OnUserListMenuItemSelected(item);
		}
		switch (item.getItemId()) {
		case ResString.EStirngMenuLogin:
			OnLogin();
			return true;
		case ResString.EStirngMenuSetting:
			OnSetting();
			return true;
		case ResString.EStirngMenuCancel:
			OnCancelLogin();
			return true;
		case ResString.EStirngMenuExit:
			OnExit();
			return true;
		}
		return false;
	}

	class LoginClickEvent implements View.OnClickListener {
		public void onClick(View v) {
			if (v == btnLogin) {
				
				OnLogin();
			}
		}
	}

	class CancelClickEvent implements View.OnClickListener {
		public void onClick(View v) {
			if (v == btnCancelLogin) {
				OnCancelLogin();
			}
		}
	}

	private boolean AutoLogin = false;
	private boolean ConfigAutoLogin = false;

	public void ConstructLoginView() {
	
		if (ViewStatus == LOGIN_STATUS) {
			return;
		}
		ViewStatus = LOGIN_STATUS;
		setContentView(R.layout.main);

		btnLogin = (Button) this.findViewById(R.id.LoginBtn);
		AccountEditer = (EditText) this.findViewById(R.id.AccountEditer);
		PwdEditer = (EditText) this.findViewById(R.id.PasswordEditer);
		RmbPwdCheck = (CheckBox) this.findViewById(R.id.RmbPwdChecker);
		AutoLoginCheck = (CheckBox) this.findViewById(R.id.AutoLoginChecker);
		Information = (TextView) this.findViewById(R.id.InformationLabel);
		Information.setText("");
		String account = mBoundService.GetAccount();
		if (account != null) {
			AccountEditer.setText(account);
		}
		boolean rmbpwd = mBoundService.GetRmbPwd();
		boolean autologin = mBoundService.GetAutoLogin();
		String password = mBoundService.GetPassword();
		boolean rmbcheck = false;
		boolean autocheck = false;
		if (rmbpwd) {
			if (password != null) {
				if (password.length() > 0) {
					PwdEditer.setText(default_pwd);
					rmbcheck = true;
					if (autologin) {
						autocheck = true;
					}
				}

			}
		}
		RmbPwdCheck.setChecked(rmbcheck);
		AutoLoginCheck.setChecked(autocheck);
		btnLogin.setOnClickListener(new LoginClickEvent());
		if ((autologin && ConfigAutoLogin) || AutoLogin) {
			AutoLogin = false;
			ConfigAutoLogin = false;
			Message msg = handler.obtainMessage(Methods.UI_Login);
			handler.sendMessage(msg);
		}
		showGroupList = true;
        
		//showSpker(null);
	}

	public void ConstructLoginingView() {

		if (ViewStatus == LOGINING_STATUS) {
			return;
		}
		setContentView(R.layout.login);
		ViewStatus = LOGINING_STATUS;
		btnCancelLogin = (Button) this.findViewById(R.id.CancelLogin);
		btnCancelLogin.setOnClickListener(new CancelClickEvent());
		showSpker(null);
		showInfo(null);
	}

	public void ConstructLogoutingView() {
	
		if (ViewStatus == LOGOUTING_STATUS) {
			return;
		}
		setContentView(R.layout.logout);
		ViewStatus = LOGOUTING_STATUS;
		showSpker(null);
		showInfo(null);
	}

	public void ConstructUserListView() {

		Log.i("poc", "ConstructUserListView");

		if (ViewStatus == USERLIST_STATUS) {
			return;
		}

		
		
		setContentView(R.layout.userlist);
		ViewStatus = USERLIST_STATUS;
		userList = (ListView) this.findViewById(R.id.UserListView);
		userList.setAdapter(new UserListAdapter(this));
		//userList.setBackgroundColor(Color.BLACK);
		//userList.set
		// userList.setOnTouchListener(new UserListTounchHandler());
		statusPane = (TextView) this.findViewById(R.id.contextPane);
		groupPane = (TextView) this.findViewById(R.id.GroupTitlePane);
		titlePane = (TextView) this.findViewById(R.id.groupTitle);
		//spkPane = (TextView) this.findViewById(R.id.InfoLabel);
		InviteBtn = (Button) this.findViewById(R.id.InviteTmpGroup);
		SpkBtn = (Button) this.findViewById(R.id.SpkBtn);
		BackBtn = (Button) this.findViewById(R.id.BackToIndexBtn);
		
		UpdateTitle();
		UpdateGroupTitle();
		
		InviteBtn.setFocusable(false);
		InviteBtn.setOnTouchListener(new InviteBtnTounchHandler());
		
		SpkBtn.setFocusable(false);
		SpkBtn.setOnTouchListener(new SpkBtnTounchHandler());

		BackBtn.setFocusable(false);
		BackBtn.setOnTouchListener(new BackBtnTounchHandler());
		
		
		if (showGroupList) {
			BackBtn.setVisibility(BackBtn.INVISIBLE);
		}
		
		int iState = mBoundService.GetCurrentState();
		if (iState == EMarkOnlineUser || iState == EMarkSessionUser) {
			InviteBtn.setVisibility(BackBtn.VISIBLE);
		}else{
			InviteBtn.setVisibility(BackBtn.INVISIBLE);
		}

	}

	public class InviteBtnTounchHandler implements View.OnTouchListener {
		public boolean onTouch(View v, MotionEvent event) {
			if (v == InviteBtn) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					MainView.this.OnInviteTmpGroup();
					InviteBtn.setVisibility(BackBtn.INVISIBLE);
				}
				return true;
			}
			return false;
		}
	}
	
	public class BackBtnTounchHandler implements View.OnTouchListener {
		public boolean onTouch(View v, MotionEvent event) {
			if (v == BackBtn && (!showGroupList)) {
				if (event.getAction() == MotionEvent.ACTION_UP) {
					//if(mBoundService.HasTmpGroup()){
					//	mBoundService.LeaveGroup();
					//}else{
						MainView.this.OnToGroupIndex();
					//}
					//InviteBtn.setVisibility(BackBtn.INVISIBLE);
				}/*
				 * else if(event.getAction() == MotionEvent.ACTION_UP){
				 * OnEndPtt(); }
				 */
				return true;
			}
			return false;
		}
	}
    

	void showInfo(String info){
		if(InfoPopWindow == null){
			return;
		}
		if(info == null || info.equals("")){
			if(InfoPopWindow.isShowing()){
				InfoPopWindow.dismiss();
			}
		}else{
			TextView titleView = (TextView)InfoPopView.findViewById(R.id.textNote);
		    titleView.setText(info);
		    InfoPopWindow.setBackgroundDrawable(new BitmapDrawable());
		    InfoPopWindow.setOutsideTouchable(true);
			InfoPopWindow.showAtLocation(SpkBtn, Gravity.CENTER, 0, 60);	
		} 
	}

	
	void showSpker(String spker){
		if(SpkPopWindow == null){
			return;
		}
		if(spker == null || spker.equals("")){
			if(SpkPopWindow.isShowing()){
				SpkPopWindow.dismiss();
			}
		}else{
			TextView titleView = (TextView)SpkPopView.findViewById(R.id.textNote);
			titleView.setText(spker);
			SpkPopWindow.setOutsideTouchable(true); 
			SpkPopWindow.setFocusable(false); 
			SpkPopWindow.showAtLocation(SpkBtn, Gravity.CENTER, 0, 0);	
		}
	
	}

	public class SpkBtnTounchHandler implements View.OnTouchListener {
		public boolean onTouch(View v, MotionEvent event) {
			if (v == SpkBtn) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					OnStartPtt();
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					OnEndPtt();
				}
				return true;
			}

			return false;
		}
	}

	/*
	 * public class UserListTounchHandler implements View.OnTouchListener {
	 * private float startX = 0, endX; public boolean onTouch (View v,
	 * MotionEvent event){ if(v == userList ){ if(event.getAction() ==
	 * MotionEvent.ACTION_DOWN){ startX = event.getRawX() ; }else
	 * if(event.getAction() == MotionEvent.ACTION_UP){ endX = event.getRawX();
	 * if( startX - endX > 50){ MainView.this.OnToGroupIndex(); } } return true;
	 * } return false; } }
	 */
	private boolean ptt_start = false;

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			if (ViewStatus == USERLIST_STATUS) {
				if (!ptt_start) {
					ptt_start = true;
					OnStartPtt();
				}
				return true;
			}
		}
		

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (showGroupList) {
				backHome();
			} else {
				OnToGroupIndex();
			}
			return true;
		}
		return false;
	}

	void backHome() {
		PackageManager pm = getPackageManager();
		ResolveInfo homeInfo = pm.resolveActivity(
				new Intent(Intent.ACTION_MAIN)
						.addCategory(Intent.CATEGORY_HOME), 0);
		ActivityInfo ai = homeInfo.activityInfo;
		Intent startIntent = new Intent(Intent.ACTION_MAIN);
		startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		startIntent.setComponent(new ComponentName(ai.packageName, ai.name));
		startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			startActivity(startIntent);
		} catch (Exception e) {
			Log.i("poc", e.getMessage());
		}

	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN	) {
			if (ViewStatus == USERLIST_STATUS) {
				if (ptt_start) {
					ptt_start = false;
					OnEndPtt();
				}
				return true;
			}
		}
		

		

		if (keyCode == KeyEvent.KEYCODE_BACK) {

			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
			// this.OnLeftKey();
			this.OnToGroupIndex();
			return true;
		}
		// if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
		// this.OnRightKey();
		// return true;
		// }
		return false;

	}

	/*
	 * private boolean NetisAvailable() { ConnectivityManager connManager =
	 * (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE); NetworkInfo
	 * networkInfo = connManager.getActiveNetworkInfo(); if(networkInfo ==
	 * null){ return false; } return networkInfo.isAvailable(); }
	 * 
	 * public void NetConnectionStateChanged(int state){ switch(state){ case
	 * TelephonyManager.DATA_CONNECTED: if(ViewStatus == LOGIN_STATUS){
	 * if(BuildNetDlg != null){ if(BuildNetDlg.isShowing()){
	 * BuildNetDlg.dismiss(); BuildNetDlg = null; } } ActualLogin(); } break;
	 * case TelephonyManager.DATA_DISCONNECTED: break; } }
	 */

	// private ProgressDialog BuildNetDlg = null;

	/*
	 * private void NetBuild() { ConnectivityManager connManager =
	 * (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE); if(-1 ==
	 * connManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
	 * "internet")){ showNote("没有可以使用的网络。"); return; } BuildNetDlg =
	 * ProgressDialog.show(this, "", "正在建立网络.请等待...", true, true); }
	 */
	

	public void OnLogin() {
		if(SaveParams() < 0){
			return;
		}
		ActualLogin();
		//mBoundService.OpenLocation(true);
	}

	public int SaveParams() {
		String account = AccountEditer.getText().toString();
		String pwd = PwdEditer.getText().toString();
		boolean rmbCheck = RmbPwdCheck.isChecked();
		boolean autoCheck = AutoLoginCheck.isChecked();
		if(account == null || pwd == null){
			return -1;
		}
		if (account.length() < 1 || pwd.length() < 1) {
			return -1;
		}

		if (!pwd.equals(default_pwd)) {
			mBoundService.SetPassword(pwd);
		}

		mBoundService.SetAccount(account);

		mBoundService.SetRmbPwd(rmbCheck);
		mBoundService.SetAutoLogin(autoCheck);
		mBoundService.SaveConfig();
		return 0;
	}
    int 	LoginType = 1;
	public void ActualLogin() {
		Log.i("poc", "ActualLogin()");
		mBoundService.SetLoginType(LoginType); //设置登陆模式，0，为一步登陆，数据通过UDP下发；
		ConstructLoginingView();
		mBoundService.Login();
	}

	void OnCancelLogin() {
		// mBoundService.CancelReLogin();
		ConstructLoginView();
		mBoundService.CancelLogin();
	}

	public class SettingDlgOk extends DlgClickListener {
		public void onClick(DialogInterface dialog, int id) {
			EditText ServEditer = (EditText) ((Dialog) dialog)
					.findViewById(R.id.SeverIpEditor);
			if (ServEditer != null) {
				mBoundService.SetServerIp(ServEditer.getText().toString());
			}
			
		
			CheckBox SpkCheck = (CheckBox) ((Dialog) dialog)
					.findViewById(R.id.LoudSpkCheck);
			if (SpkCheck != null) {
				PocEngine.spkIsOnMusic = SpkCheck.isChecked();
			}
			SharedPreferences settings = getSharedPreferences(PocSettingFile, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("SpkIsOnMusic", PocEngine.spkIsOnMusic);
			mBoundService.SetSpkMode(PocEngine.spkIsOnMusic);
		}
	}

	public void OnSetting() {
		// this.showTextQuery(mBoundService.GetString(ResString.EStirngPromptInputServerIp),
		// mBoundService.GetServerIp(),
		// null, new SettingDlgOk());
		this.showSetting("设置", null, new SettingDlgOk());
	}

	public void ShowInformation(String aLabel) {
		if (ViewStatus == LOGIN_STATUS) {
			Information.setText(aLabel);
			//mVibrator.vibrate(new long[] { 600, 50, 600, 50 }, -1);
		}
	}

	
	
	private void OnExit() {		
        stopService(new Intent(this, PocService.class));
        Log.i("poc", "stopService()");
        mBoundService.Close();
		android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0); 
	}

	// /////////////////////////////////////////////////////////
	// userlist adapter
	// /////////////////////////////////////////////////////////
	public boolean CreateUserListMenu(Menu menu) {

		int aPrivi = mBoundService.GetUserPrivilege();
		boolean InGroup = mBoundService.IsInGroupList();
		boolean TmpGroup = mBoundService.HasTmpGroup();
		if (showGroupList) {
			if (InGroup
					&& (((aPrivi & PRIVILEGE_CHANGE_GROUP) != 0) || TmpGroup)) {
				menu.add(0, ResString.EStirngMenuOutSession, 0, mBoundService
						.GetString(ResString.EStirngMenuOutSession));
			}
			
			if ((aPrivi & PRIVILEGE_MODIFY_NAME) != 0) {
				menu.add(0, ResString.EStirngMenuUserModifyName, 0,
						mBoundService
								.GetString(ResString.EStirngMenuUserModifyName));
			}
			menu.add(0, ResString.EStirngMenuUserSetPwd, 0,
					mBoundService.GetString(ResString.EStirngMenuUserSetPwd));
			menu.add(0, ResString.EStirngMenuLogout, 0,
					mBoundService.GetString(ResString.EStirngMenuLogout));

	
			return true;
		}

		int iState = mBoundService.GetCurrentState();

		boolean IsBuddyList = mBoundService.IsCurrentBuddyList();

		boolean InCurrentGroup = mBoundService.IsInCurrentGroupList();
		
		boolean IsMonitor = mBoundService.IsMonitorCurrentGroup();

		if (IsBuddyList) {
			if (iState == ENoMarkItem) {
				if ((aPrivi & PRIVILEGE_BUDDY) != 0) {
					menu.add(0, ResString.EStirngMenuAddBuddy, 0, mBoundService
							.GetString(ResString.EStirngMenuAddBuddy));
				}
				if (InGroup
						&& (((aPrivi & PRIVILEGE_CHANGE_GROUP) != 0) || TmpGroup)) {
					menu.add(0, ResString.EStirngMenuOutSession, 0,
							mBoundService
									.GetString(ResString.EStirngMenuOutSession));
				}
			} else if (iState == EMarkOfflineUser
					|| iState == EMarkOffOnlineUser) {
				if ((aPrivi & PRIVILEGE_BUDDY) != 0) {
					menu.add(
							0,
							ResString.EStirngMenuRemoveBuddy,
							0,
							mBoundService
									.GetString(ResString.EStirngMenuRemoveBuddy));
				}
			} else if (iState == EMarkOnlineUser || iState == EMarkSessionUser) {
				if ((aPrivi & PRIVILEGE_INVITE_TMPGROUP) != 0) {
					menu.add(
							0,
							ResString.EStirngMenuInviteTmpSession,
							0,
							mBoundService
									.GetString(ResString.EStirngMenuInviteTmpSession));
				}
			}
		} else {
			boolean IsInTmpGroup = mBoundService.IsCurrentTmpGroup();
			if (iState == ENoMarkItem) {
				if (!InCurrentGroup
						&& ((aPrivi & PRIVILEGE_INVITE_TMPGROUP) != 0)) {
					menu.add(
							0,
							ResString.EStirngMenuEnterSession,
							0,
							mBoundService
									.GetString(ResString.EStirngMenuEnterSession));
				}
				if (InGroup
						&& (((aPrivi & PRIVILEGE_CHANGE_GROUP) != 0) || TmpGroup)) {
					menu.add(0, ResString.EStirngMenuOutSession, 0,
							mBoundService
									.GetString(ResString.EStirngMenuOutSession));
				}

				if (!IsInTmpGroup && ((aPrivi & PRIVILEGE_MONITOR_GROUP) != 0)) {
					if (IsMonitor) {
						menu.add(
								0,
								ResString.EStirngMenuRemoveMonitorGroup,
								0,
								mBoundService
										.GetString(ResString.EStirngMenuRemoveMonitorGroup));
					} else {
						menu.add(
								0,
								ResString.EStirngMenuAddMonitorGroup,
								0,
								mBoundService
										.GetString(ResString.EStirngMenuAddMonitorGroup));
					}
				}
			} else if (iState == EMarkOfflineUser
					|| iState == EMarkOffOnlineUser) {
				if ((aPrivi & PRIVILEGE_GROUP_EDIT) != 0 && (!IsInTmpGroup)) {
					menu.add(
							0,
							ResString.EStirngMenuGroupRemoveUser,
							0,
							mBoundService
									.GetString(ResString.EStirngMenuGroupRemoveUser));

				}

				if ((aPrivi & PRIVILEGE_BUDDY) != 0) {
					menu.add(
							0,
							ResString.EStirngMenuAddUserToBuddy,
							0,
							mBoundService
									.GetString(ResString.EStirngMenuAddUserToBuddy));
				}
		
			} else if (iState == EMarkOnlineUser || iState == EMarkSessionUser) {

				if ((aPrivi & PRIVILEGE_INVITE_TMPGROUP) != 0) {
					menu.add(
							0,
							ResString.EStirngMenuInviteTmpSession,
							0,
							mBoundService
									.GetString(ResString.EStirngMenuInviteTmpSession));
				}


				if ((aPrivi & PRIVILEGE_BUDDY) != 0) {
					menu.add(
							0,
							ResString.EStirngMenuAddUserToBuddy,
							0,
							mBoundService
									.GetString(ResString.EStirngMenuAddUserToBuddy));
				}
			}
		}

		if ((aPrivi & PRIVILEGE_MODIFY_NAME) != 0) {
			menu.add(0, ResString.EStirngMenuUserModifyName, 0, mBoundService
					.GetString(ResString.EStirngMenuUserModifyName));
		}
		menu.add(0, ResString.EStirngMenuUserSetPwd, 0,
				mBoundService.GetString(ResString.EStirngMenuUserSetPwd));
		menu.add(0, ResString.EStirngMenuLogout, 0,
				mBoundService.GetString(ResString.EStirngMenuLogout));

		return true;
	}

	public boolean OnUserListMenuItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case ResString.EStirngMenuAddBuddy: {
			OnAddBuddyUser();
			return true;
		}
		case ResString.EStirngMenuRemoveBuddy: {
			OnRemoveBuddyUser();
			return true;
		}
		case ResString.EStirngMenuEnterSession: {
			OnEnterGroup();
			return true;
		}
		case ResString.EStirngMenuOutSession: {
			OnLeaveGroup();
			return true;
		}
		case ResString.EStirngMenuInviteTmpSession: {
			OnInviteTmpGroup();
			return true;
		}
		case ResString.EStirngMenuAddUserToBuddy: {
			OnAddUserToBuddy();
			return true;
		}

		case ResString.EStirngMenuUserSetPwd: {
			OnModifyPassword();
			return true;
		}
		case ResString.EStirngMenuLogout: {
			OnLogout();
			return true;
		}

		
		case ResString.EStirngMenuAddMonitorGroup: {
			OnAddMonitorGroup();
			return true;
		}
		case ResString.EStirngMenuRemoveMonitorGroup: {
			OnRemoveMonitorGroup();
			return true;
		}
		default: {
			return false;
		}
		}
		// return false;
	}

	// ///////////////////////////////////////////////////
	public void OnStartPtt() {
		mBoundService.StartPTT();
	}

	public void OnEndPtt() {
		mBoundService.EndPTT();
	}

	public void UpdateTitle() {
		if (ViewStatus == USERLIST_STATUS) {
			String activeGroup = mBoundService.GetActivateGroupTitle();
			String name = mBoundService.GetName();
			if (activeGroup != null && activeGroup.length() > 0) {
				statusPane.setText(this.getString(R.string.username) + name);
				groupPane.setText( this.getString(R.string.ingroup) + activeGroup);
			} else {
				statusPane.setText(this.getString(R.string.username) + name);
				groupPane.setText( this.getString(R.string.noingroup));
			}
		}
	}
	
	public void UpdateGroupTitle() {
		if (ViewStatus == USERLIST_STATUS) {
			String Group = mBoundService.GetCurrentGroupTitle();
			boolean IsMonitor = mBoundService.IsMonitorCurrentGroup();
			if(IsMonitor && (!showGroupList)){
				titlePane.setTextColor(Color.DKGRAY);
			}else{
				titlePane.setTextColor(Color.GRAY);
			}
			titlePane.setText(Group);
		}
	}

	public void UpdateUserList() {
		if (ViewStatus == USERLIST_STATUS) {
			Log.i("poc", "UpdateUserList");
			userList.invalidateViews();
			UpdateGroupTitle();
		}
	}
	
	public void SetTalker(String aTalker, boolean aBtnable) {
		if (ViewStatus == USERLIST_STATUS) {
			if (aTalker == null) {
				showSpker(aTalker);
				//spkPane.setText("");
			} else {
				showSpker(aTalker + SPK_LABEL);
				/*if (aBtnable) {
					spkPane.setText(aTalker + SPK_LABEL);
				} else {
					spkPane.setText(aTalker + SPK_LABEL);
				}*/
			}
		}
	}

	public void SetTalkInfo(int aStrId) {
		if (ViewStatus == USERLIST_STATUS) {
			if(aStrId == ResString.EStirngInviteTmpGroupFail){
				showInfo(this.getString(R.string.callbusy));
				//spkPane.setText(this.getString(R.string.callbusy));	
			}else{
				showInfo(mBoundService.GetString(aStrId));
				//spkPane.setText(mBoundService.GetString(aStrId));
			}
			
		}
	}

	public void OnToGroup(int aIndex) {
		mBoundService.ToGroup(aIndex);
		showGroupList = false;
		UpdateUserList();
		
		BackBtn.setVisibility(View.VISIBLE);

		
		//if (aIndex > 0) {
			// public native int CanEnterGroup(int aIndex);
			// public native void EnterGroupIdx(int aIndex);
			// mBoundService.EnterGroupIdx(aIndex);
			// mBoundService.EnterGroup();
		//}
		
		mBoundService.EnterGroupIdx(aIndex);
		mBoundService.ResetCurrentGroupMark();
		InviteBtn.setVisibility(BackBtn.INVISIBLE);
	}

	public void OnToGroupIndex() {
		if(mBoundService.HasTmpGroup()){
			OnLeaveGroup();
			
			//showGroupList = true;
			//UpdateUserList();
			//BackBtn.setVisibility(View.INVISIBLE);
			//InviteBtn.setVisibility(BackBtn.INVISIBLE);
			return;
		}
		mBoundService.ToGroupIndex();
		showGroupList = true;
		UpdateUserList();
		BackBtn.setVisibility(View.INVISIBLE);
		InviteBtn.setVisibility(BackBtn.INVISIBLE);
	}

	/*
	 * public void OnLeftKey(){ //mBoundService.NextGroupOfMgr();
	 * 
	 * UpdateUserList(); } public void OnRightKey(){
	 * //mBoundService.PreGroupOfMgr(); UpdateUserList(); }
	 */

	public class AddBuddyUserDlgOk extends DlgClickListener {
		public void onClick(DialogInterface dialog, int id) {
			EditText edit = (EditText) ((Dialog) dialog).findViewById(1);
			if (edit != null) {
				mBoundService.AddBuddyUser(edit.getText().toString());
			}
		}
	}

	public void OnAddBuddyUser() {
		showTextQuery(
				mBoundService
						.GetString(ResString.EStirngPromptInputUserAccount),
				"", null, new AddBuddyUserDlgOk());
	}

	public void OnAddUserToBuddy() {
		mBoundService.AddBuddyUser();
		mBoundService.ResetCurrentGroupMark();
		UpdateUserList();
	}

	public void OnRemoveBuddyUser() {
		mBoundService.RemoveBuddyUser();
		UpdateUserList();
	}
	
	

	public class ModifyPwdDlgOk extends DlgClickListener {
		public void onClick(DialogInterface dialog, int id) {
			EditText edit1 = (EditText) ((Dialog) dialog)
					.findViewById(R.id.Editer1);
			EditText edit2 = (EditText) ((Dialog) dialog)
					.findViewById(R.id.Editer2);
			if (edit1 != null && edit2 != null) {
				int res = mBoundService.ModifyPassword(edit1.getText().toString(), edit2.getText().toString());
			    if(res == -2){
			    	showInfo(mBoundService.GetString(ResString.EStringErrorOldPwdError));
			    }else if(res != 0){
			    	showInfo(mBoundService.GetString(ResString.EStirngModifyPasswordFailed));
			    }
			}
		}
	}

	public void OnModifyPassword() {
		showText2Query(
				mBoundService.GetString(ResString.EStirngMenuUserSetPwd), null,
				new ModifyPwdDlgOk());
	}

	public void OnAddMonitorGroup() {
		mBoundService.AddMonitorGroup();
	}

	public void OnRemoveMonitorGroup() {
		mBoundService.RemoveMonitorGroup();
	}

	public void OnEnterGroup() {
		mBoundService.EnterGroup();
	}

	public void OnLeaveGroup() {
		mBoundService.LeaveGroup();
	}

	public void OnInviteTmpGroup() {
		mBoundService.InviteTmpGroup();
		mBoundService.ResetCurrentGroupMark();
		UpdateUserList();
	}

	public void OnLogout() {
		if (ViewStatus == USERLIST_STATUS) {
			ConstructLogoutingView();
			mBoundService.LogOut();
		}
	}




	// ////////////////////////////////////////////////////
	private class UserListAdapter extends BaseAdapter {
		private Context mContext;
		
		public UserListAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return mBoundService.GetUserListCount();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			UserItemView sv;
			UserItem usrItem = mBoundService.GetUserListItem(position);

			if (convertView == null) {
				if (usrItem == null) {
					sv = new UserItemView(mContext, position, -1, "", false);
				} else {
					sv = new UserItemView(mContext, position, usrItem.Status, usrItem.Name, usrItem.Checked);
				}
			} else {
				sv = (UserItemView) convertView;
				if (usrItem != null) {
				    sv.mChecker.setChecked(usrItem.Checked);
				    sv.mIndex = position;
				    sv.mTitle.setText(usrItem.Name);
				    sv.mStatus = usrItem.Status;
				    if(mBoundService.IsGroupIndex() == 0){
				    	if (usrItem.Status == 3) {
							sv.mHeader.setImageResource(R.drawable.status_insession);
						} else if (usrItem.Status == 2) {
							sv.mHeader.setImageResource(R.drawable.status_online);
						} else {
							sv.mHeader.setImageResource(R.drawable.status_offline);
						}
				    	if(mBoundService.IsCurrentTmpGroup() ){
							sv.mChecker.setVisibility(View.INVISIBLE);
						} else {
							sv.mChecker.setVisibility(View.VISIBLE);
						}
				        sv.mTitle.setTextColor(Color.LTGRAY);
					
				    }else{
					    sv.mHeader.setImageResource(R.drawable.group);
					    if(sv.mStatus == 4){
						    sv.mTitle.setTextColor(Color.LTGRAY);
					    }else{
						    sv.mTitle.setTextColor(Color.BLUE);
					    }
				    }
				    sv.mHeader.invalidate();
				}				
			}
			return sv;
			
		}
	}
				
	private class UserItemView extends RelativeLayout {
		public UserItemView(Context context, int index, int status,
				String title, boolean selected) {
			super(context);
			// this.setOrientation(HORIZONTAL);
			mIndex = index;
			mHeader = new ImageView(context);
			mTitle = new TextView(context);
			mChecker = new CheckBox(context);

			if (status == 3) {
				mHeader.setImageResource(R.drawable.status_insession);
			} else if (status == 2) {
				mHeader.setImageResource(R.drawable.status_online);
			} else {
				mHeader.setImageResource(R.drawable.status_offline);
			}
			mHeader.invalidate();

			mHeader.setAdjustViewBounds(true);
			mHeader.setId(1);
			mTitle.setId(2);
			mChecker.setId(3);

			mTitle.setText(title);
			mChecker.setChecked(selected);

			mChecker.setClickable(false);

			RelativeLayout.LayoutParams lparas1 = new RelativeLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lparas1.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			lparas1.addRule(RelativeLayout.CENTER_VERTICAL);
			lparas1.leftMargin = 10;

			RelativeLayout.LayoutParams lparas2 = new RelativeLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lparas2.addRule(RelativeLayout.RIGHT_OF, 1);
			lparas2.addRule(RelativeLayout.CENTER_VERTICAL);
			mTitle.setTextSize(22);

			RelativeLayout.LayoutParams lparas3 = new RelativeLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lparas3.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			lparas3.rightMargin = 10;
			setBackgroundColor(Color.WHITE);
			mHeader.setBackgroundColor(Color.WHITE);
			mTitle.setBackgroundColor(Color.WHITE);
			mTitle.setTextColor(Color.GRAY);
			mChecker.setBackgroundColor(Color.WHITE);
			addView(mHeader, lparas1);
			addView(mTitle, lparas2);
			addView(mChecker, lparas3);

		}

		public boolean onTouchEvent(MotionEvent event) {
			int act = event.getAction();
			if (act == MotionEvent.ACTION_UP) {

				if (MainView.this.showGroupList) {
					
					MainView.this.OnToGroup(mIndex);
				} else {
					boolean check = mChecker.isChecked();
					mChecker.setChecked(!check);
					mBoundService.SetUserListItemMask(mIndex, !check);
					int iState = mBoundService.GetCurrentState();
					if (iState == EMarkOnlineUser || iState == EMarkSessionUser) {
						InviteBtn.setVisibility(BackBtn.VISIBLE);
					}else{
						InviteBtn.setVisibility(BackBtn.INVISIBLE);
					}
				}
			}
			return true;
		}

		private ImageView mHeader;
		private TextView mTitle;
		private CheckBox mChecker;
		private int mStatus;
		private int mIndex;

	}

	// ///////////////////////////////////////////////////////
	private PocService mBoundService;
	private boolean mIsBound = false;

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundService = ((PocService.PocBinder) service).getService();
			mBoundService.setMainView(MainView.this);
			int state = mBoundService.GetClientState();
			ConstructView(state);
		}

		public void onServiceDisconnected(ComponentName className) {
			mBoundService.setMainView(null);
			mBoundService = null;
		}
	};

	void doBindService() {
		// startService(new Intent(this, PocService.class));
		if (!mIsBound) {
			bindService(new Intent(this, PocService.class), mConnection,
					Context.BIND_AUTO_CREATE);
			mIsBound = true;
		}
	}

	void doUnbindService() {
		if (mIsBound) {
			unbindService(mConnection);
			mIsBound = false;
		}
	}
	

	// ////////////////////////////////////////////////////////////////////////////
	public Handler handler() {
		return handler;
	}

	public void handleMessage_inner(Message msg) {
        
		if(msg.what == 102){
			ShowLocationSetting();
			return;
		}
		
		Method method = MethodArray.elementAt(msg.what);
		if (method == null) {
			return;
		}
		Log.i("poc", "handleMessage_inner" + msg.what + method.getName());
		try {
			method.invoke(this, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ////////////////////////////////////////////////////////////////////////////
	public void UI_UpdateUserList(Message msg) {
		UpdateUserList();
	}

	public void UI_SetTalker(Message msg) {
		boolean btn = (msg.arg1 == 0) ? false : true;
		SetTalker((String) msg.obj, btn);
	}

	public void UI_SetTalkInfo(Message msg) {
		Log.i("poc", "UI_SetTalkInfo:");
		SetTalkInfo(msg.arg1);
	}


	public void UI_ShowUserListView(Message msg) {
		PocEngine.isOffLine = false;
		ConstructUserListView();
		UpdateGroupTitle();
		mBoundService.OpenLocation();
		if(LoginType == 1){
			mBoundService.UpdateUserData();
		}
	}

	public void UI_ShowLogoutView(Message msg) {
		ConstructLogoutingView();
	}

	public void UI_RetryLogin(Message msg) {
		ConstructLoginView();
	}

	public void UI_ShowError(Message msg) {
		showNote(mBoundService.GetString(msg.arg1));
	}

	public void UI_ShowNote(Message msg) {
		showNote(mBoundService.GetString(msg.arg1));
	}

	public void UI_UpdateTitle(Message msg) {
		UpdateTitle();
	}


	public void UI_Login(Message msg) {
		if (!PocEngine.isOffLine) {
			mBoundService.CancelReLogin();
		}
		ActualLogin();
	}

	public void UI_ShowLoginInformation(Message msg) {
		ShowInformation(mBoundService.GetString(msg.arg1));
	}

	public class UpdateSoftwareDlgOk extends DlgClickListener {

		public String mUrl;

		public UpdateSoftwareDlgOk(String url) {
			mUrl = url;
		}

		public void onClick(DialogInterface dialog, int id) {
			Uri uriWebPage = Uri.parse(mUrl);
			Intent intent = new Intent(Intent.ACTION_VIEW, uriWebPage);
			startActivity(intent);
		}
	}

	public void UI_NotifyUpdateSoftWare(Message msg) {
		// Log.i("poc", "UI_NotifyUpdateSoftWare:" + (String)msg.obj);
		ConstructLoginView();
		mBoundService.CancelLogin();
		showQuery(mBoundService.GetString(ResString.EStirngUpdateVersion),
				null, new UpdateSoftwareDlgOk((String) msg.obj));
	}
	
	public void PostLocationSetting()
	{
		if (handler == null) {
			return;
		}
		Message msg = handler.obtainMessage(102);
		handler.sendMessage(msg);
	}
	

	public class SetLocationDlgOk extends DlgClickListener {
		public void onClick(DialogInterface dialog, int id) {
			Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);        
		    startActivityForResult(intent, 2); 
		}
	}
	
	public class SetLocationDlgCancel extends DlgClickListener {
		public void onClick(DialogInterface dialog, int id) {
			//mBoundService.mOpenLocationSetting = false;
			//mBoundService.SetLocationTime(0);
		}
	}	
	public void ShowLocationSetting()
	{
		showQuery(this.getString(R.string.opengps), new SetLocationDlgCancel(), new SetLocationDlgOk());
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == 2){
			Log.i("poc", "onActivityResult: " + resultCode);
			mBoundService.OpenLocation();
		}
	}
	
	AlertDialog telDlg = null;
	public void showCallPrompt(String note, DlgClickListener cancel, String btnLabel) {
		if(telDlg != null){
			telDlg.dismiss();
			telDlg = null;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(note)
				.setCancelable(false)
				.setNegativeButton(btnLabel, cancel);
		telDlg = builder.create();
		telDlg.show();
	}
	
	
	public void showCallQuery(String messge, DlgClickListener cancel,
			DlgClickListener ok) {
		if(telDlg != null){
			telDlg.dismiss();
			telDlg = null;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(messge)
				.setCancelable(false)
				.setPositiveButton(
						"接听", ok)
				.setNegativeButton(
						"挂断",
						cancel);
		telDlg = builder.create();
		telDlg.show();
	}
	
	public class VoIPTelDlgOk extends DlgClickListener {
		public void onClick(DialogInterface dialog, int id) {
			mBoundService.StopPlayRing();
			mBoundService.AcceptIpTel();
		    mBoundService.SetSpkMode(false);
		    telDlg = null;
		}
	}
	public class VoIPTelDlgCancel extends DlgClickListener {
		public void onClick(DialogInterface dialog, int id) {
			mBoundService.StopPlayRing();
			mBoundService.RejustIpTel();
			telDlg = null;
		}
	}
	
	public void ShowCallingVoIP(String name){
		showCallPrompt("正在呼叫" + name + "...", new VoIPTelDlgCancel(), "取消");
	}

	public void  UI_NotifyVoIPCall(Message msg)
	{
		/*if(PocEngine.AutoHangup){
			mBoundService.AcceptIpTel();
		    mBoundService.SetSpkMode(false);
		}else{
			mBoundService.StartPlayRing();
			showCallQuery((String)msg.obj + "呼叫,是否接听?", new VoIPTelDlgCancel(),
					new VoIPTelDlgOk());
		}*/
		mBoundService.StartPlayRing();
		showCallQuery((String)msg.obj + "呼叫,是否接听?", new VoIPTelDlgCancel(),
				new VoIPTelDlgOk());

	}
	
	public  void  UI_NotifyVoIPVoice(Message msg)
	{
		if(telDlg != null){
			telDlg.dismiss();
			telDlg = null;
		}
		mBoundService.StopPlayRing();
		mBoundService.SetSpkMode(false);
		showCallPrompt("与" + (String)msg.obj + "通话中", new VoIPTelDlgCancel(), "挂断");
		
	}
	
	public void  UI_NotifyVoIPHangoff(Message msg)
	{
		mBoundService.SetSpkMode(true);
		if(telDlg != null){
			telDlg.dismiss();
			telDlg = null;
		}
		mBoundService.StopPlayRing();
	}
	public void  UI_NotifyVoIPCalling(Message msg)
	{
		Log.i("poc", "UI_NotifyVoIPCalling:" + (String)msg.obj);
		//ShowCallingVoIP((String)msg.obj);
		
	}
	public void  UI_NotifyVoIPRing(Message msg)
	{
		Log.i("poc", "UI_NotifyVoIPRing:" + (String)msg.obj);
		mBoundService.StartPlayRing();
	}
	public void  UI_SetVoIPBtnStatus(Message msg)
	{
		Log.i("poc", "UI_SetVoIPBtnStatus:" + msg.arg1 + "=" + msg.arg2);
	}

	public void POC_StartPtt(Message msg) {
		mBoundService.StartPTT();
	}

	public void POC_EndPtt(Message msg) {
		mBoundService.EndPTT();
	}
	//接收短信通知接口
	public void UI_NotifyMsg(Message msg){
		//String obj_string = (String)msg.obj;
		String from = mBoundService.GetUserName(msg.arg1);
		//showNote(from + ":" + (String)msg.obj);
		addReceiveMsg(from, msg.arg2, (String)msg.obj, msg.arg1);
		//Log.i("poc", "NotifyMsg:Id:" + msg.arg1 + ":");
	}
	
	public void UI_NotifyMsgAck(Message msg)
	{
		Log.i("poc", "NotifyMsgAck:Id:" + msg.arg1 + ", msgId:" + msg.arg2 + ", type:" + (String)msg.obj );
	}
	
	
	public class SmsDlgOk extends DlgClickListener {
		public long msgId;
		public long userId;
		
		public void onClick(DialogInterface dialog, int id) {
			Log.i("poc", "SendMsgAck:MsgId:" + msgId + ", Id:" + userId);

			mBoundService.SendMsgAck(msgId, userId, 1);
		}
	}
	
	
	
	public void showMsgNote(String note,  DlgClickListener ok) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(note)
				.setCancelable(false)
				.setPositiveButton(
						mBoundService.GetString(ResString.EStirngButtonOK),
						ok);
		builder.create().show();
	}
	
	//如何存下，方便以后看，需要你们实现
	void addReceiveMsg(String Id, long msgId, String msg, long fromId){
		//SharedPreferences settings = getSharedPreferences(ReceiveMsg, 0);
		//SharedPreferences.Editor editor = settings.edit();
		//editor.putString(Id, msg);
		ContentValues values = new ContentValues();
	    values.put("address", Id);
	    values.put("type", "1");
	    values.put("read", "0");
	    values.put("body", msg);
	    values.put("date", new Date().getTime());
	    //values.put("person", "test");
	    Uri uri = this.getApplicationContext().getContentResolver()
	     .insert(Uri.parse("content://sms/inbox"), values);
	    SmsDlgOk ok = new SmsDlgOk();
	    ok.msgId = msgId;
	    ok.userId = fromId;
	    showMsgNote(Id + ":" + msg, ok);
		playAlert();
	}
	
	
	void playAlert(){
		
		try {
		     MediaPlayer mp = new MediaPlayer();
		     mp.reset();  
		     mp.setDataSource(this,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		     mp.prepare();
		     mp.start();
		} catch (Exception e) {
		     //e.printStackTrace();
		} 

	}
	

	private void InitMethod() {
		try {
			Class paras[] = { Message.class };
			MethodArray.add(getClass().getMethod("UI_UpdateUserList", paras));
			MethodArray.add(getClass().getMethod("UI_SetTalker", paras));
			MethodArray.add(getClass().getMethod("UI_SetTalkInfo", paras));
			MethodArray.add(getClass().getMethod("UI_ShowUserListView", paras));
			MethodArray.add(getClass().getMethod("UI_ShowLogoutView", paras));
			MethodArray.add(getClass().getMethod("UI_RetryLogin", paras));
			MethodArray.add(getClass().getMethod("UI_ShowError", paras));
			MethodArray.add(getClass().getMethod("UI_ShowNote", paras));
			MethodArray.add(getClass().getMethod("UI_UpdateTitle", paras));
			MethodArray.add(getClass().getMethod("UI_Login", paras));
			MethodArray.add(getClass().getMethod("UI_ShowLoginInformation",
					paras));
			MethodArray.add(getClass().getMethod("UI_NotifyUpdateSoftWare",
					paras));
			MethodArray.add(getClass().getMethod("UI_NotifyVoIPCall", paras));
			MethodArray.add(getClass().getMethod("UI_NotifyVoIPVoice", paras));
			MethodArray.add(getClass().getMethod("UI_NotifyVoIPHangoff", paras));
			MethodArray.add(getClass().getMethod("UI_NotifyVoIPCalling", paras));
			MethodArray.add(getClass().getMethod("UI_NotifyVoIPRing", paras));
			MethodArray.add(getClass().getMethod("UI_SetVoIPBtnStatus", paras));
			
			MethodArray.add(getClass().getMethod("POC_StartPtt", paras));
			MethodArray.add(getClass().getMethod("POC_EndPtt", paras));
			
			MethodArray.add(getClass().getMethod("UI_NotifyMsg", paras));
			MethodArray.add(getClass().getMethod("UI_NotifyMsgAck", paras));

		} catch (Exception e) {
			Log.i("poc", e.getMessage());
		}
	}

}