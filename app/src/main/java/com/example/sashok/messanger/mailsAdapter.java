package com.example.sashok.messanger;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sashok on 19.2.17.
 */

public class mailsAdapter  extends RecyclerView.Adapter<mailsAdapter.ViewHolder>  {
    private List<Mail> mails;
    SharedPreferences mSettings;
    private  Context ctx;
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

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.mail_list, parent, false);
        ViewHolder pvh = new ViewHolder(v);
        return pvh;
    }

    mailsAdapter(Context ctx,List<Mail> mails){
          this.ctx=ctx;
          this.mails=mails;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (mSettings==null) mSettings=ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String curLogin=mSettings.getString(Person.COLUMN_NAME_LOGIN,"");
        chatDBlocal = ctx.openOrCreateDatabase(DB_NAME,
                Context.MODE_PRIVATE, null);
        chatDBlocal.execSQL(CREATE_USERS_DB);
        Cursor cursor=chatDBlocal.query(Person.TABLE_NAME,new String[]{Person._ID},Person.COLUMN_NAME_LOGIN+"=?",new String[]{curLogin},null,null,null);
        int curID=0;
        if (cursor.moveToFirst()){
            curID=cursor.getInt(cursor.getColumnIndex(Person._ID));
        }

        if (curID==mails.get(position).receiveID) {
            holder.layout_left.setVisibility(View.INVISIBLE);
            holder.mail_right.setText(mails.get(position).text);
            holder.layout_right.setVisibility(View.VISIBLE);

        }
        else{
            holder.layout_right.setVisibility(View.INVISIBLE);
            holder.layout_left.setVisibility(View.VISIBLE);
            holder.mail_left.setText(mails.get(position).text);
        }

        cursor.close();

    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public int getItemCount() {
        return mails.size();
    }

    public  class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mail_left,mail_right;
        public RelativeLayout layout_left,layout_right;
        public ViewHolder(View v) {
            super(v);
            mail_left=(TextView)v.findViewById(R.id.mail_view_left);
            mail_right=(TextView)v.findViewById(R.id.mail_view_rigth);
            layout_left=(RelativeLayout)v.findViewById(R.id.layout_left);
            layout_right=(RelativeLayout)v.findViewById(R.id.layout_right);


        }
    }
}