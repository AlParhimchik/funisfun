package com.example.sashok.messanger;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.text.Editable;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutionException;

import microsoft.aspnet.signalr.client.ConnectionState;
import microsoft.aspnet.signalr.client.Credentials;
import microsoft.aspnet.signalr.client.Platform;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.StateChangedCallback;
import microsoft.aspnet.signalr.client.http.Request;
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.transport.ClientTransport;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;

/**
 * Created by sasho on 06.02.2017.
 */

public class FoneService extends Service {
    private HubConnection mHubConnection;
    private HubProxy mHubProxy;
    OpenConnection mOpenConnection;
    SharedPreferences mSettings;
    User cur_user=new User();
    Boolean is_binded=true;
    Boolean isConServer=false;
    BroadcastReceiver broadcastReceiver;
    private final IBinder mBinder = new FoneService.LocalBinder(); // Binder given to clients
    public final String CREATE_USERS_DB="CREATE TABLE IF NOT EXISTS "+ Person.TABLE_NAME +
            " (_id integer primary key unique,"+ Person.COLUMN_NAME_PASSWORD+" text not null,"+ Person.COLUMN_NAME_LOGIN+" text unique not null,"
            + Person.COLUMN_NAME_FIRST_NAME+","+ Person.COLUMN_NAME_LAST_NAME+")";

    SQLiteDatabase chatDBlocal;
    public final String DB_NAME="messenger.db";
    public final String CREATE_MAILS_DB="CREATE TABLE IF NOT EXISTS "+ Message.TABLE_NAME +
            " (_id integer primary key not null unique ,"+ Message.COLUMN_NAME_TEXT+" TEXT,"+ Message.COLIMN_NAME_DATE+" DATETIME not null,"
            + Message.COLUMN_NAME_ReceiveID+" INTEGER ,"+ Message.COLUMN_NAME_SenderID
            +" INTEGER, CONSTRAINT receive_key FOREIGN key("+Message.COLUMN_NAME_ReceiveID+") REFERENCES "+Person.TABLE_NAME+"("+Person._ID+") ON DELETE SET NULL," +
            "constraint send_key foreign key("+Message.COLUMN_NAME_SenderID+") REFERENCES "+Person.TABLE_NAME+"("+Person._ID+") on delete set null)";

    @Override
    public void onCreate() {
        super.onCreate();

    }

    public  FoneService(){}

    @Override
    public void onDestroy() {
        Log.i("TAG","onDestroyService");
        if (mHubConnection!=null) mHubConnection.stop();
        if (broadcastReceiver!=null) unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        broadcastReceiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {


                    //if (mHubConnection!=null) mHubConnection.stop();

            }
        };
        is_binded = false;
        //connect();
//        copy_db();
        IntentFilter filter=new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(broadcastReceiver, filter);

        if (isActiveNetwork()) {
            update_form();
            selectUserssTask userOp=new selectUserssTask();
            userOp.execute();
            selectMailsTask mailOp=new selectMailsTask();
            mailOp.execute();

        }
        else{
            Intent mails_intent=new Intent();
            mails_intent.setAction(getString(R.string.ACTION_STORE_MAILS));
            sendBroadcast(mails_intent);
            Intent user_intent=new Intent();
            user_intent.setAction(getString(R.string.ACTION_STORE_USERS));
            sendBroadcast(user_intent);
        }
        //        if (isActiveNetwork()) {
//            connect();
//        }
        return START_NOT_STICKY;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("TAG","onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {

         return mBinder;
    }

    public class LocalBinder extends Binder {
        public FoneService getService() {
            // Return this instance of SignalRService so clients can call public methods
            return FoneService.this;
        }
    }

    public class OpenConnection extends AsyncTask<Void,Void,Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            Boolean result;
            final String login = cur_user.login.toString();
            Platform.loadPlatformComponent(new AndroidPlatformComponent());
            Credentials credentials = new Credentials() {
                @Override
                public void prepareRequest(Request request) {
                    request.addHeader("login", login);

                }
            };
            Log.i("TAG", "start connection...");
            String serverUrl = getString(R.string.server_name);
            mHubConnection = new HubConnection(serverUrl);
            mHubConnection.setCredentials(credentials);
            String SERVER_HUB_CHAT = "ChatHub";
            mHubProxy = mHubConnection.createHubProxy(SERVER_HUB_CHAT);
            ClientTransport clientTransport = new ServerSentEventsTransport(mHubConnection.getLogger());
            final SignalRFuture<Void> signalRFuture = mHubConnection.start(clientTransport);
            try {
                signalRFuture.get();
                result = true;
            } catch (InterruptedException | ExecutionException e) {
                result = false;

            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            isConServer = result;
            if (result) {
                mHubConnection.stateChanged(new StateChangedCallback() {
                    @Override
                    public void stateChanged(ConnectionState connectionState, ConnectionState connectionState1) {
                        Log.i("SignalR", connectionState.name() + "->" + connectionState1.name());
                    }
                });
                mHubConnection.reconnecting(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("TAG", "reconnected");
                        isConServer = false;
                    }
                });
                mHubConnection.connected(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("TAG", "connected");

                    }
                });
                mHubConnection.reconnected(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("TAG", "reconnected");
                        isConServer = false;
                    }
                });
                mHubConnection.closed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("TAG", "closed");
                        isConServer = false;
                        //mHubProxy = null;
//                        if (isActiveNetwork())
//                        mOpenConnection = new OpenConnection();
//                        mOpenConnection.execute();

                    }
                });
                mHubProxy.on("UpdateMessege", new SubscriptionHandler1<JsonElement>() {
                            @Override
                            public void run(final JsonElement je) {
                                Boolean result = true;
                            }
                        }
                    ,JsonElement.class);

                mHubProxy.on("updateUsers", new SubscriptionHandler1<JsonElement>() {
                            @Override
                            public void run(final JsonElement je) {
                                try {

                                    JsonObject jo;
                                    ContentValues insertValues;
                                    jo = je.getAsJsonObject();
                                    insertValues = new ContentValues();
                                    insertValues.put(Person._ID, jo.get("Id").getAsInt());
                                    insertValues.put(Person.COLUMN_NAME_FIRST_NAME, jo.get("FirstName").getAsString());
                                    insertValues.put(Person.COLUMN_NAME_LAST_NAME, jo.get("LastName").getAsString());
                                    chatDBlocal = openOrCreateDatabase(DB_NAME,
                                            Context.MODE_PRIVATE, null);
                                    chatDBlocal.execSQL(CREATE_USERS_DB);
                                    // chatDBlocal.delete(Message.TABLE_NAME,null,null);
                                    chatDBlocal.insert(Person.TABLE_NAME, null, insertValues);
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Intent intent=new Intent(getString(R.string.ACTION_NEW_USER_ADDED));
                                sendBroadcast(intent);
                            }
                        }
                        ,JsonElement.class);

                mHubProxy.on("UpdateMessage", new SubscriptionHandler1<JsonElement>() {
                    @Override
                    public void  run(final JsonElement je) {
                        Boolean result=true;
                        if (je==null) result=false;
                        else {
                            ContentValues insertValues;
                            try {
                                JsonObject jo=je.getAsJsonObject();
                                insertValues = new ContentValues();
                                insertValues.put(Message._ID, jo.get("Id").getAsInt());
                                insertValues.put(Message.COLIMN_NAME_DATE, jo.get("Time").getAsString());
                                if (!jo.get("ReceiveID").isJsonNull())
                                    insertValues.put(Message.COLUMN_NAME_ReceiveID, jo.get("ReceiveID").getAsInt());
                                if (!jo.get("SenderID").isJsonNull())
                                    insertValues.put(Message.COLUMN_NAME_SenderID, jo.get("SenderID").getAsInt());
                                insertValues.put(Message.COLUMN_NAME_TEXT, jo.get("Text").getAsString());
                                chatDBlocal = openOrCreateDatabase(DB_NAME,
                                        Context.MODE_PRIVATE, null);
                                chatDBlocal.execSQL("pragma foreign_keys = on");
                                chatDBlocal.execSQL(CREATE_MAILS_DB);
                                // chatDBlocal.delete(Message.TABLE_NAME,null,null);
                                chatDBlocal.insert(Message.TABLE_NAME, null, insertValues);
                            } catch (Exception e) {
                                e.printStackTrace();
                                result=false;
                            }
                        }
                        Intent intent=new Intent();
                        intent.putExtra("result",result);
                        intent.setAction(getString(R.string.ACTION_SEND_MESSAGE));
                        sendBroadcast(intent);

                    }
                },JsonElement.class);
            }
        }
    }

    public void sendMessage(String text, int curID, int another_user_id) {
        if (isActiveNetwork()) {
            update_form();
            SendMessageTask send_mes = new SendMessageTask();
            ContentValues values=new ContentValues();
            values.put("text",text);
            values.put("sender_id",curID);
            values.put("receiver_id",another_user_id);
            send_mes.execute(values);
        }
        else{
            Intent intent=new Intent();
            intent.putExtra("result",false);
            intent.setAction(getString(R.string.ACTION_SEND_MESSAGE));
            sendBroadcast(intent);

        }

    }

    public  class SendMessageTask extends AsyncTask<ContentValues,Void,Boolean> {

        @Override
        protected Boolean doInBackground(ContentValues[] objects) {
            ContentValues values=objects[0];
            String text = values.getAsString("text");
            int cur_id=values.getAsInteger("sender_id");
            int another_user_id=values.getAsInteger("receiver_id");
            JsonObject jo;
            final String SERVER_METHOD_SEND = "NewMessage";
            try {
                while (isConServer==false){}
                jo = mHubProxy.invoke(JsonObject.class, SERVER_METHOD_SEND, cur_id,another_user_id,text).get();
            }
            catch (InterruptedException | ExecutionException e) {
                jo=null;
            }
            if (jo==null) return false;
            ContentValues insertValues;
            try {
                insertValues = new ContentValues();
                insertValues.put(Message._ID, jo.get("Id").getAsInt());
                insertValues.put(Message.COLIMN_NAME_DATE, jo.get("Time").getAsString());
                if (!jo.get("ReceiveID").isJsonNull()) insertValues.put(Message.COLUMN_NAME_ReceiveID, jo.get("ReceiveID").getAsInt());
                if (!jo.get("SenderID").isJsonNull())  insertValues.put(Message.COLUMN_NAME_SenderID, jo.get("SenderID").getAsInt());
                insertValues.put(Message.COLUMN_NAME_TEXT, jo.get("Text").getAsString());
                chatDBlocal = openOrCreateDatabase(DB_NAME,
                        Context.MODE_PRIVATE, null);
                chatDBlocal.execSQL("pragma foreign_keys = on");
                chatDBlocal.execSQL(CREATE_MAILS_DB);
                // chatDBlocal.delete(Message.TABLE_NAME,null,null);
                chatDBlocal.insert(Message.TABLE_NAME, null, insertValues);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return  true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            Intent intent=new Intent();
            intent.putExtra("result",result);
            intent.setAction(getString(R.string.ACTION_SEND_MESSAGE));
            sendBroadcast(intent);
        }
    }

    public void sing_in()     {
        if (isActiveNetwork()) {
            update_form();
            SingInTask sing_in = new SingInTask();
            sing_in.execute();
        }
        else {
            Intent intent=new Intent();
            intent.putExtra("result",false);
            intent.setAction(getString(R.string.ACTION_SING_IN));
            sendBroadcast(intent);
        }
    }

    public  class SingInTask extends AsyncTask<Void,Void,JsonObject> {

        @Override
        protected JsonObject doInBackground(Void[] objects) {
            JsonObject result;
            final String SERVER_METHOD_SEND = "SingIn";
            try {
                while (isConServer==false){}
                result = mHubProxy.invoke(JsonObject.class, SERVER_METHOD_SEND, cur_user.login,cur_user.password).get();
            }
            catch (InterruptedException | ExecutionException e) {
                result=null;
            }
          return result;
         }

        @Override
        protected void onPostExecute(JsonObject jo) {
            super.onPostExecute(jo);
            Intent intent=new Intent();
            if (mSettings==null) getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor=mSettings.edit();
            if (jo==null || jo.isJsonNull()) {
                intent.putExtra("result",false);
                editor.putBoolean(getString(R.string.SAVE_KEY),false);
            }
            else {
                intent.putExtra("result",true);
                editor.putInt(Person._ID, jo.get("Id").getAsInt());
                editor.putString(Person.COLUMN_NAME_FIRST_NAME, jo.get("FirstName").getAsString());
                editor.putString(Person.COLUMN_NAME_LAST_NAME, jo.get("LastName").getAsString());
                //editor.putString(Person.COLUMN_NAME_LOGIN, jo.get("Login").getAsString());
                editor.putBoolean(getString(R.string.SAVE_KEY),true);
            }
            editor.apply();

            intent.setAction(getString(R.string.ACTION_SING_IN));
            sendBroadcast(intent);
        }
    }

    public  void sing_up()    {
        if (isActiveNetwork()) {
            update_form();
            SingUpTask sing_up = new SingUpTask();
            sing_up.execute();
        }
        else {
            Intent intent=new Intent();
            intent.putExtra("result",false);
            intent.setAction(getString(R.string.ACTION_SING_UP));
            sendBroadcast(intent);
        }

    }

    public  class SingUpTask extends AsyncTask<Void,Void,JsonObject> {

        @Override
        protected JsonObject doInBackground(Void[] objects) {
            JsonObject result;
            final String SERVER_METHOD_SEND = "addNewUser";
            try {

                while (isConServer==false){}
                result = mHubProxy.invoke(JsonObject.class, SERVER_METHOD_SEND, cur_user.first_name,cur_user.login,cur_user.password,cur_user.last_name).get();
            }
            catch (InterruptedException| ExecutionException e) {
                result=null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(JsonObject jo) {
            super.onPostExecute(jo);
            if (mSettings==null) getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor=mSettings.edit();
            Intent intent=new Intent();
            if (jo==null || jo.isJsonNull()) {
                intent.putExtra("result",false);
                editor.putBoolean(getString(R.string.SAVE_KEY),false);
            }
            else {
                intent.putExtra("result",true);
                editor.putInt(Person._ID, jo.get("Id").getAsInt());
                editor.putBoolean(getString(R.string.SAVE_KEY),true);
            }
            editor.apply();
            intent.setAction(getString(R.string.ACTION_SING_UP));
            sendBroadcast(intent);
        }
    }

    public void update_form()    {
        Log.i("TAG","update_form");
       if (mSettings==null) mSettings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String last_login=cur_user.login;
        cur_user.login=mSettings.getString(Person.COLUMN_NAME_LOGIN,"");
        cur_user.password=mSettings.getString(Person.COLUMN_NAME_PASSWORD,"");
        cur_user.first_name=mSettings.getString(Person.COLUMN_NAME_FIRST_NAME,"");
        cur_user.last_name=mSettings.getString(Person.COLUMN_NAME_LAST_NAME,"");
    if (isConServer && last_login==cur_user.login);
        else{
            if (mHubConnection!=null) {
                mHubConnection.stop();
                //mHubProxy.removeSubscription("UpdateMessage");
            }
            isConServer=false;
            mOpenConnection=new OpenConnection();
            mOpenConnection.execute();
        }
    }

    public boolean isActiveNetwork(){
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        Boolean hasInternetCon;
        if (activeNetwork!=null) hasInternetCon = activeNetwork.isConnectedOrConnecting();
        else hasInternetCon=false;
        return hasInternetCon;
    }

    public class selectMailsTask extends AsyncTask<Object, Object, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            int lastID = 0;
            JsonArray ja;
            chatDBlocal = openOrCreateDatabase(DB_NAME,
                    Context.MODE_PRIVATE, null);
            chatDBlocal.execSQL(CREATE_MAILS_DB);
            Cursor cursor = chatDBlocal.rawQuery("SELECT _id from mails ORDER BY _id DESC LIMIT 1;", null);
            if (cursor.moveToFirst()) {
                lastID = cursor.getInt(cursor.getColumnIndex(Message._ID));

            }
            cursor.close();
            //update_form();
            final String SERVER_METHOD_SEND = "selectMails";
            try {
                while (isConServer == false) {
                }
                ja = mHubProxy.invoke(JsonArray.class, SERVER_METHOD_SEND, cur_user.login, lastID).get();
            } catch (InterruptedException | ExecutionException e) {
                ja = null;
            }
            JsonObject jo;
            ContentValues insertValues;
            if (ja != null) {
                for (final JsonElement jsonElement : ja) {
                    try {
                        jo = jsonElement.getAsJsonObject();
                        insertValues = new ContentValues();
                        insertValues.put(Message._ID, jo.get("Id").getAsInt());
                        insertValues.put(Message.COLIMN_NAME_DATE, jo.get("Time").getAsString());
                        if (!jo.get("ReceiveID").isJsonNull()) insertValues.put(Message.COLUMN_NAME_ReceiveID, jo.get("ReceiveID").getAsInt());
                        if (!jo.get("SenderID").isJsonNull())  insertValues.put(Message.COLUMN_NAME_SenderID, jo.get("SenderID").getAsInt());
                        insertValues.put(Message.COLUMN_NAME_TEXT, jo.get("Text").getAsString());
                        chatDBlocal = openOrCreateDatabase(DB_NAME,
                                Context.MODE_PRIVATE, null);
                        chatDBlocal.execSQL("pragma foreign_keys = on");
                        chatDBlocal.execSQL(CREATE_MAILS_DB);
                        // chatDBlocal.delete(Message.TABLE_NAME,null,null);
                        chatDBlocal.insert(Message.TABLE_NAME, null, insertValues);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void object) {
            super.onPostExecute(object);
            Intent intent=new Intent();
            intent.setAction(getString(R.string.ACTION_STORE_MAILS));
            sendBroadcast(intent);

        }
    }

    public class selectUserssTask extends  AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            int lastID=0;
            JsonArray ja;
            chatDBlocal = openOrCreateDatabase(DB_NAME,
                    Context.MODE_PRIVATE, null);
            chatDBlocal.execSQL(CREATE_USERS_DB);
            Cursor cursor=chatDBlocal.rawQuery("SELECT _id from persons ORDER BY _id DESC LIMIT 1;",null);
            if (cursor.moveToFirst()) {
                lastID = cursor.getInt(cursor.getColumnIndex(Person._ID));

            }
            cursor.close();
            //update_form();
            final String SERVER_METHOD_SEND = "selectUsers";
            try {
                while (mHubProxy==null){}
                ja = mHubProxy.invoke(JsonArray.class, SERVER_METHOD_SEND,lastID).get();
            }
            catch (InterruptedException| ExecutionException e) {
                ja=null;
                e.printStackTrace();
            }
            JsonObject jo;
            ContentValues insertValues;
            if (ja!=null){
                for(final JsonElement jsonElement : ja){
                    try {
                        jo=jsonElement.getAsJsonObject();
                        insertValues = new ContentValues();
                        insertValues.put(Person._ID, jo.get("Id").getAsInt());
                        insertValues.put(Person.COLUMN_NAME_FIRST_NAME, jo.get("FirstName").getAsString());
                        insertValues.put(Person.COLUMN_NAME_LAST_NAME, jo.get("LastName").getAsString());
                        insertValues.put(Person.COLUMN_NAME_LOGIN, jo.get("Login").getAsString());
                        insertValues.put(Person.COLUMN_NAME_PASSWORD, jo.get("Password").getAsString());
                        chatDBlocal = openOrCreateDatabase(DB_NAME,
                                Context.MODE_PRIVATE, null);
                        chatDBlocal.execSQL(CREATE_USERS_DB);
                        // chatDBlocal.delete(Message.TABLE_NAME,null,null);
                        chatDBlocal.insert(Person.TABLE_NAME, null, insertValues);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }


            }
            return null;
        }

        @Override
        protected void onPostExecute(Void object) {
            super.onPostExecute(object);
            Intent intent=new Intent();
            intent.setAction(getString(R.string.ACTION_STORE_USERS));
            sendBroadcast(intent);

        }
    }

    public  class showOnlineUsers extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void[] objects) {
            JsonObject result;
            final String SERVER_METHOD_SEND = "showToUsers";
            try {

                while (isConServer==false){}
                result = mHubProxy.invoke(JsonObject.class, SERVER_METHOD_SEND).get();
            }
            catch (InterruptedException| ExecutionException e) {
                result=null;
            }
//            Log.i("TAG",result.toString());


            final String method = "onlineUsers";
            try {

                while (isConServer==false){}
                result = mHubProxy.invoke(JsonObject.class, method).get();
            }
            catch (InterruptedException| ExecutionException e) {
                result=null;
            }
            Log.i("TAG",result.toString());

            return null;
        }

    }

    public void showUsers(){
        showOnlineUsers op=new showOnlineUsers();
        op.execute();

    }

}
