package org.ryan.naxin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private SQLiteDatabase SQLiteDb = null;
	
	private static final int DB_VERSION = 1;
	private static final String DB_NAME = "NaXinDB.db";  
    private static final String TBL_NAME = "MessageTable";  
    private static final String CREATE_TBL = "create table MessageTable ( _id integer primary key autoincrement, UserImg int, UserName text, Message text, LastTime text, IsMe int, GroupID int)"; 

	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		// TODO Auto-generated constructor stub
	}

	//��onCreate������д����Ĳ���
	@Override
	public void onCreate(SQLiteDatabase arg0) {
		// TODO Auto-generated method stub
		this.SQLiteDb = arg0;
		try {
			if(Constant.DEBUG) Log.e("DataBase", "������=========>");
			SQLiteDb.execSQL(CREATE_TBL);//��Ҫ�쳣����
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
	}
	
	
	
	// 1. ��ɾ�Ĳ�ķ��� ---- ����
    public void insert(ContentValues values) {  
		SQLiteDb = getWritableDatabase();  
        SQLiteDb.insert(TBL_NAME, null, values);  
    }  
    // 2. ��ɾ�Ĳ�ķ��� ---- ��ѯ
    public Cursor queryAll() {  
		SQLiteDb = getWritableDatabase();   
        Cursor c = SQLiteDb.query(TBL_NAME, null, null, null, null, null, null);  
        return c;  
    }  
    
    public Cursor queryGroupTop() {  
		SQLiteDb = getWritableDatabase();   
        Cursor c = SQLiteDb.query(TBL_NAME, null, "UserName!=?", new String[]{ChatActivity.ME}, "GroupID", null, "_id");  
        return c;  
    }  
    
    
    public Cursor queryGroup(int GroupID) {  
		SQLiteDb = getWritableDatabase();   
        Cursor c = SQLiteDb.query(TBL_NAME, null, "GroupID=?", new String[]{String.valueOf(GroupID)},   null, null, null);  
        return c;  
    }  
    
    
    // 3. ��ɾ�Ĳ�ķ��� ---- ɾ��
    public void del(int id) {  
		SQLiteDb = getWritableDatabase();  
        SQLiteDb.delete(TBL_NAME, "_id=?", new String[] { String.valueOf(id) });  
    }  
    
    public void delGroup(int GroupID) {  
		SQLiteDb = getWritableDatabase();  
        SQLiteDb.delete(TBL_NAME, "GroupID=?", new String[] { String.valueOf(GroupID) });  
    }  
    
    
    public void close() {  
        if (SQLiteDb != null)  
        	SQLiteDb.close();  
    }  
	
	
	
	
	
	
	

}
