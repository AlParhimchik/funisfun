package com.example.sashok.messanger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
        mails=new ArrayList<>();
        readMailsFromDb();
    }

    private void readMailsFromDb() {
        chatDBlocal = ctx.openOrCreateDatabase(DB_NAME,
                Context.MODE_PRIVATE, null);
        chatDBlocal.execSQL(CREATE_MAILS_DB);
        chatDBlocal.execSQL(CREATE_USERS_DB);
        int cur_user_id;
        if (mSettings == null)
            mSettings = ctx.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        cur_user_id = mSettings.getInt(Person._ID, 0);

        Cursor cursor = chatDBlocal.query(Message.TABLE_NAME, null, Message.COLUMN_NAME_SenderID + " = ? OR " + Message.COLUMN_NAME_ReceiveID + " = ?", new String[]{String.valueOf(cur_user_id), String.valueOf(cur_user_id)}, null, null, Message._ID + " DESC");
        if (cursor.moveToFirst()) {
            int another_user_id;
            do {
                Mail mail = new Mail();
                mail.mailID = cursor.getInt(cursor.getColumnIndex(Message._ID));
                mail.text = cursor.getString(cursor.getColumnIndex(Message.COLUMN_NAME_TEXT));
                String time = cursor.getString(cursor.getColumnIndex(Message.COLIMN_NAME_DATE));
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                time = time.replace("T", " ");
                try {
                    mail.time = dateFormat.parse(time);
                    //mail.time=Date.valueOf(time);
                } catch (Exception e) {
                    Log.e("TAG", "Parsing ISO8601 datetime failed", e);
                }

                mail.receiveID = cursor.getInt(cursor.getColumnIndex(Message.COLUMN_NAME_ReceiveID));
                mail.senderID = cursor.getInt(cursor.getColumnIndex(Message.COLUMN_NAME_SenderID));


                if (mail.receiveID == cur_user_id) {
                    another_user_id = mail.senderID;
                } else {
                    another_user_id = mail.receiveID;
                }
                if (!contains(mails, another_user_id)) {
                    Cursor newCursor;
                    newCursor = chatDBlocal.query(Person.TABLE_NAME, null, Person._ID + " = ?", new String[]{String.valueOf(another_user_id)}, null, null, null);
                    if (newCursor.moveToFirst()) {
                        User user = new User();
                        user.id = newCursor.getInt(newCursor.getColumnIndex(Person._ID));
                        user.first_name = newCursor.getString(newCursor.getColumnIndex(Person.COLUMN_NAME_FIRST_NAME));
                        user.last_name = newCursor.getString(newCursor.getColumnIndex(Person.COLUMN_NAME_LAST_NAME));
                        user.login = newCursor.getString(newCursor.getColumnIndex(Person.COLUMN_NAME_LOGIN));
                        if (mail.receiveID==cur_user_id) mail.sender = user;
                        else{
                            mail.receiver=user;
                        }
                    }
                    mails.add(mail);
                }
            } while (cursor.moveToNext());
            cursor.close();

            Collections.sort(mails, new Comparator<Mail>() {
                public int compare(Mail mail1, Mail mail2) {
                    return mail1.mailID > mail2.mailID ? -1 : 1;
                }
            });
        }
    }

    public  boolean contains(List<Mail> mails,int user_id) {
        for (Mail mail : mails) {
            if (mail.receiveID == user_id || mail.senderID == user_id) {
                return true;
            }

        }
        return false;
    }
}


