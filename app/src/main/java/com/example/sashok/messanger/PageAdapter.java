package com.example.sashok.messanger;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by sashok on 21.2.17.
 */

public class PageAdapter extends FragmentPagerAdapter {
    Context ctx;

    public PageAdapter(FragmentManager fm, Context ctx) {
        super(fm);
        this.ctx=ctx;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                UserFragment tab1 = new UserFragment();
                return tab1;
            case 1:
                MailFragment tab2 = new MailFragment();
                return tab2;
        }
        return null;

    }

    @Override
    public int getCount()
        {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position==0) return  "Пользователи";
        else return "Сообщения";
    }
}
