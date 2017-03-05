package com.example.sashok.messanger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sashok on 21.2.17.
 */

public class UserFragment extends android.support.v4.app.Fragment {
    Activity ctx;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    List<User> users;
    BroadcastReceiver receiver;
    public final String DB_NAME="messenger.db";
    SQLiteDatabase chatDBlocal;
    public final String CREATE_USERS_DB="CREATE TABLE IF NOT EXISTS "+ Person.TABLE_NAME +
            " (_id integer primary key unique,"+ Person.COLUMN_NAME_PASSWORD+" text not null,"+ Person.COLUMN_NAME_LOGIN+" text unique not null,"
            + Person.COLUMN_NAME_FIRST_NAME+","+ Person.COLUMN_NAME_LAST_NAME+")";
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.users_room, container, false);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.my_recycler_view_users);
        mLayoutManager = new LinearLayoutManager(ctx);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter=new usersAdapter(ctx,users);
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }
    public UserFragment()        {
        }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx=getActivity();
        users=new ArrayList<User>();
        readUsersFromDb();
        receiver=new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction()==getString(R.string.ACTION_NEW_USER_ADDED))
                {
                    readUsersFromDb();
                    mAdapter.notifyDataSetChanged();

                }

            }

        };

    }

    private void readUsersFromDb() {
        chatDBlocal = ctx.openOrCreateDatabase(DB_NAME,
                Context.MODE_PRIVATE, null);
        chatDBlocal.execSQL(CREATE_USERS_DB);
        int last_user_id;
        if (users.size()==0) last_user_id=0;
        else{
            last_user_id=users.get(users.size()-1).id;
        }

        Cursor cursor=chatDBlocal.query(Person.TABLE_NAME,null,Person._ID+" > ?",new String[]{String.valueOf(last_user_id)},null,null,null);
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
    public void onStop() {
        super.onStop();
        if (receiver!=null) ctx.unregisterReceiver(receiver);
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter(getString(R.string.ACTION_NEW_USER_ADDED));
        ctx.registerReceiver(receiver,intentFilter);
    }

}
