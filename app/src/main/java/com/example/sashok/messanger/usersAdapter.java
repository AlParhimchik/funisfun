package com.example.sashok.messanger;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Created by sashok on 21.2.17.
 */

public class usersAdapter extends RecyclerView.Adapter<usersAdapter.UsersViewHolder>
    {
        List<User> users;
        Context ctx;

    @Override
    public UsersViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.users_list, parent, false);
        usersAdapter.UsersViewHolder pvh = new usersAdapter.UsersViewHolder(v);
        return pvh;
    }

    @Override
    public void onBindViewHolder(UsersViewHolder holder, int position) {
        User user=users.get(position);
        holder.user_name.setText(user.first_name+" "+user.last_name);
        final int id=user.id;
        final String name=user.first_name+" "+user.last_name;
        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(ctx,ChatActivity.class);
                intent.putExtra(ctx.getString(R.string.ID_KEY),id);
                intent.putExtra(ctx.getString(R.string.NAME_ANOTHER_USER),name);
                ctx.startActivity(intent);
            }
        });

    }

    public usersAdapter(Context ctx,List<User> users){
        this.users=users;
        this.ctx=ctx;
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public  class UsersViewHolder extends RecyclerView.ViewHolder {
        public TextView user_name;
        public ImageView icon;
        public RelativeLayout layout;
        public UsersViewHolder(View v) {
            super(v);
            user_name=(TextView)v.findViewById(R.id.user_name);
            icon=(ImageView)v.findViewById(R.id.user_icon);
            layout=(RelativeLayout)v.findViewById(R.id.layout_users);
        }
    }
}
