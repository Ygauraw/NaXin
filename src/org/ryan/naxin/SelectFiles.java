package org.ryan.naxin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SelectFiles extends Activity {
	
	// ����ؼ���Ա
	private TextView mTextView;
	private ListView mListView;
	
	// ��¼��ǰ�ĸ��ļ���
	File mCurrentParent;
	// ��¼��ǰ·���µ������ļ����ļ�����
	File[] mCurrentFiles;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.selectfiles);
		
		mTextView = (TextView)findViewById(R.id.sdcard_path);
		mListView = (ListView)findViewById(R.id.listView_files);
		
		mListView.setOnItemClickListener(new OnItemClickListener(){
			public void onItemClick(AdapterView<?> parent, View view, int position, long id){
				if(Constant.DEBUG) Log.i(Constant.TAG, "onItemClick position =" + position );
				//���ѡ�����һ���ļ�
				if(mCurrentFiles[position].isFile() == true){

					try {
						// �����ļ�
						Intent FilePathIntent = new Intent();
						String SelectFilePath;
						SelectFilePath = mCurrentFiles[position].getCanonicalPath();
						FilePathIntent.putExtra(Constant.SelectFilesStr, SelectFilePath);
						setResult(Constant.SelectFiles_ResultCode_OK, FilePathIntent);
						finish();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					return;
				}
				//�����һ��Ŀ¼
				else{
					// ��ȡѡ���Ŀ¼�������ļ�
					File[] tempFiles = mCurrentFiles[position].listFiles();
					if((tempFiles == null)||(tempFiles.length == 0)){
						//���û���ļ�
						Toast.makeText(SelectFiles.this, "There are's the files!", Toast.LENGTH_SHORT).show();
					}
					else{
						//������ļ������г�
						mCurrentParent = mCurrentFiles[position];
						mCurrentFiles = tempFiles;
						inflateListView(mCurrentFiles);
					}
				}
			}
		});
		
		
		// �󶨰�ť�ļ����¼�
		Button mBunton = (Button)findViewById(R.id.button_parent);
		mBunton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				try {
					// TODO Auto-generated method stub
					if (!mCurrentParent.getCanonicalPath().equals("/mnt/sdcard")) {
						// ��ȡ��һ��Ŀ¼
						mCurrentParent = mCurrentParent.getParentFile();
						mCurrentFiles = mCurrentParent.listFiles();
						inflateListView(mCurrentFiles);
					}
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		});
		
		// ��ȡϵͳ��SD��Ŀ¼
		File root = new File("/mnt/sdcard");
		// �ж�SD���Ƿ����
		if(root.exists() == true){
			//��¼��ǰ·�����ļ�����
			mCurrentParent = root;
			mCurrentFiles = root.listFiles();
			//ʹ�õ�ǰĿ¼�µ�ȫ���ļ����ļ������LISTVIEW
			inflateListView(mCurrentFiles);
		}
		else{
			mTextView.setText(R.string.no_found_SDcard);
		}
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	private void inflateListView(File[] files){
		// ����һ��List���ϣ�List���ϵ�Ԫ����Map
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		for (int i = 0; i < files.length; i++)
		{
			Map<String, Object> listItem = new HashMap<String, Object>();
			//�����ǰFile���ļ��У�ʹ��folderͼ�ꣻ����ʹ��fileͼ��
			if (files[i].isDirectory())
			{
				listItem.put("icon", R.drawable.folder);
			}
			else
			{
				listItem.put("icon", R.drawable.file);
			}
			listItem.put("fileName", files[i].getName());
			//���List��
			listItems.add(listItem);
		}
		
		// ���ջ���Ҫ����һ��Adapter
		// ����һ��SimpleAdapter
		SimpleAdapter simpleAdapter = new SimpleAdapter(this, listItems,
			R.layout.list_files, new String[] { "icon", "fileName" }, new int[] {
				R.id.icon, R.id.file_name });
		
		
		// ΪListView��Adapter
		mListView.setAdapter(simpleAdapter);
		// ���õ�ǰ·�������·����
		try {
			mTextView.setText(mCurrentParent.getCanonicalPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	


}
