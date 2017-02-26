package com.example.sashok.messanger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by sashok on 21.2.17.
 */

public class MailFragment extends android.support.v4.app.Fragment {
    private List<Mail> mails;
    Activity ctx;
    SharedPreferences mSettings;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    public final String DB_NAME="messenger.db";
    SQLiteDatabase chatDBlocal;
    public final String CREATE_USERS_DB="CREATE TABLE IF NOT EXISTS "+ Person.TABLE_NAME +
            " (_id integer primary key unique,"+ Person.COLUMN_NAME_PASSWORD+" text not null,"+ Person.COLUMN_NAME_LOGIN+" text unique not null,"
            + Person.COLUMN_NAME_FIRST_NAME+","+ Person.COLUMN_NAME_LAST_NAME+")";
    public final String CREATE_MAILS_DB="CREATE TABLE IF NOT EXISTS "+ Message.TABLE_NAME +
            " (_id integer primary key not null unique ,"+ Message.COLUMN_NAME_TEXT+" TEXT,"+ Message.COLIMN_NAME_DATE+" DATETIME not null,"
            + Message.COLUMN_NAME_ReceiveID+" INTEGER ,"+ Message.COLUMN_NAME_SenderID
            +" INTEGER, CONSTRAINT receive_key FOREIGN key("+Message.COLUMN_NAME_ReceiveID+") REFERENCES "+Person.TABLE_NAME+"("+Person._ID+") ON DELETE SET NULL," +
            "constraint send_key foreign key("+Message.COLUMN_NAME_SenderID+") REFERENCES "+Person.TABLE_NAME+"("+Person._ID+") on delete set null)";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.all_mails_room,container,false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view_all_mails);
        mLayoutManager = new LinearLayoutManager(ctx);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new mailsAdapter(ctx,mails);
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }
    public MailFragment(){}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.ctx=getActivity();
        readMailsFromDb();
    }

    private void readMailsFromDb() {
        mails=new ArrayList<>();
        chatDBlocal = ctx.openOrCreateDatabase(DB_NAME,
                Context.MODE_PRIVATE, null);
        chatDBlocal.execSQL(CREATE_MAILS_DB);
        chatDBlocal.execSQL(CREATE_USERS_DB);
        int curID=0;
        if (mSettings==null) mSettings=ctx.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String curLogin=mSettings.getString(Person.COLUMN_NAME_LOGIN,"");
        Cursor cursor=chatDBlocal.query(Person.TABLE_NAME,new String[]{Person._ID},Person.COLUMN_NAME_LOGIN+" = ?",new String[]{curLogin},null,null,null);
        if (cursor.moveToFirst()){
            curID=cursor.getInt(cursor.getColumnIndex(Person._ID));
        }

        cursor.close();
        cursor=chatDBlocal.query(Message.TABLE_NAME,null,Message.COLUMN_NAME_ReceiveID+" = ? OR "+Message.COLUMN_NAME_SenderID+" = ?",new String[]{String.valueOf(curID),String.valueOf(curID)},null,null,null);
        if (cursor.moveToFirst()){
            do {
                Mail mail=new Mail();
                mail.mailID=cursor.getInt(cursor.getColumnIndex(Message._ID));
                mail.text=cursor.getString(cursor.getColumnIndex(Message.COLUMN_NAME_TEXT));
                String time= cursor.getString(cursor.getColumnIndex(Message.COLIMN_NAME_DATE));
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                time=time.replace("T"," ");
                try {
                    mail.time = dateFormat.parse(time);
                    //mail.time=Date.valueOf(time);
                } catch (Exception e) {
                    Log.e("TAG", "Parsing ISO8601 datetime failed", e);
                }

                mail.receiveID=cursor.getInt(cursor.getColumnIndex(Message.COLUMN_NAME_ReceiveID));
                mail.senderID=cursor.getInt(cursor.getColumnIndex(Message.COLUMN_NAME_SenderID));
                Cursor newCursor;
                if ((mail.receiveID==curID)){
                    newCursor=chatDBlocal.query(Person.TABLE_NAME,null,Person._ID+" = ?",new String[]{String.valueOf(mail.senderID)},null,null,null);
                }
                else{
                    newCursor=chatDBlocal.query(Person.TABLE_NAME,null,Person._ID+" = ?",new String[]{String.valueOf(mail.receiveID)},null,null,null);

                }
                if (newCursor.moveToFirst()){
                    User user=new User();
                    user.id=newCursor.getInt(newCursor.getColumnIndex(Person._ID));
                    user.first_name=newCursor.getString(newCursor.getColumnIndex(Person.COLUMN_NAME_FIRST_NAME));
                    user.last_name=newCursor.getString(newCursor.getColumnIndex(Person.COLUMN_NAME_LAST_NAME));
                    user.login=newCursor.getString(newCursor.getColumnIndex(Person.COLUMN_NAME_LOGIN));
                    if (mail.receiveID==curID) mail.sender=user;
                    else mail.receiver=user;
                }
                mails.add(mail);

            }while (cursor.moveToNext());
            cursor.close();
        }
    }
}


