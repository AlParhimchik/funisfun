package com.example.sashok.messanger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    String  another_user_name;
    int cur_user_id=0;
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
    private final Context mContext = this;
    private FoneService mService;
    private boolean mConected = false;
    BroadcastReceiver receiver;
    TextView btn_send;
    RelativeLayout layout;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_layout);
        bindFoneService();
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        //layoutManager.setStackFromEnd(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
         mails=new ArrayList<>();
        readData();
        mAdapter = new mail_with_user_Adapter(this,mails);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.getLayoutManager().scrollToPosition(mails.size()-1);
        text_input = (EditText) findViewById(R.id.mail_input);
        text_input.setImeActionLabel("Send", KeyEvent.KEYCODE_ENTER);
        text_input.setOnFocusChangeListener(this);
        another_user_name=getIntent().getStringExtra(getString(R.string.NAME_ANOTHER_USER));
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        myToolbar.setNavigationOnClickListener(this);
        TextView mTitle = (TextView) myToolbar.findViewById(R.id.toolbar_title);
        mTitle.setText(another_user_name);
        layout=(RelativeLayout)findViewById(R.id.focus_layout);
        btn_send=(TextView)findViewById(R.id.send_button);
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();

            }
        });

        receiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Boolean result=intent.getBooleanExtra("result",false);
                if (result) {
                    readData();
                    mAdapter.notifyDataSetChanged();
                    mRecyclerView.getLayoutManager().scrollToPosition(mails.size()-1);
                }
                else{
                    Toast.makeText(getBaseContext(),"no internet connection",Toast.LENGTH_LONG).show();
                }

            }
        };


    }

    private void sendMessage(){
        if (mConected){
            if (!TextUtils.isEmpty(text_input.getText())) {
                hideKeyboard(layout);
                if (mSettings==null) mSettings=getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                int another_user_id=getIntent().getIntExtra(getString(R.string.ID_KEY),0);
                mService.sendMessage(text_input.getText().toString(),cur_user_id,another_user_id);
                text_input.setText("");
            }
        }
    }

     private void readData() {
        if (mSettings==null) mSettings=getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        cur_user_id=mSettings.getInt(Person._ID,0);
        chatDBlocal = openOrCreateDatabase(DB_NAME,
                Context.MODE_PRIVATE, null);
        chatDBlocal.execSQL(CREATE_MAILS_DB);
        String another_user_id=String.valueOf(getIntent().getIntExtra(getString(R.string.ID_KEY),0));
        int last_mail_id;
        if (mails.size()==0) last_mail_id=0;
        else{
            last_mail_id=mails.get(mails.size()-1).mailID;
        }
         Cursor cursor=chatDBlocal.query(Message.TABLE_NAME,null,"(("+Message.COLUMN_NAME_ReceiveID+" = ? AND "+Message.COLUMN_NAME_SenderID +" = ? ) OR ("+Message.COLUMN_NAME_ReceiveID+" = ? AND "+Message.COLUMN_NAME_SenderID+" = ? )) AND ("+Message._ID +" > ? )" ,new String[]{String.valueOf(cur_user_id),another_user_id,another_user_id,String.valueOf(cur_user_id),String.valueOf(last_mail_id)},null,null,null);
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

    @Override
    public void onBackPressed() {
        finish();
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

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            FoneService.LocalBinder binder = (FoneService.LocalBinder) service;
            mService = binder.getService();
            mConected=true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mConected = false;
        }
    };

    public void bindFoneService()   {
        Intent intent = new Intent();
        intent.setClass(mContext, FoneService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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

    @Override
    protected void onStop() {
        super.onStop();
        if (receiver!=null) unregisterReceiver(receiver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter(getString(R.string.ACTION_SEND_MESSAGE));
        registerReceiver(receiver,intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConected)
        {
            mConected=false;
            unbindService(mConnection);
        }
    }
}
