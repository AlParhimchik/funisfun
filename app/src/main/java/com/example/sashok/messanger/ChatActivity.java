package com.example.sashok.messanger;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sasho on 07.02.2017.
 */

public class ChatActivity extends AppCompatActivity implements View.OnFocusChangeListener, View.OnClickListener {
    private List<Mail> mails;
    private boolean mBound = false;
    String first_name,last_name;
    SharedPreferences mSettings;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private EditText text_input;
    public final String DB_NAME="messenger.db";
    SQLiteDatabase chatDBlocal;
     public final String CREATE_MAILS_DB="CREATE TABLE IF NOT EXISTS "+ Message.TABLE_NAME +
            " (_id integer primary key not null unique ,"+ Message.COLUMN_NAME_TEXT+" TEXT,"+ Message.COLIMN_NAME_DATE+" DATETIME not null,"
            + Message.COLUMN_NAME_ReceiveID+" INTEGER ,"+ Message.COLUMN_NAME_SenderID
            +" INTEGER, CONSTRAINT receive_key FOREIGN key("+Message.COLUMN_NAME_ReceiveID+") REFERENCES "+Person.TABLE_NAME+"("+Person._ID+") ON DELETE SET NULL," +
            "constraint send_key foreign key("+Message.COLUMN_NAME_SenderID+") REFERENCES "+Person.TABLE_NAME+"("+Person._ID+") on delete set null)";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mails=new ArrayList<Mail>();
        readData();
        mAdapter = new mailsAdapter(this,mails);
        mRecyclerView.setAdapter(mAdapter);
        text_input = (EditText) findViewById(R.id.mail_input);
        text_input.setOnFocusChangeListener(this);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        update_form();
        myToolbar.setNavigationOnClickListener(this);
        TextView mTitle = (TextView) myToolbar.findViewById(R.id.toolbar_title);
        mTitle.setText(first_name+" "+last_name);

    }

    private void readData() {
        chatDBlocal = openOrCreateDatabase(DB_NAME,
                Context.MODE_PRIVATE, null);
        chatDBlocal.execSQL(CREATE_MAILS_DB);
        if (mSettings==null) mSettings=getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String curLogin=mSettings.getString(Person.COLUMN_NAME_LOGIN,"");
        Cursor cursor=chatDBlocal.query(Message.TABLE_NAME,null,null,null,null,null,null);
        //Cursor cursor=chatDBlocal.query(Message.TABLE_NAME,null,Person.COLUMN_NAME_LOGIN+" = ?",new String[]{curLogin},null,null,null);
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
                mails.add(mail);

            }while (cursor.moveToNext());
        }
    }

    public void update_form()    {
        if (mSettings==null) mSettings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        first_name=mSettings.getString(Person.COLUMN_NAME_FIRST_NAME,"");
        last_name=mSettings.getString(Person.COLUMN_NAME_LAST_NAME,"");

    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_layout,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId()==R.id.out)
        {
            finish();
        }
        return true;
    }

    public void onFocusChange(View view, boolean b) {
        if (!b)
        {
            hideKeyboard(view);
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onClick(View v) {
        finish();
    }
}
