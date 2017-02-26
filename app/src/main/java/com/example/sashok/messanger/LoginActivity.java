package com.example.sashok.messanger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by sasho on 06.02.2017.
 */

public class LoginActivity extends AppCompatActivity implements  View.OnFocusChangeListener{
    SharedPreferences mSettings;
    EditText login_form,password_form;
    BroadcastReceiver myReciever;
    ProgressDialog progressDialog;
    private final Context mContext = this;
    private FoneService mService;
    private boolean mConected = false;

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            FoneService.LocalBinder binder = (FoneService.LocalBinder) service;
            mService = binder.getService();
//            mBound = true;
            loadCookie();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mConected = false;
        }
    };

    @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        bindFoneService();
        myReciever=new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent) {
                progressDialog.dismiss();
                    Boolean result = intent.getBooleanExtra("result", false);
                    if (result) {
                        Intent chatIntent = new Intent(getBaseContext(), HomeActivity.class);
                        startActivity(chatIntent);
                    } else {
                        Toast.makeText(getBaseContext(), result.toString(), Toast.LENGTH_SHORT).show();
                    }
                }

        };

        login_form= (EditText) findViewById(R.id.login_form);
        password_form = (EditText) findViewById(R.id.password_form);
        login_form.setOnFocusChangeListener(this);
        password_form.setOnFocusChangeListener(this);
    }

    public void bindFoneService()
    {
        Intent intent = new Intent();
        intent.setClass(mContext, FoneService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void loadCookie()
    {
        mSettings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        boolean isSaved=mSettings.getBoolean(getString(R.string.SAVE_KEY),false);
        if (isSaved)
        {
            connect();
            make_sing_in();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (myReciever!=null) unregisterReceiver(myReciever);
    }

    public  void sing_in(View view)     {
        if (!TextUtils.isEmpty(login_form.getText().toString()) && !TextUtils.isEmpty(password_form.getText().toString()))
        {
            if (mSettings==null) getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor=mSettings.edit();
            editor.putString(Person.COLUMN_NAME_LOGIN,login_form.getText().toString());
            editor.putString(Person.COLUMN_NAME_PASSWORD,password_form.getText().toString());
            editor.apply();
            if (mConected==false) connect();
            make_sing_in();
        }


    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter(getString(R.string.ACTION_SING_IN));
        intentFilter.addAction(getString(R.string.ACTION_SING_UP));
        registerReceiver(myReciever,intentFilter);
    }

    public void sing_up(View view)     {
        RegFragmentDialog dialog= new RegFragmentDialog(this, new RegFragmentDialog.onRegistredListener() {
            @Override
            public void onButtonClick() {
//                if (mConected==false) connect();
                progressDialog=new ProgressDialog(LoginActivity.this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(false);
                progressDialog.setMessage("Registrating...");
                progressDialog.show();
                mService.sing_up();
                }


        });
        dialog.getWindow().getAttributes().windowAnimations=R.style.RegistrationDialogAnimation;
        dialog.show();

    }

    @Override
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
    protected void onDestroy() {
        super.onDestroy();
        Log.i("TAG","Destroy");
        if (mConected)
        {
            mConected=false;
            unbindService(mConnection);
        }
    }

    public void connect()
    {
        if (mService!=null)
        {
            mService.connect();
            mConected=true;
        }

    }

    public  void make_sing_in()        {

        if (mConected)
        {
            progressDialog=new ProgressDialog(LoginActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Authenticating...");
            progressDialog.show();
            mService.sing_in();
        }
        else Toast.makeText(getBaseContext(),"unable to connect", Toast.LENGTH_LONG).show();
    }


}

