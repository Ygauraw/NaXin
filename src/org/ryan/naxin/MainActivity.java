package org.ryan.naxin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends Activity {
	private static final String TAG = "NaXin";
	private static final String FileSavePath = "/mnt/sdcard";
	
	private boolean isRunning = true;
	public static MainActivity mInstance = null;
	private Person mMySelf = null;
	
	private ViewPager mViewPager = null;
	private RadioButton mBnChat = null;
	private RadioButton mBnAddress = null;
	private RadioButton mBnFriends = null;
	private RadioButton mBnSetting = null;
	
	
	private MyPagerAdapter mPageAdapter = null;
	private final ArrayList<View> mArrayList = new ArrayList<View>();
	
	private PopupWindow mPopupWindow = null;
	private boolean mPopupWindowShow = false;
	
	private ExpandableListView mExpandListView = null;
	private MyExListAdapter mExListAdapter = null;
	

	private SharedPreferences mSharedPreferences = null;
	private String userName = "";
	private int userImgId = 0;
	private int userID = 0;
	
	public ArrayList<Map<Integer,Person>> children = new ArrayList<Map<Integer,Person>>();
	public Map<Integer,Person> childrenMap = new HashMap<Integer,Person>();
	public ArrayList<Integer> personKeys = new ArrayList<Integer>();

	//------------------------------------------------------
	// ����ϵ�˽�����ʾ�������
	private TextView mWifiFailedText = null;
	
	//------------------------------------------------------
	// �ڡ��Ự��������ʾ���е������¼
	private String[] mMessageMapIndexStr = {"UserImg","UserName","Message","LastTime","GroupID"};
	private ListView mMessageListView = null;
	// ���ݿ����
	private DatabaseHelper mDatabaseHelper = null;
	private SimpleCursorAdapter mMessageSimpleAdapter = null;
	
	/*******************************************************/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);
		mInstance = this;
		initView();
	}

	private void initView(){
		mViewPager = (ViewPager)findViewById(R.id.main_viewpager);
		
		// װ��TAB VIEW
		LayoutInflater mInflater = LayoutInflater.from(this);
		mArrayList.add(mInflater.inflate(R.layout.chat_tab, null));
		mArrayList.add(mInflater.inflate(R.layout.address_tab, null));
		mArrayList.add(mInflater.inflate(R.layout.friends_tab, null));
		mArrayList.add(mInflater.inflate(R.layout.setting_tab, null));
		
		
		for(int i=0;i<4;i++){
			(mArrayList.get(i)).setOnTouchListener(new View.OnTouchListener() {
				// ���Pop windows������������
				public boolean onTouch(View v, MotionEvent event) {
					// TODO Auto-generated method stub
					if(mPopupWindowShow){
						mPopupWindow.dismiss();
						mPopupWindowShow = false;
					}
					return false;
				}
			});
		}
		
		//--------------���ý�����Ӧ��onTouch�¼�--------------------
		ScrollView mScrollView = (ScrollView) (mArrayList.get(3)
				.findViewById(R.id.setting_tab_scrollView));
		
		mScrollView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(mPopupWindowShow){
					mPopupWindow.dismiss();
					mPopupWindowShow = false;
				}
				return false;
			}
		});
		
		
		// -------------��ȡExpandableListViewʵ��-----------------
		mExListAdapter = new MyExListAdapter(this);
		mExpandListView = (ExpandableListView) (mArrayList.get(1)
								.findViewById(R.id.expandableListView_address));
		mExpandListView.setAdapter(mExListAdapter);
		mExpandListView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				if(mPopupWindowShow){
					mPopupWindow.dismiss();
					mPopupWindowShow = false;
				}
				return false;
			}
		});
		
		mExpandListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				// TODO Auto-generated method stub
				Intent mIntent = new Intent(MainActivity.this, ChatActivity.class);
				Person chatPerson = childrenMap.get(personKeys.get(childPosition));
				Bundle bd = new Bundle();
				bd.putSerializable(Constant.userNameStr, chatPerson);
				bd.putSerializable(Constant.myInfoStr, mMySelf);
				bd.putString(Constant.userNewMsgStr, "");
				mIntent.putExtras(bd);
				MainActivity.this.startActivity(mIntent);
				return false;
			}
		});
		
		
		// ����������
		mPageAdapter = new MyPagerAdapter();
		mViewPager.setAdapter(mPageAdapter);
		mViewPager.setCurrentItem(1); //Ĭ��������ϵ��ҳ��
		
		mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			public void onPageSelected(int arg0) {
				// TODO Auto-generated method stub
				switch(arg0){
					case 0:
						mBnChat.setChecked(true);
						updateTheMessageList();
						break;
						
					case 1:
						mBnAddress.setChecked(true);
						break;
					
					case 2:
						mBnFriends.setChecked(true);
						break;
					
					case 3:
						mBnSetting.setChecked(true);
						break;
				
				}
			}
			
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub
				
			}
			
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub
				
			}
		});
		
		
		// ��Radio�����¼�
		mBnChat = (RadioButton)findViewById(R.id.radioButton1);
		mBnAddress = (RadioButton)findViewById(R.id.radioButton2);
		mBnFriends = (RadioButton)findViewById(R.id.radioButton3);
		mBnSetting = (RadioButton)findViewById(R.id.radioButton4);
		mBnChat.setOnClickListener(new MyRaidoClickListener(0));
		mBnAddress.setOnClickListener(new MyRaidoClickListener(1));
		mBnFriends.setOnClickListener(new MyRaidoClickListener(2));
		mBnSetting.setOnClickListener(new MyRaidoClickListener(3));
		mBnAddress.setChecked(true);
		
		/*----------------��ø�����Ϣ----------------------------*/
		getMyInformation();
		// ������ҳ�������Լ�����Ϣ
		((TextView)((mArrayList.get(3).findViewById(R.id.setting_username)))).setText(userName);
		((ImageView)((mArrayList.get(3).findViewById(R.id.setting_userimg))))
										.setImageResource(Constant.mThumbIds[userImgId]);
		
		
		
		/*----------------��ʾ���еĶԻ��б�----------------------------*/
		mMessageListView = (ListView) (mArrayList.get(0).findViewById(R.id.message_listView));
		// ��ʼ�����ݿ�
		mDatabaseHelper = new DatabaseHelper(this);
		
		// ��ʼ��ȡ���ݿ��ȡ�����¼
		mMessageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				final String GroupID = ((TextView)(arg1.findViewById(R.id.db_id))).getText().toString();
				if(Constant.DEBUG) Log.e("database", "the short click!!!!!"+arg2+"  "+arg3+" the ID is="+GroupID);
				
				// �������촰��
				Intent mChatIntent = new Intent(MainActivity.this, ChatActivity.class);
				Person chatPerson = childrenMap.get(Integer.valueOf(GroupID));
				
				if(chatPerson != null){
					Bundle bd = new Bundle();
					bd.putSerializable(Constant.userNameStr, chatPerson);
					bd.putSerializable(Constant.myInfoStr, mMySelf);
					bd.putString(Constant.userNewMsgStr, "");
					mChatIntent.putExtras(bd);
					MainActivity.this.startActivity(mChatIntent);
				}
				else{
					Toast.makeText(MainActivity.this, "���û���ǰ�����ߣ�����", Toast.LENGTH_SHORT).show();
				}

			}
        	
		});
        
		mMessageListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				final String GroupID = ((TextView)(arg1.findViewById(R.id.db_id))).getText().toString();
				if(Constant.DEBUG) Log.e("database", "the long click!!!!!"+arg2+"  "+arg3+" the ID is="+GroupID);
				
				AlertDialog.Builder mbuild = new AlertDialog.Builder(MainActivity.this);
				mbuild.setMessage("�Ƿ�ɾ����¼")
					  .setPositiveButton("��", new DialogInterface.OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							mDatabaseHelper.delGroup(Integer.valueOf(GroupID));
							
							// ������Ϣ�б�
							updateTheMessageList();
						}
					})
					.setNegativeButton("��", null)
					.setTitle(null);
				
				mbuild.create().show();
				
				return false;
			}
		});
        
        // �ر����ݿ�
        mDatabaseHelper.close();
		
		
		/*----------------���������Ϣ----------------------------*/
		mWifiFailedText = ((TextView)((mArrayList.get(1).findViewById(R.id.wifi_failed))));
		mWifiFailedText.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(android.os.Build.VERSION.SDK_INT > 10 ){
				     //3.0���ϴ����ý��棬Ҳ����ֱ����ACTION_WIRELESS_SETTINGS�򿪵�wifi����
				    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
				} else {
				    startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
				}
				
			}
		});
		// �жϵ�ǰ�����Ƿ��������
		ConnectivityManager conManager = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = conManager.getActiveNetworkInfo();
        boolean bisConnFlag = false;
        if(network!=null){
            bisConnFlag=conManager.getActiveNetworkInfo().isAvailable();
        }
		if(bisConnFlag){
			//����Ѿ��������ˣ��Ͳ���ʾ���������쳣
			mWifiFailedText.setVisibility(View.GONE);
			isServerStart = false;
		}
		else{
			//��������������˾Ϳ�ʼ�򿪺�̨����
			/*----------------����ͨ�ŷ���----------------------------*/
			StartMyService();
			isServerStart = true;
		}
		
		
		/*----------------ע��㲥�¼�----------------------------*/
		RegBroadcastReceiver();
		
		//��ʼ�ҵ�����.....
		new CheckPersonOnlineThread().start();
		
		isRunning = true;
		if(Constant.DEBUG) Log.i(TAG, "Finish the initView....");
	}
	
	
	
	private void updateTheMessageList(){
		// ��ʼ��List������
		//public Map<Integer,Person> childrenMap = new HashMap<Integer,Person>();
		//public ArrayList<Integer> personKeys = new ArrayList<Integer>();
		
		
		Cursor myCursor = mDatabaseHelper.queryGroupTop();
		mMessageSimpleAdapter = new SimpleCursorAdapter(this, R.layout.message_list, 
				myCursor, 
				mMessageMapIndexStr, //{"UserImg","UserName","Message","TheLastTime","_id"};
				new int[]{R.id.message_img, R.id.message_name, R.id.message_msg, R.id.message_time, R.id.db_id});
		
		mMessageListView.setAdapter(mMessageSimpleAdapter);
		mMessageSimpleAdapter.notifyDataSetChanged();
		
        // �ر����ݿ�
        mDatabaseHelper.close();
	}
	
	
	
	
	
	
	
	
	private class MyRaidoClickListener implements View.OnClickListener{
		private int mIndex = 0;
		
		public MyRaidoClickListener(int index) {
			if(index < 4){
				this.mIndex = index;
			}
		}

		public void onClick(View v) {
			mViewPager.setCurrentItem(this.mIndex);
		}
	}
	
	
	
	
	/**
	 * define the ViewPager Adapter
	 * @author Owner
	 * 
	 */
	private class MyPagerAdapter extends PagerAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mArrayList.size();
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			// TODO Auto-generated method stub
			return arg0 == (arg1);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			// TODO Auto-generated method stub
			((ViewPager)container).addView(mArrayList.get(position));
			return mArrayList.get(position);
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			// TODO Auto-generated method stub
			((ViewPager)container).removeView(mArrayList.get(position));
		}
		
	}



	/**
	 * Handle the BACK and MENU key event
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0){
			if(mPopupWindowShow){
				mPopupWindow.dismiss();
				mPopupWindowShow = false;
			}
			else{
				Intent intent = new Intent(this, ExitDialog.class);
				this.startActivity(intent);
			}
			
		}
		else if(keyCode == KeyEvent.KEYCODE_MENU){
			
			if(mPopupWindowShow){
				mPopupWindow.dismiss();
				mPopupWindowShow = false;
			}
			else{
				LayoutInflater inflater = LayoutInflater.from(this);
				View popupView = inflater.inflate(R.layout.popupmenu, null);
				
				mPopupWindow = new PopupWindow(popupView, LayoutParams.MATCH_PARENT, 
												LayoutParams.WRAP_CONTENT);
				
				mPopupWindow.showAtLocation(this.findViewById(R.id.main_layout),
											Gravity.BOTTOM|Gravity.CENTER, 0, 0);
				
				popupView.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						// TODO Auto-generated method stub
						mPopupWindow.dismiss();
						mPopupWindowShow = false;
						
						Intent mIntent = new Intent(MainActivity.this, ExitDialog.class);
						MainActivity.this.startActivity(mIntent);
						
					}
				});
				
				// Ϊ���ø��ؼ���ȡ�����㣬��ӦOnTouch�¼�
				//mPopupWindow.setFocusable(false);
				//mPopupWindow.setOutsideTouchable(true);

				mPopupWindowShow = true;
			}
		}
		
		return true;
	}
		
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		// ע���˳�APP��ʱ��Ҫֹͣ����,ȡ���㲥ע��
    	unregisterReceiver(mBroadcastReceiver);
    	stopService(mServiceIntent);
		unbindService(mServiceConn);
		mMainService = null;
		isRunning = false;
	}

	
	//---------------��ȡ�û���Ϣ-----------------------
	private void getMyInformation(){
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		userName = mSharedPreferences.getString(Constant.userNameStr, "");
		userImgId =mSharedPreferences.getInt(Constant.userImgStr, 0);
		userID = mSharedPreferences.getInt(Constant.userIDStr, 0);
		
		mMySelf = new Person(userID,userName,userImgId, "192.168.0.1");
		
		if(Constant.DEBUG) Log.i(Constant.TAG, "Activity GetInformation : userName="+userName+"  ImgId="+userImgId +"  userID" +userID);
	}

	//----------------����һ���̼߳���Ƿ����û�����------------------------
	private final class CheckPersonOnlineThread extends Thread{

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
			
			while(isRunning){
				
				// Map��������������û���Ϣ����keySet�������е�key
				Set<Integer> myKeys = childrenMap.keySet();
				for(Integer key : myKeys){
					Person checkPerson = childrenMap.get(key);
					long diffTime = System.currentTimeMillis() - checkPerson.getTimeStamp();
					//if(Constant.DEBUG) Log.d(TAG,"diffTime:"+diffTime+"  TimeStamp:"+checkPerson.getTimeStamp());
					if(diffTime > 15000){//15s
						
						// ɾ���û�ID�����ID��ʶ���û���ΨһID
						personKeys.remove(key);
						
						// ɾ��Map�е�ָ��Person
						childrenMap.remove(key);
						
						
						// ������Ϣ
						Message removeOneMsg = new Message();
						removeOneMsg.what = Constant.MSG_removeOne;
						mainHandler.sendMessage(removeOneMsg);
					}
				}
				
				// Delay 1s
				try {
					sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	
	// -----------------��������-------------------------
	// ����service��Intent
	private Intent mServiceIntent = null;
	private MainService mMainService = null;
	private boolean isServerStart = false;
	private ServiceConnection mServiceConn = new ServiceConnection(){
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			// �����IBinder�Ǳ��صģ����Կ���ֱ��ʹ��
			mMainService = ((MainService.MyBinder)service).getServiceInstance();
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			Toast.makeText(MainActivity.this, "the Service is Disconnedted!", Toast.LENGTH_LONG).show();
			mMainService = null;
		}
		
	};
	
	private void StartMyService(){
		mServiceIntent = new Intent(MainActivity.this, MainService.class);
		mServiceIntent.putExtra(Constant.userIDStr, userID);
		mServiceIntent.putExtra(Constant.userImgStr, userImgId);
		mServiceIntent.putExtra(Constant.userNameStr, userName);
		
		this.bindService(mServiceIntent, mServiceConn, BIND_AUTO_CREATE);
		this.startService(mServiceIntent);
	}
	
	
	// ----------------ע��㲥�¼�----------------------
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		private ProgressDialog mProgressDialog = null;
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String mainAction = intent.getAction();
			//if(Constant.DEBUG) Log.i(TAG, "BroadcastReceiver receiver ==>"+ mainAction);
			
			if(mainAction.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
				// ��ȡ��ǰWIFI״̬
				Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);    
				if (null != parcelableExtra) {    
					NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;    
					State state = networkInfo.getState();  
					if(state==State.CONNECTED){  
						//Toast.makeText(MainActivity.this, "WIFI�����Ѿ���������!!!", Toast.LENGTH_SHORT).show();
						mWifiFailedText.setVisibility(View.GONE);
						
						if(!isServerStart){
							StartMyService();
							isServerStart = true;
						}
					}  
					else{
						//Toast.makeText(MainActivity.this, "WIFI�Ͽ���!!!", Toast.LENGTH_SHORT).show();
						mWifiFailedText.setVisibility(View.VISIBLE);
					}
				}
			}
			else if(mainAction.equals(Constant.BR_NewUser_Update)){
				String recvUserName = intent.getExtras().getString(Constant.userNameStr);
				int recvUserImgID = intent.getExtras().getInt(Constant.userImgStr);
				int recvUserID = intent.getExtras().getInt(Constant.userIDStr);
				String recvUserHost = intent.getExtras().getString(Constant.userHostStr);
				
				if(!personKeys.contains(Integer.valueOf(recvUserID))){
					// ����һ��Person���󣬸��Ǿɶ���
					Person mPerson = new Person(recvUserID,recvUserName,recvUserImgID,recvUserHost);
					
					// �����û�ID�����ID��ʶ���û���ΨһID
					personKeys.add(Integer.valueOf(recvUserID));
					// �û�ID��һ��������û�person���MAP�ṹ������ͨ��ID���ҵ�mPerson
					childrenMap.put(Integer.valueOf(recvUserID), mPerson);
					
					if(children.size() != 0)
						children.set(0, childrenMap);
					else{
						children.add(0, childrenMap);
					}
					
					//if(Constant.DEBUG) Log.i(TAG,"add a new person, timeStamp:"+mPerson.getTimeStamp());
					
				}
				else{
					// �Ѿ����������person����
					// ����һ��Person���󣬸��Ǿɶ���
					Person mPerson = new Person(recvUserID,recvUserName,recvUserImgID,recvUserHost);
					// �û�ID��һ��������û�person���MAP�ṹ������ͨ��ID���ҵ�mPerson
					childrenMap.put(Integer.valueOf(recvUserID), mPerson);
					
				}
				mExListAdapter.notifyDataSetChanged();
				if(Constant.DEBUG) Log.i(TAG, "BR_NewUser_Update recvUserID="+recvUserID+"  recvUserImgID="+recvUserImgID+"  recvUserName="+recvUserName);
			}
			//-------------
			else if(mainAction.equals(Constant.BR_NewMessage)){
				
				if(!isActivityOnTop("ChatActivity")){
					String message = intent.getExtras().getString(Constant.userNewMsgStr);
					int recvUID = intent.getExtras().getInt(Constant.userIDStr);
					//if(Constant.DEBUG) Log.e(Constant.TAG,"UID="+recvUID+"  message="+message);
					Person recvPerson = MainActivity.this.childrenMap.get(Integer.valueOf(recvUID));
					
					// ����Notification
					NotificationManager notifyManger = (NotificationManager)(MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE)); 
					Notification notify = new Notification();
					notify.icon = R.drawable.ic_launcher;// ����ͼ��
					notify.tickerText = "�����µ���Ϣ"; 
					notify.flags = Notification.FLAG_AUTO_CANCEL;
					notify.defaults = Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE; // ��������
					
					long[]vibrate = new long[]{1000,1000,1000,1000,1000};
					notify.vibrate  = vibrate; // ������
					
					Intent mIntent = new Intent(MainActivity.this, ChatActivity.class);  
					Bundle bd = new Bundle();
					bd.putSerializable(Constant.userNameStr, recvPerson);
					bd.putSerializable(Constant.myInfoStr, mMySelf);
					bd.putString(Constant.userNewMsgStr, message);
					mIntent.putExtras(bd);
					
					
					PendingIntent contentIntent = PendingIntent.getActivity(MainActivity.this, 
													0, mIntent, PendingIntent.FLAG_ONE_SHOT);
					
					notify.setLatestEventInfo(MainActivity.this, 
											recvPerson.getName()+"˵��", 
											message, 
											contentIntent );
					
					notifyManger.notify(0, notify);
					
				}
				
			}
			//-------------
			else if(mainAction.equals(Constant.BR_RecvNewFile)){
				if(!isActivityOnTop("ChatActivity")){
					final String fileName = intent.getExtras().getString(Constant.RecvFileName);
					int recvUID = intent.getExtras().getInt(Constant.userIDStr);
					final Person chatPerson = MainActivity.this.childrenMap.get(Integer.valueOf(recvUID));
					
					AlertDialog.Builder alert= new AlertDialog.Builder(MainActivity.this);
					
					alert.setMessage(chatPerson.getName()+"����������һ���ļ���"+fileName+",�Ƿ����?");
					alert.setPositiveButton("����", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// �ȰѶԻ�������
							if(dialog != null)
								dialog.cancel();
							
							// �����������Ի���
							// �����յ����͵��ֽ����������Ի�����ʾ
							mProgressDialog = new ProgressDialog(MainActivity.this);
							// ����mProgressDialog���Ϊ����
							mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
							// ����mProgressDialog����
							mProgressDialog.setTitle("�����ļ���...");
							// ����mProgressDialog�Ľ������Ƿ���ȷ
							mProgressDialog.setIndeterminate(false);
							// �Ƿ���԰����˼�ȡ��
							mProgressDialog.setCancelable(true);
		                    // ����mProgressDialog��һ��Button
							mProgressDialog.setButton("ȷ��", new DialogInterface.OnClickListener()
						    {
						         public void onClick(DialogInterface dialog, int which)
						         {
						             dialog.cancel();
						         }
						    });
							// ��ʾmProgressDialog
							mProgressDialog.show();
							
							
							
							// ͨ����̨��������Ϣ
							mMainService.startRecvFile(chatPerson, fileName);
						}
					});
					alert.setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// �ܾ������ļ�
							mMainService.rejectRecvFile(chatPerson, fileName);
						}
					});
					
					AlertDialog dialog = alert.create();
					dialog.show();
				}
				
			}
			//-------------
			else if(mainAction.equals(Constant.BR_ReceveFileDone)){
				if(!isActivityOnTop("ChatActivity")){
					String mRecvDone = intent.getExtras().getString(Constant.HandleFileName);
					int recvUID = intent.getExtras().getInt(Constant.userIDStr);
					Person recvPerson = MainActivity.this.childrenMap.get(Integer.valueOf(recvUID));
					
					// ����Notification
					NotificationManager notifyManger = (NotificationManager)(MainActivity.this.getSystemService(Context.NOTIFICATION_SERVICE)); 
					Notification notify = new Notification();
					notify.icon = R.drawable.ic_launcher;// ����ͼ��
					notify.tickerText = "�����ļ��ɹ�"; 
					notify.flags = Notification.FLAG_AUTO_CANCEL;
					notify.defaults = Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE; // ��������
					
					long[]vibrate = new long[]{1000,1000,1000,1000,1000};
					notify.vibrate  = vibrate; // ������
					
					Intent mIntent = new Intent(MainActivity.this, ChatActivity.class);  
					Bundle bd = new Bundle();
					bd.putSerializable(Constant.userNameStr, recvPerson);
					bd.putSerializable(Constant.myInfoStr, mMySelf);
					bd.putString(Constant.userNewMsgStr,"�ļ����ճɹ�������ڣ�"+FileSavePath+mRecvDone);
					mIntent.putExtras(bd);
					PendingIntent contentIntent = PendingIntent.getActivity(MainActivity.this, 
													0, mIntent, PendingIntent.FLAG_ONE_SHOT);
					
					notify.setLatestEventInfo(MainActivity.this, 
											recvPerson.getName()+"���ļ����ճɹ�", 
											"�ļ�����ڣ�"+FileSavePath+mRecvDone, 
											contentIntent );
					
					notifyManger.notify(0, notify);
				}
			}
			//-------------
			else if(mainAction.equals(Constant.BR_RecvFileSize)){
				if(!isActivityOnTop("ChatActivity")){
					long totalSize = intent.getExtras().getLong(Constant.totalSizes);
					long hasRecvSize = intent.getExtras().getLong(Constant.hasRecvSize);
					 //if(Constant.DEBUG) Log.e(Constant.TAG, "Send the file bytes:"+hasRecvSize+" totalSize:"+totalSize);
					
					if(mProgressDialog != null){
						// �����ļ��Ľ���
						int rate = (int)(hasRecvSize*100/totalSize);
						mProgressDialog.setProgress(rate);
						// �������
						if(rate >= 100){
							mProgressDialog = null;
						}
					}
				}
				
			}
			//--------------------------------------
			else if(mainAction.equals(Constant.BR_AudioRequest)){
				// �յ������������������
				if(!isActivityOnTop("ChatActivity")){
					AlertDialog.Builder alert= new AlertDialog.Builder(MainActivity.this);
					int recvUID = intent.getExtras().getInt(Constant.userIDStr);
					final Person chatPerson = MainActivity.this.childrenMap.get(Integer.valueOf(recvUID));
					
					alert.setMessage(chatPerson.getName()+"�����������ͨ��,�Ƿ����?");
					alert.setPositiveButton("����", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// ͨ����̨��������Ϣ
							Intent mSendAudioIntent = new Intent(MainActivity.this, SendAudio.class);
							Bundle bd = new Bundle();
							bd.putSerializable(Constant.userNameStr, chatPerson);
							bd.putSerializable(Constant.myInfoStr, mMySelf);
							bd.putSerializable(Constant.audioCmdStr, 0x02);
							mSendAudioIntent.putExtras(bd);
							MainActivity.this.startActivityForResult(mSendAudioIntent, Constant.SendAudio_RequestCode);
							
							// ��̨��ʼ������������,��ʼ��������
							mMainService.agreeRecvAudio(chatPerson);
						}
					});
					alert.setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// �ܾ���������ͨ��
							mMainService.rejectAudioConnect(chatPerson);
						}
					});
					
					AlertDialog dialog = alert.create();
					dialog.show();
					
				}
			}
			//--------------------------------------
			else if(mainAction.equals(Constant.BR_VideoRequest)){
				// �յ������������������
				if(!isActivityOnTop("ChatActivity")){
					AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
					int recvUID = intent.getExtras().getInt(Constant.userIDStr);
					final Person chatPerson = MainActivity.this.childrenMap.get(Integer.valueOf(recvUID));
					
					alert.setMessage(chatPerson.getName()+"���������Ƶͨ��,�Ƿ����?");
					alert.setPositiveButton("����", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// ͨ����̨��������Ϣ
							Intent mSendVideoIntent = new Intent(MainActivity.this, SendVideo.class);
							Bundle bd = new Bundle();
							bd.putSerializable(Constant.userNameStr, chatPerson);
							bd.putSerializable(Constant.myInfoStr, mMySelf);
							bd.putSerializable(Constant.audioCmdStr, 0x02);
							mSendVideoIntent.putExtras(bd);
							MainActivity.this.startActivityForResult(mSendVideoIntent, Constant.SendVideo_RequestCode);
							
							// ��̨��ʼ������Ƶ����,��ʼ������Ƶ
							mMainService.agreeVideoConnect(chatPerson);
						}
					});
					alert.setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							// �ܾ�������Ƶͨ��
							mMainService.rejectVideoConnect(chatPerson);
						}
					});
					
					AlertDialog dialog = alert.create();
					dialog.show();
					
				}
			}
			
			//--------------------------------------
		}
	};
	
	private IntentFilter mBroadcastFilter = new IntentFilter();
	
	private void RegBroadcastReceiver(){
		
		mBroadcastFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		
		mBroadcastFilter.addAction(Constant.BR_NewUser_Update);
		mBroadcastFilter.addAction(Constant.BR_NewMessage);
		
		mBroadcastFilter.addAction(Constant.BR_RecvNewFile);
		mBroadcastFilter.addAction(Constant.BR_RecvFileSize);
		mBroadcastFilter.addAction(Constant.BR_ReceveFileDone);
		
		mBroadcastFilter.addAction(Constant.BR_AudioRequest);
		mBroadcastFilter.addAction(Constant.BR_VideoRequest);
		this.registerReceiver(mBroadcastReceiver, mBroadcastFilter);
	}
	
	
	// ---------���߳���Ӧ��Ϣ�����handler---------
	private final Handler mainHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			if(msg.what == Constant.MSG_removeOne){
				if(Constant.DEBUG) Log.i(TAG,"one Person is offline!");
				mExListAdapter.notifyDataSetChanged();
			}
			
		}
	};
	
	private boolean isActivityOnTop(String ActivityName){
		ActivityManager mActivityManager = (ActivityManager)(MainActivity.this.getSystemService(Context.ACTIVITY_SERVICE));
		List<RunningTaskInfo> runningTaskInfos = mActivityManager.getRunningTasks(1);
		if(runningTaskInfos != null){
			String cmpNameTemp = (runningTaskInfos.get(0).topActivity).toString();
			//if(Constant.DEBUG) Log.e(Constant.TAG, "The top Activity is:"+cmpNameTemp);
			if(cmpNameTemp.contains(ActivityName)){
				return true;
			}
		}
		return false;
	}

	
	//-----------------������setting����Ĵ�����-----------------------------
	public void onSetFileSavePath(View v){
		AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(MainActivity.this);
		
		LayoutInflater mInfalter = LayoutInflater.from(MainActivity.this);
		View pathView = mInfalter.inflate(R.layout.filepath, null);
		TextView editFilePath = (TextView)pathView.findViewById(R.id.editFilePath);
		editFilePath.setText(FileSavePath);
		
		mAlertBuilder.setTitle("���յ��ļ������ڣ�")
					.setView(pathView)
					.setPositiveButton("֪����", null);
					//.setNegativeButton("ȡ��", null);
		
		mAlertBuilder.create().show();
	}
	
	
	public void onSettingAbout(View v){
		AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(MainActivity.this);
		
		mAlertBuilder.setTitle("��������")
					.setIcon(R.drawable.ic_launcher)
					.setMessage("    ���ھ�������ͨ�Ź��ߣ�������������/�շ��ļ�/�����Խ�/ʵʱ��Ƶ��" +
								"���в���UIģ��΢�ŷ�񣬲����� @Сì ���Ჱ��ͷ��\n\n" +
								"���ߣ�Ryan_XM \n" +
								"���䣺RyanXm1122@qq.com \n" +
								"�汾�� V1.0 \n")
					.setPositiveButton("ȷ��", null);
					
		mAlertBuilder.create().show();
	}
	
	public void onSetShortCut(View v){
		AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(MainActivity.this);
		
		mAlertBuilder.setIcon(R.drawable.ic_launcher)
					.setMessage("�Ƿ񴴽���ݷ�ʽ?")
					.setPositiveButton("��", new DialogInterface.OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							Intent addIntent = new Intent(
									"com.android.launcher.action.INSTALL_SHORTCUT");
							String title = getResources().getString(R.string.app_name);
							Parcelable icon = Intent.ShortcutIconResource.fromContext(MainActivity.this, R.drawable.ic_launcher);
							
							Intent myIntent = new Intent(MainActivity.this, WelcomeActivity.class);
							
							addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
							addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
							addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, myIntent);
							
							sendBroadcast(addIntent);
						}
					})
					.setNegativeButton("��", null);
		mAlertBuilder.create().show();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
