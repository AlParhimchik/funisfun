package com.example.sashok.messanger;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by sashok on 14.2.17.
 */

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {
    String first_name,last_name;
    SharedPreferences mSettings;
    ViewPager viewPager;
    PageAdapter adapter;
    BroadcastReceiver receiver;
    ProgressBar progressBar;
    LinearLayout mainLayout;
    TabLayout tabLayout;
    Intent service;
    Boolean mail_update=false,user_update=false;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        startService();
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar_home);
        mainLayout=(LinearLayout) findViewById(R.id.home_layout);
        progressBar=(ProgressBar) findViewById(R.id.progressBar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        update_form();
        startProgressBar();
//        myToolbar.setNavigationOnClickListener(this);
        TextView mTitle = (TextView) myToolbar.findViewById(R.id.toolbar_title_home);
        mTitle.setText(first_name+" "+last_name);
        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        receiver=new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction()==getString(R.string.ACTION_STORE_MAILS)){
                    mail_update=true;
                }
                if (intent.getAction()==getString(R.string.ACTION_STORE_USERS)){
                    user_update=true;
                }
                if (mail_update && user_update){
                    mail_update=user_update=false;
                    progressBar.setVisibility(View.INVISIBLE);
                    startWithViewPager();
                }
            }

        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i("TAG","hey");
    }

    public void startWithViewPager(){
        mainLayout.setVisibility(View.VISIBLE);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        adapter = new PageAdapter
                (getSupportFragmentManager(),this);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void startProgressBar() {

        progressBar.setVisibility(View.VISIBLE);
        mainLayout.setVisibility(View.INVISIBLE);

    }

    public void startService() {


       service = new Intent(getBaseContext(), FoneService.class);
        startService(service);

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
    public boolean onOptionsItemSelected(MenuItem item)    {
        if (item.getItemId()==R.id.out)
        {
            SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor=sharedPref.edit();
            editor.putBoolean("isSaved",false);
            editor.apply();
            finish();
        }
        return true;
    }

    public void update_form()    {
        if (mSettings==null) mSettings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        first_name=mSettings.getString(Person.COLUMN_NAME_FIRST_NAME,"");
        last_name=mSettings.getString(Person.COLUMN_NAME_LAST_NAME,"");

    }


    @Override
    public void onClick(View v) {
        if (mSettings==null) mSettings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor=mSettings.edit();
        editor.putBoolean(getString(R.string.SAVE_KEY),false);
        editor.apply();
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

        IntentFilter intentFilter = new IntentFilter(getString(R.string.ACTION_STORE_USERS));
        intentFilter.addAction(getString(R.string.ACTION_STORE_MAILS));
        registerReceiver(receiver,intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        boolean result=stopService(new Intent(getBaseContext(),FoneService.class));
        Log.i("TAG","Destroy home"+String.valueOf(result));
    }
}
