package com.example.sashok.messanger;

import android.provider.BaseColumns;

/**
 * Created by sasho on 01.02.2017.
 */

public abstract  class Person implements BaseColumns
{
    public static final String TABLE_NAME ="persons";
    public static final String COLUMN_NAME_LOGIN="login";
    public static final String COLUMN_NAME_PASSWORD="password";
    public static final String COLUMN_NAME_FIRST_NAME="first";
    public static final String COLUMN_NAME_LAST_NAME="last";
}