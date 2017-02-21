package com.example.sashok.messanger;

import android.provider.BaseColumns;

/**
 * Created by sasho on 03.02.2017.
 */

public abstract  class Message implements BaseColumns
{
    public static final String TABLE_NAME ="mails";
    public static final String COLUMN_NAME_TEXT="text";
    public static final String COLIMN_NAME_DATE="date";
    public static final String COLUMN_NAME_ReceiveID="receiveID";
    public static final String COLUMN_NAME_SenderID="senderID";
}
