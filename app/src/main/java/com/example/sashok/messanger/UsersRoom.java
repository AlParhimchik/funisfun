package com.example.sashok.messanger;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sashok on 21.2.17.
 */

public class UsersRoom extends AppCompatActivity implements View.OnClickListener {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    List<User> users;
    public final String DB_NAME="messenger.db";
    SQLiteDatabase chatDBlocal;
    public final String CREATE_USERS_DB="CREATE TABLE IF NOT EXISTS "+ Person.TABLE_NAME +
            " (_id integer primary key unique,"+ Person.COLUMN_NAME_PASSWORD+" text not null,"+ Person.COLUMN_NAME_LOGIN+" text unique not null,"
            + Person.COLUMN_NAME_FIRST_NAME+","+ Person.COLUMN_NAME_LAST_NAME+")";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.users_room);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar_users);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        myToolbar.setNavigationOnClickListener(this);
        TextView mTitle = (TextView) myToolbar.findViewById(R.id.toolbar_title_users);
        mTitle.setText(getString(R.string.name_users));
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view_users);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        readUsersFromDb();
        mAdapter=new usersAdapter(this,users);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void readUsersFromDb() {
        users=new ArrayList<User>();
        chatDBlocal = openOrCreateDatabase(DB_NAME,
                Context.MODE_PRIVATE, null);
        chatDBlocal.execSQL(CREATE_USERS_DB);
        Cursor cursor=chatDBlocal.query(Person.TABLE_NAME,null,null,null,null,null,null);
        if (cursor.moveToFirst())
            do{
                User user=new User();
                user.id=cursor.getInt(cursor.getColumnIndex(Person._ID));
                user.first_name=cursor.getString(cursor.getColumnIndex(Person.COLUMN_NAME_FIRST_NAME));
                user.last_name=cursor.getString(cursor.getColumnIndex(Person.COLUMN_NAME_LAST_NAME));
                user.login=cursor.getString(cursor.getColumnIndex(Person.COLUMN_NAME_LOGIN));
                users.add(user);

            }while(cursor.moveToNext());
        cursor.close();


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

    @Override
    public void onClick(View v) {
        finish();
    }




}
