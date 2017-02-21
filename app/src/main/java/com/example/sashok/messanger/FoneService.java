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
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import microsoft.aspnet.signalr.client.Action;
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
    Boolean hasInternetCon=false;
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
        connect();
//        copy_db();
        IntentFilter filter=new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(broadcastReceiver, filter);
        selectUserssTask userOp=new selectUserssTask();
        userOp.execute();
        selectMailsTask mailOp=new selectMailsTask();
        mailOp.execute();


        //        if (isActiveNetwork()) {
//            connect();
//        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {

         return mBinder;
    }

    public void connect()    {
        if (isActiveNetwork() && !isConServer) {
            update_form();
            mOpenConnection = new OpenConnection();
            mOpenConnection.execute();
        }

    }

    public class OpenConnection extends AsyncTask<Void,Void,Boolean> {
        @Override
        protected Boolean doInBackground(Void... params)
        {
            Boolean result;
            final String login=cur_user.login.toString();
            Platform.loadPlatformComponent(new AndroidPlatformComponent());
            Credentials credentials = new Credentials() {
                @Override
                public void prepareRequest(Request request) {
                    request.addHeader("login",login);

                }
            };
            String serverUrl = getString(R.string.server_name);
            mHubConnection = new HubConnection(serverUrl);
            mHubConnection.setCredentials(credentials);
            String SERVER_HUB_CHAT = "ChatHub";
            mHubProxy = mHubConnection.createHubProxy(SERVER_HUB_CHAT);
            ClientTransport clientTransport = new ServerSentEventsTransport(mHubConnection.getLogger());
            final SignalRFuture<Void> signalRFuture = mHubConnection.start(clientTransport);
            try {
                signalRFuture.get();
                result=true;
            } catch (InterruptedException | ExecutionException e) {
                result=false;

            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            isConServer=result;
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
                        isConServer=false;
                    }
                });
                mHubConnection.connected(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("TAG", "connected");
                        isConServer=false;
                    }
                });
                mHubConnection.reconnected(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("TAG", "reconnected");
                        isConServer=false;
                    }
                });
                mHubConnection.closed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("TAG", "closed");
                        isConServer=false;
                        if (isActiveNetwork())
                        mOpenConnection = new OpenConnection();
                        mOpenConnection.execute();

                    }
                });
                mHubProxy.on("updateUsers",
                        new SubscriptionHandler1<JsonElement>() {
                            @Override
                            public void run(final JsonElement json) {
                                Log.i("TAG",json.toString());

                            }
                        }
                        , JsonElement.class);
                mHubProxy.on("singedin",new  SubscriptionHandler() {
                    @Override
                    public void run() {
                        Log.i("TAG","sing_in");

                    }
                });
                if (is_binded == false) sing_in();
//                Intent intent = new Intent(getBaseContext(), FoneService.class);
//                startService(intent);
            }
        }
    }

    public class LocalBinder extends Binder {
        public FoneService getService() {
            // Return this instance of SignalRService so clients can call public methods
            return FoneService.this;
        }
    }

    public void sing_in()     {
        if (isActiveNetwork()) {
            //if (!isConServer) connect();
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

    public  class SingInTask extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected Boolean doInBackground(Void[] objects) {
            Boolean result=false;
            final String SERVER_METHOD_SEND = "SingIn";
            try {
                while (mHubProxy==null){}
                result = mHubProxy.invoke(Boolean.class, SERVER_METHOD_SEND, cur_user.login,cur_user.password).get();
            }
            catch (InterruptedException | ExecutionException e) {
                result=false;
            }
          return result;
         }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            Intent intent=new Intent();
            intent.putExtra("result",result);
            intent.setAction(getString(R.string.ACTION_SING_IN));
            //            mHubProxy.subscribe("updateUsers").addReceivedHandler(new Action<JsonElement[]>() {
//                    @Override
//                    public void run(JsonElement[] jsonElements) throws Exception {
//                        Log.i("TAG", jsonElements[0].toString());
//
//                    }
//                });

            sendBroadcast(intent);
        }
    }


    public  void sing_up()
    {
        if (isActiveNetwork()) {
//            if (!isConServer) connect();
            mHubConnection.stop();
            mHubProxy=null;
            connect();
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

    public  class SingUpTask extends AsyncTask<Void,Void,Boolean> {

        @Override
        protected Boolean doInBackground(Void[] objects) {
            Boolean result;
            final String SERVER_METHOD_SEND = "addNewUser";
            try {
                while (mHubProxy==null){}
                result = mHubProxy.invoke(Boolean.class, SERVER_METHOD_SEND, cur_user.first_name,cur_user.login,cur_user.password,cur_user.last_name).get();
            }
            catch (InterruptedException| ExecutionException e) {
                result=false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mSettings==null) getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor=mSettings.edit();
            editor.putBoolean(getString(R.string.SAVE_KEY), result);
            editor.apply();
            super.onPostExecute(result);
            Intent intent=new Intent();
            intent.putExtra("result",result);
            intent.setAction(getString(R.string.ACTION_SING_UP));
            sendBroadcast(intent);
        }
    }

    public void update_form()    {
       if (mSettings==null) mSettings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        cur_user.login=mSettings.getString(Person.COLUMN_NAME_LOGIN,"");
        cur_user.password=mSettings.getString(Person.COLUMN_NAME_PASSWORD,"");
        cur_user.first_name=mSettings.getString(Person.COLUMN_NAME_FIRST_NAME,"");
        cur_user.last_name=mSettings.getString(Person.COLUMN_NAME_LAST_NAME,"");

    }

    public boolean isActiveNetwork(){
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork!=null) hasInternetCon = activeNetwork.isConnectedOrConnecting();
        else hasInternetCon=false;
        return hasInternetCon;
    }

    public class selectMailsTask extends  AsyncTask<Void,Void,JsonArray> {

        @Override
        protected JsonArray doInBackground(Void... params) {
            int lastID = 0;
            JsonArray result;
            chatDBlocal = openOrCreateDatabase(DB_NAME,
                    Context.MODE_PRIVATE, null);
            chatDBlocal.execSQL(CREATE_MAILS_DB);
            Cursor cursor = chatDBlocal.rawQuery("SELECT _id from mails ORDER BY _id DESC LIMIT 1;", null);
            if (cursor.moveToFirst()) {
                lastID = cursor.getInt(cursor.getColumnIndex(Message._ID));

            }
            cursor.close();
            update_form();
            final String SERVER_METHOD_SEND = "selectMails";
            try {
                while (mHubProxy == null) {
                }
                result = mHubProxy.invoke(JsonArray.class, SERVER_METHOD_SEND, cur_user.login, lastID).get();
            } catch (InterruptedException | ExecutionException e) {
                result = null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(JsonArray ja) {
            super.onPostExecute(ja);
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
        }
    }

    public  void copy_db()    {
        try {
            File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                String currentDBPath = "/data/data/" + getPackageName() + "/databases/"+DB_NAME;
                String backupDBPath = "messengerdb.db";
                File currentDB = new File(currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
            }
        } catch (Exception e) {

        }
    }

    public class selectUserssTask extends  AsyncTask<Void,Void,JsonArray>{

        @Override
        protected JsonArray doInBackground(Void... params) {
            int lastID=0;
            JsonArray result;
            chatDBlocal = openOrCreateDatabase(DB_NAME,
                    Context.MODE_PRIVATE, null);
            chatDBlocal.execSQL(CREATE_USERS_DB);
            Cursor cursor=chatDBlocal.rawQuery("SELECT _id from persons ORDER BY _id DESC LIMIT 1;",null);
            if (cursor.moveToFirst()) {
                lastID = cursor.getInt(cursor.getColumnIndex(Person._ID));

            }
            cursor.close();
            update_form();
            final String SERVER_METHOD_SEND = "selectUsers";
            try {
                while (mHubProxy==null){}
                result = mHubProxy.invoke(JsonArray.class, SERVER_METHOD_SEND,lastID).get();
            }
            catch (InterruptedException| ExecutionException e) {
                result=null;
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(JsonArray ja) {
            super.onPostExecute(ja);
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
        }
    }
}
