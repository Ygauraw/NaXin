package org.ryan.naxin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ChatActivity extends Activity {
	
	public static final String ME = "  ��   ";
	
	private EditText mChatEditText = null;
	private Button mSendBn = null;
	private ListView mChatListView = null;
	
	private Person chatPerson = null;
	private Person myPerson  = null;
	
	private ExSimpleAdapter mSimpleAdapter = null;
	private List<Map<String,Object>> mListData = new ArrayList<Map<String,Object>>();
	
	private String[] mMapIndexStr = {"UserImg","UserName","Message"};
	
	private ArrayList<Boolean> mMsgFromList = new ArrayList<Boolean>();
	
	// ���ݿ������
	private DatabaseHelper mDatabaseHelper = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.chat);

		String tempMsg = (String)(getIntent().getExtras().getString(Constant.userNewMsgStr));
		myPerson = (Person)(getIntent().getExtras().getSerializable(Constant.myInfoStr));
		chatPerson = (Person)(getIntent().getExtras().getSerializable(Constant.userNameStr));
		((TextView)findViewById(R.id.chat_userName)).setText("�� "+chatPerson.getName()+" ������...");
		
		
		// ��ʼ���ؼ�
		mChatEditText = (EditText)findViewById(R.id.chat_editText);
		mSendBn = (Button)findViewById(R.id.chat_send);
		mChatListView = (ListView)findViewById(R.id.chat_listView);
		
		// ��ʼ��List������
		mSimpleAdapter = new ExSimpleAdapter(this,mListData, 
										R.layout.message_left,
										mMapIndexStr,
										new int[]{R.id.msg_img,R.id.msg_name,R.id.msg}
										);
		mChatListView.setAdapter(mSimpleAdapter);
		
		// �󶨷��Ͱ�ť
		mSendBn.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String chatMsg = (mChatEditText.getText()).toString().trim();
				if(chatMsg.equals("")){
					Toast.makeText(ChatActivity.this, "��������������", Toast.LENGTH_SHORT).show();
				}
				else{
					// ������ݲ�Ϊ�������
					addListItem(true, myPerson.getImgID(), ME, chatMsg );
					
					mChatEditText.setText("");
					
					// ͨ����̨��������Ϣ
					mMainService.sendMsg(myPerson.getUID(), chatPerson.getPersonHost() ,chatMsg);
				}
			}
		});
		
		
		// ---------------------�������¼�浽���ݿ���----------------------------
		mDatabaseHelper = new DatabaseHelper(this);
		Cursor myCursor = mDatabaseHelper.queryGroup(chatPerson.getUID());
		// ��ԭ�еļ�¼��ȡ����
		if(Constant.DEBUG) Log.d(Constant.TAG, "��ǰ�����¼һ���� "+myCursor.getCount()+" ��");
		
		//UserImg int, UserName text, Message text, LastTime text, IsMe int, GroupID int
		for(myCursor.moveToFirst(); !myCursor.isAfterLast(); myCursor.moveToNext())
		{
		    int UserImgColumn = myCursor.getColumnIndex("UserImg");
		    int UserNameColumn = myCursor.getColumnIndex("UserName");
		    int MessageColumn = myCursor.getColumnIndex("Message");
		    int LastTimeColumn = myCursor.getColumnIndex("LastTime");
		    int IsMeColumn = myCursor.getColumnIndex("IsMe");
		    
		    
		    int UserImg = myCursor.getInt(UserImgColumn);
		    String UserName = myCursor.getString(UserNameColumn);
		    String Message = myCursor.getString(MessageColumn);
		    //String LastTime = myCursor.getString(LastTimeColumn);
		    int IsMe = myCursor.getInt(IsMeColumn);
		    
		    
			Map<String,Object> mMapData = new HashMap<String, Object>();
			
			mMapData.put(mMapIndexStr[0], UserImg);// Image
			mMapData.put(mMapIndexStr[1], UserName );// Name
			mMapData.put(mMapIndexStr[2], Message);// Message
			
			mListData.add(mMapData);
			mMsgFromList.add(IsMe==1?true:false);
			mSimpleAdapter.notifyDataSetChanged();
		    
		}
		
		if(myCursor!= null)
			myCursor.close();
		
		
		
		// ����Ǵ�MainActivity�н�����Ϣ
		if(!tempMsg.equals("")){
			
			addListItem(false, chatPerson.getImgID(), chatPerson.getName(), tempMsg );
		}
		
		// --------------------�󶨷���-----------------------------
		mServiceIntent = new Intent(ChatActivity.this, MainService.class);
		this.bindService(mServiceIntent, mServiceConn, BIND_AUTO_CREATE);
		
		// ------------------���չ㲥-------------------------------
		IntentFilter mChatFilter = new IntentFilter();
		mChatFilter.addAction(Constant.BR_NewMessage);
		mChatFilter.addAction(Constant.BR_RecvNewFile);
		mChatFilter.addAction(Constant.BR_HandleNewFileResult);
		mChatFilter.addAction(Constant.BR_SendFileSize);
		mChatFilter.addAction(Constant.BR_RecvFileSize);
		mChatFilter.addAction(Constant.BR_ReceveFileDone);
		mChatFilter.addAction(Constant.BR_AudioRequest);
		mChatFilter.addAction(Constant.BR_VideoRequest);
		this.registerReceiver(mChatReceiver, mChatFilter);
		
		
	}

	
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
	private void addListItem(boolean isMe, int ImgId, String name, String msg){
		
		Map<String,Object> mMapData = new HashMap<String, Object>();
		
		mMapData.put(mMapIndexStr[0], Constant.mThumbIds[ImgId]);// Image
		mMapData.put(mMapIndexStr[1], name );// Name
		mMapData.put(mMapIndexStr[2], msg);// Message
		
		mListData.add(mMapData);
		mMsgFromList.add(isMe);
		mSimpleAdapter.notifyDataSetChanged();
		
		
		// ------��ÿ�������¼���浽���ݿ���--------------
        ContentValues values = new ContentValues();
        values.put("UserImg", Constant.mThumbIds[ImgId]);
        values.put("UserName", name);
        values.put("Message", msg);
        values.put("IsMe", isMe?1:0);
        values.put("LastTime", formatter.format(new Date(System.currentTimeMillis())));
        values.put("GroupID", chatPerson.getUID()); //����������UID��Ϊһ�������¼��ͬ��GroupID
        
        mDatabaseHelper.insert(values);
        mDatabaseHelper.close();
	}
	
	
	
	//����һ����չ��SimpleAdapter
	private class ExSimpleAdapter extends SimpleAdapter{
		
		public ExSimpleAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			LayoutInflater mLayoutInflater = LayoutInflater.from(ChatActivity.this);
			if(mMsgFromList.get(position))
				convertView = mLayoutInflater.inflate(R.layout.message_right, null);
			else
				convertView = mLayoutInflater.inflate(R.layout.message_left, null);
			
			return super.getView(position, convertView, parent);
		}
	}
	
	
	
	
	
	// ���������Intent
	private Intent mServiceIntent = null;
	private MainService mMainService = null;
	
	private ServiceConnection mServiceConn = new ServiceConnection(){
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			// �����IBinder�Ǳ��صģ����Կ���ֱ��ʹ��
			if(Constant.DEBUG) Log.i(Constant.TAG,"ChatActivity: the Service is connedted!");
			mMainService = ((MainService.MyBinder)service).getServiceInstance();
		}

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			if(Constant.DEBUG) Log.i(Constant.TAG,"ChatActivity: the Service is Disconnedted!");
		}
		
	};
	
	
	//-------------�㲥������-------------------
	private BroadcastReceiver mChatReceiver = new BroadcastReceiver(){
		private ProgressDialog mProgressDialog = null;
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(action.equals(Constant.BR_NewMessage)){
				// ������ݲ�Ϊ�������
				if(Constant.DEBUG) Log.e(Constant.TAG,"ChatActivity: the new message broadcast receiver!");

				String message = intent.getExtras().getString(Constant.userNewMsgStr);
				
				addListItem(false, chatPerson.getImgID(), chatPerson.getName(), message );
				
				
				// ������
				long[]  pattern = new long[]{50,200,50};
				Vibrator vib = (Vibrator) ChatActivity.this.getSystemService(Service.VIBRATOR_SERVICE);
				vib.vibrate(pattern, -1);
				
			}
			//---------------------
			else if(action.equals(Constant.BR_RecvNewFile)){
				final String fileName = intent.getExtras().getString(Constant.RecvFileName);
				
				AlertDialog.Builder alert= new AlertDialog.Builder(ChatActivity.this);
				alert.setMessage(chatPerson.getName()+"����������һ���ļ���"+fileName+",�Ƿ����?");
				alert.setPositiveButton("����", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						// ͨ����̨��������Ϣ
						// �ȰѶԻ�������
						if(dialog != null)
							dialog.cancel();
						
						// �����������Ի���
						// �����յ����͵��ֽ����������Ի�����ʾ
						mProgressDialog = new ProgressDialog(ChatActivity.this);
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
						
						
						// ��̨��ʼ������Ϣ
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
			//----------------------
			// �Ƿ�Ҫ�����ļ�
			else if(action.equals(Constant.BR_HandleNewFileResult)){

				String message = "Sorry, �Ҿܾ���������ļ�";
				boolean isAgree = intent.getExtras().getBoolean(Constant.HandleFileName);
				if(isAgree){
					message = "Thanks, ��ͬ���������ļ�";
				}
				
				addListItem(false, chatPerson.getImgID(), chatPerson.getName(), message );
				
				
				if(isAgree){
					// ͬ����գ��������ʹ���
					// �����յ����͵��ֽ����������Ի�����ʾ
					mProgressDialog = new ProgressDialog(ChatActivity.this);
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
		 
					
					// ��ʼͨ��socket�����ļ�
					mMainService.startSendFile(chatPerson);
				}
			}
			//----------------------
			// �����ļ��ɹ�
			else if(action.equals(Constant.BR_ReceveFileDone)){
				String mRecvDone = intent.getExtras().getString(Constant.HandleFileName);
				
				addListItem(true, myPerson.getImgID(), ME, "���ճɹ��������ڣ�/mnt/sdcard/"+mRecvDone );
			}
			//----------------------
			else if(action.equals(Constant.BR_SendFileSize)){
				long totalSize = intent.getExtras().getLong(Constant.totalSizes);
				long hasSendSize = intent.getExtras().getLong(Constant.hasSendSize);
				 //if(Constant.DEBUG) Log.e(Constant.TAG, "Send the file bytes:"+hasSendSize+" totalsize:"+totalSize);
				
				if(mProgressDialog != null){
					// �����ļ��Ľ���
					int rate = (int)(hasSendSize*100/totalSize);
					mProgressDialog.setProgress(rate);
					// �������
					if(rate >= 100){
						mProgressDialog = null;
					}
				}
				
			}
			//----------------------
			else if(action.equals(Constant.BR_RecvFileSize)){
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
			//----------------------
			else if(action.equals(Constant.BR_AudioRequest)){
				// �յ������������������
				AlertDialog.Builder alert= new AlertDialog.Builder(ChatActivity.this);
				alert.setMessage(chatPerson.getName()+"�����������ͨ��,�Ƿ����?");
				alert.setPositiveButton("����", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						// ͨ����̨��������Ϣ
						Intent mSendAudioIntent = new Intent(ChatActivity.this, SendAudio.class);
						Bundle bd = new Bundle();
						bd.putSerializable(Constant.userNameStr, chatPerson);
						bd.putSerializable(Constant.myInfoStr, myPerson);
						bd.putSerializable(Constant.audioCmdStr, 0x02);
						mSendAudioIntent.putExtras(bd);
						ChatActivity.this.startActivityForResult(mSendAudioIntent, Constant.SendAudio_RequestCode);
						
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
			//----------------------
			else if(action.equals(Constant.BR_VideoRequest)){
				// �յ������������������
				AlertDialog.Builder alert= new AlertDialog.Builder(ChatActivity.this);
				alert.setMessage(chatPerson.getName()+"���������Ƶͨ��,�Ƿ����?");
				alert.setPositiveButton("����", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						// ͨ����̨��������Ϣ
						Intent mSendVideoIntent = new Intent(ChatActivity.this, SendVideo.class);
						Bundle bd = new Bundle();
						bd.putSerializable(Constant.userNameStr, chatPerson);
						bd.putSerializable(Constant.myInfoStr, myPerson);
						bd.putSerializable(Constant.audioCmdStr, 0x02);
						mSendVideoIntent.putExtras(bd);
						ChatActivity.this.startActivityForResult(mSendVideoIntent, Constant.SendVideo_RequestCode);
						
						// ��̨��ʼ������Ƶ����,��ʼ������Ƶ
						mMainService.agreeVideoConnect(chatPerson);
					}
				});
				alert.setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						// �ܾ���������ͨ��
						mMainService.rejectVideoConnect(chatPerson);
					}
				});
				
				AlertDialog dialog = alert.create();
				dialog.show();
				
			}
			//----------------------
		}
	};
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, 0, 0, "�����ļ�");
		menu.add(0, 1, 0, "��������");
		menu.add(0, 2, 0, "������Ƶ");
		return super.onCreateOptionsMenu(menu);
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch(item.getItemId()){
			case 0: // �����ļ�
				Intent mSendFilesIntent = new Intent(this, SelectFiles.class);
				this.startActivityForResult(mSendFilesIntent, Constant.SelectFiles_RequestCode);
				break;
				
			case 1: // ��������
				Intent mSendAudioIntent = new Intent(this, SendAudio.class);
				Bundle bd = new Bundle();
				bd.putSerializable(Constant.userNameStr, chatPerson);
				bd.putSerializable(Constant.myInfoStr, myPerson);
				bd.putSerializable(Constant.audioCmdStr, 0x01);
				mSendAudioIntent.putExtras(bd);
				this.startActivityForResult(mSendAudioIntent, Constant.SendAudio_RequestCode);
				break;
				
			case 2: // ������Ƶ
				Intent mSendVideoIntent = new Intent(this, SendVideo.class);
				Bundle video_bd = new Bundle();
				video_bd.putSerializable(Constant.userNameStr, chatPerson);
				video_bd.putSerializable(Constant.myInfoStr, myPerson);
				video_bd.putSerializable(Constant.videoCmdStr, 0x01);
				mSendVideoIntent.putExtras(video_bd);
				this.startActivityForResult(mSendVideoIntent, Constant.SendVideo_RequestCode);
				break;
		}
		
		return super.onOptionsItemSelected(item);
	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode){
			// �����ļ�
			case Constant.SelectFiles_RequestCode:
				if(resultCode == Constant.SelectFiles_ResultCode_OK){
					String mSendFilePath = data.getExtras().getString(Constant.SelectFilesStr);
					if(Constant.DEBUG) Log.i(Constant.TAG, "Select the file path:" + mSendFilePath);
					
					// ������ݲ�Ϊ�������
					addListItem(true, myPerson.getImgID(), ME,  "���㷢����һ���ļ���" + mSendFilePath );

					// ͨ����̨��������Ϣ
					mMainService.sendFile(myPerson.getUID(), chatPerson.getPersonHost(), mSendFilePath);
				}
				break;
			
			// ��������
			case Constant.SendAudio_RequestCode:
				break;
		
			// ������Ƶ
			case Constant.SendVideo_RequestCode:
				break;
		}
		
	}



	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mChatReceiver);
		this.unbindService(mServiceConn);
		mServiceConn = null;
		mMainService = null;
		
		if(mDatabaseHelper != null)
			mDatabaseHelper.close();
	}
	
}
