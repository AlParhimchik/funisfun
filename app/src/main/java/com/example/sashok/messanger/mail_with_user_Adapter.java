package com.example.sashok.messanger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Created by sashok on 19.2.17.
 */

public class mail_with_user_Adapter  extends RecyclerView.Adapter<mail_with_user_Adapter.ViewHolder>  {
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


    mail_with_user_Adapter(Context ctx,List<Mail> mails){
        this.ctx=ctx;
        this.mails=mails;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String date_in_view;
        String cur_date = new SimpleDateFormat("MM-dd").format(Calendar.getInstance().getTime());
        String mail_date=new SimpleDateFormat("MM-dd").format(mails.get(position).time);
        if (cur_date.equals(mail_date)){
            Format formatter = new SimpleDateFormat("HH:mm");
            date_in_view = formatter.format(mails.get(position).time);
        }
        else {
            Format formatter = new SimpleDateFormat("MM.dd");
            date_in_view = formatter.format(mails.get(position).time);
        }
        if (mSettings==null) mSettings=ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        int curID=mSettings.getInt(Person._ID,0);


        if (curID==mails.get(position).receiveID) {
            holder.left_layout.setVisibility(View.VISIBLE);
            holder.mail_text_left.setText(mails.get(position).text);
            holder.right_layout.setVisibility(View.INVISIBLE);
            holder.date_right.setText(date_in_view);

        }
        else{
            holder.left_layout.setVisibility(View.INVISIBLE);
            holder.mail_text_right.setText(mails.get(position).text);
            holder.right_layout.setVisibility(View.VISIBLE);
            holder.date_left.setText(date_in_view);

        }
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
        public TextView mail_text_right,date_right,mail_text_left,date_left;
        public  RelativeLayout left_layout,right_layout;

        public ViewHolder(View v) {
            super(v);
            mail_text_right=(TextView)v.findViewById(R.id.mail_view_rigth);
            mail_text_left=(TextView)v.findViewById(R.id.mail_view_left);
            date_right=(TextView)v.findViewById(R.id.date_right);
            date_left=(TextView)v.findViewById(R.id.date_left);
            left_layout=(RelativeLayout)v.findViewById(R.id.layout_left);
            right_layout=(RelativeLayout)v.findViewById(R.id.layout_right);
        }
    }
}
