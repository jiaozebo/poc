package com.corget;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {

	//public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
	public static final String HEADSET_EVENT = "android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT";
	//public static final String PTT_KEY_ACTION = "android.intent.action.PTT_KEY";
	//public static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
	public static int   pttFlags = 0;
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.i("poc", "receiver:" + action);
		if (HEADSET_EVENT.equals(action)) {
			Bundle bundle = intent.getExtras();
			if (bundle == null) {
				return;
			}
		    boolean bool1 = intent.getCategories().contains("android.bluetooth.headset.intent.category.companyid.85"); 
			if (bool1){
			    Object[] arrayOfObject = (Object[])(Object[])bundle.get("android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_ARGS");
			    if(arrayOfObject.length < 2){
			    	return;
			    }
			    if(!arrayOfObject[0].equals("TALK")){
			    	return;
			    }
			    int p = ((Integer)arrayOfObject[1]).intValue();
			    Log.i("poc", "receiver:" + p);
			    if(p == 1){
			    	Log.i("poc", "receiver:" + arrayOfObject[0] );	
			    	StartPTT(context, 1);
			    }else if(p == 0){
			    	Log.i("poc", "receiver:" + arrayOfObject[0]);
			    	StartPTT(context, 0);
			    }
			}   
			
		}
	}

	private void StartPoc(Context context) {
		Intent startIntent = new Intent(context, com.corget.MainView.class);
		startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startIntent.putExtra("login", true);
		context.startActivity(startIntent);
	}

	
	private void StartPTT(Context context, int flg) {
		if(flg == 1){
			/*if(!PocEngine.isOffLine){
				Log.i("poc", "start active:");
				Intent startIntent = new Intent(context, com.corget.MainView.class);
			    startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			    startIntent.putExtra("login", true);
			    context.startActivity(startIntent);
			}*/
			PocEngine.UI_StartPTT();
		}else{
			PocEngine.UI_EndPTT();
		}
	}
	
}
