package com.example.sashok.messanger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/**
 * Created by sashok on 14.2.17.
 */

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {
    String first_name,last_name;
    SharedPreferences mSettings;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        startService();
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar_home);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        update_form();
        myToolbar.setNavigationOnClickListener(this);
        TextView mTitle = (TextView) myToolbar.findViewById(R.id.toolbar_title_home);
        mTitle.setText(first_name+" "+last_name);
    }



    public void startService() {


        Intent intent = new Intent(getBaseContext(), FoneService.class);
        startService(intent);

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

    public void showMessages(View view){
        Intent intent=new Intent(getBaseContext(),ChatActivity.class);
        startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        finish();
    }

    public void update_form()    {
        if (mSettings==null) mSettings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        first_name=mSettings.getString(Person.COLUMN_NAME_FIRST_NAME,"");
        last_name=mSettings.getString(Person.COLUMN_NAME_LAST_NAME,"");

    }

    public void showUsers(View view) {
        Intent intent=new Intent(getBaseContext(),UsersRoom.class);
        startActivity(intent);
    }
}
