/*
 * Copyright (C) 2017 Jonatan Cheiro Anriquez
 *
 * This file is part of Block Calls.
 *
 * Block Calls is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Block Calls is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Block Calls. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jachness.blockcalls.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.jachness.blockcalls.BuildConfig;
import com.jachness.blockcalls.R;
import com.jachness.blockcalls.androidService.CallBlockingService;
import com.jachness.blockcalls.db.LogTable;
import com.jachness.blockcalls.stuff.AppContext;
import com.jachness.blockcalls.stuff.AppPreferences;
import com.jachness.blockcalls.stuff.PermUtil;
import com.jachness.blockcalls.stuff.Util;

import java.util.ArrayList;
import java.util.List;

import hugo.weaving.DebugLog;
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

/**
 * Welcome / Main Screen
 */
public class MainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener,
        View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERM_CODE_READ_CONTACTS_FOR_ADDING = 2;


    private View coordinatorLayout;
    private AppPreferences appPreferences;
    private FloatingActionButton fab;

    private Toolbar toolbar;
    private TabLayout tabLayout;
    private int counter = 0;
    private ShowcaseView showcase;


    @Override
    @DebugLog
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        boolean[] allowPermissions = PermUtil.checkInitialPermissions((AppContext)
                getApplicationContext());
        for (boolean allow : allowPermissions) {
            if (!allow) {
                Intent newActivity = new Intent(this, InitialPermissionsActivity.class);
                startActivity(newActivity);
                finish();
                return;
            }
        }

        setContentView(R.layout.main);

        coordinatorLayout = findViewById(R.id.main_coordinator_layout);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        BlackListFragment blackListFragment = new BlackListFragment();
        LogListFragment logListFragment = new LogListFragment();
        adapter.addFragment(blackListFragment, getResources().getString(R.string
                .common_black_list));
        adapter.addFragment(logListFragment, getResources().getString(R.string.common_log));
        viewPager.addOnPageChangeListener(this);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {

                switch (position) {
                    case 1:
                        fab.hide();
                        break;

                    default:
                        fab.show();
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });



        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        fab = (FloatingActionButton) findViewById(R.id.mainFloatingActionButton);
        fab.setOnClickListener(this);


        AppContext appContext = (AppContext) getApplicationContext();
        appPreferences = appContext.getAppPreferences();
        if (appPreferences.isFirstTime()) {
            appPreferences.setAllDefaults();
            appPreferences.setFirstTime(false);

            Intent i = new Intent(getApplicationContext(), CallBlockingService.class);
            i.putExtra(CallBlockingService.DRY, true);
            startService(i);
        }

    }


    private void showFabPrompt() {
        appPreferences.setLesson1(true);
        new MaterialTapTargetPrompt.Builder(MainActivity.this)
                .setTarget(findViewById(R.id.mainFloatingActionButton))
                .setBackgroundColourFromRes(R.color.colorPrimary)
                .setPrimaryText("Block a phone number")
                .setSecondaryText("Tap the plus button to add a number to the blacklist. You will no longer receive calls from that number.")
                .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener() {
                    @Override
                    public void onHidePrompt(MotionEvent event, boolean tappedTarget) {

                    }

                    @Override
                    public void onHidePromptComplete() {

                    }
                })
                .show();
    }

    @Override
    @DebugLog
    protected void onResume() {
        super.onResume();
        if (appPreferences.isBlockingEnable() && !Util.isServiceRunning(this, CallBlockingService
                .class)) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "starting " + CallBlockingService.class.getSimpleName());
            Intent i = new Intent(getApplicationContext(), CallBlockingService.class);
            i.putExtra(CallBlockingService.DRY, true);
            startService(i);
        }

        if (!appPreferences.isLesson1()) {
            showFabPrompt();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    @DebugLog
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERM_CODE_READ_CONTACTS_FOR_ADDING:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doAddingFromContacts();
                } else {
                    showSnackForContactPermission();
                }
                break;
        }
    }

    @DebugLog
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showSnackForContactPermission() {
        Snackbar snackBar = Snackbar.make(coordinatorLayout, R.string
                .common_contact_permission_message, Snackbar
                .LENGTH_LONG);
        snackBar.getView().setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark,
                getTheme()));
        snackBar.setAction(R.string.common_grant, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
        snackBar.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        super.getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.callLogDeleteAll:
                AlertDialog.Builder builder = new AlertDialog.Builder(this, 0);
                builder.setTitle(R.string.delete_log_title);
                builder.setMessage(R.string.delete_log_message);
                builder.setPositiveButton(R.string.common_delete, new DialogInterface.OnClickListener
                        () {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getContentResolver().delete(LogTable.CONTENT_URI, null, null);
                    }
                });
                builder.setNegativeButton(R.string.common_cancel, new DialogInterface
                        .OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
                return true;
            case R.id.mainMnAllSettings:
                startActivity(new Intent(MainActivity.this, AllSettingsActivity.class));
                return true;
            case R.id.mainMnBlockingSettings:
                startActivity(new Intent(MainActivity.this, BlockingSettingsActivity.class));
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    @DebugLog
    public void onClick(final View v) {
        Object posObj = v.getTag();
        final int position = posObj == null ? 0 : (int) posObj;

        switch (position) {
            case 0:
                AlertDialog.Builder menuBuilder = new AlertDialog.Builder(this);
                menuBuilder.setTitle(R.string.main_menu_title_add_new)
                        .setItems(R.array.main_menu_add, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent newActivity = new Intent(MainActivity.this, AddActivity
                                        .class);
                                switch (which) {
                                    case 0:
                                        newActivity.putExtra(AddActivity.FRAGMENT_KEY,
                                                AddActivity.FRAGMENT_MANUAL);
                                        startActivity(newActivity);
                                        return;
                                    case 1:
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            if (!PermUtil.checkReadContacts(MainActivity.this)) {
                                                MainActivity.this.requestPermissions(new
                                                                String[]{PermUtil.READ_CONTACTS},
                                                        PERM_CODE_READ_CONTACTS_FOR_ADDING);
                                                return;
                                            }
                                        }
                                        doAddingFromContacts();
                                        return;
                                    case 2:
                                        newActivity.putExtra(AddActivity.FRAGMENT_KEY,
                                                AddActivity.FRAGMENT_CALLLOG);
                                        startActivity(newActivity);
                                        return;
                                    default:
                                        throw new RuntimeException("Should not be here");
                                }
                            }
                        });
                menuBuilder.show();
                return;
            default:
                throw new IllegalArgumentException("Should not be here");
        }


    }

    @DebugLog
    private void doAddingFromContacts() {
        Intent newActivity = new Intent(MainActivity.this, AddActivity.class);
        newActivity.putExtra(AddActivity.FRAGMENT_KEY, AddActivity.FRAGMENT_CONTACTS);
        startActivity(newActivity);
    }

    public class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }



}

