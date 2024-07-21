package com.example.mst.mav2dvi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.mst.mav2dvi.fragments.FragmentStatus;
import com.example.mst.mav2dvi.fragments.FragmentLiveView;
import com.example.mst.mav2dvi.fragments.FragmentMission;
import com.example.mst.mav2dvi.fragments.MenuFragment;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MenuFragment.OnFragmentSelectedListener {

    private FragmentStatus mFragment1 = null;
    private Snackbar snackbarStatus;
    private TcpClient.TcpConnectStatus mTcpStatus = TcpClient.TcpConnectStatus.DISCONNECTED;
    protected BroadcastReceiver mReceiver;

    private TcpController.TcpControllerCallback mTcpControllerCallback = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Instance of first fragment
        MenuFragment menuFragment = new MenuFragment();
        //create new
        if (savedInstanceState == null) {
            // Add Fragment to FrameLayout (flContainer), using FragmentManager
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();// begin  FragmentTransaction
            ft.add(R.id.flContainer, menuFragment);                                // add    Fragment
            ft.commit();                                                            // commit FragmentTransaction
        //replace
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flContainer, menuFragment) // replace flContainer
                    .commit();
        }

        //if landscape set default content fragment - FragmentStatus
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            if(mFragment1 == null){
                mFragment1 = new FragmentStatus();
            }
            FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
            ft2.add(R.id.flContainer2, mFragment1);
            ft2.commit();
        }

        registerReceiver();
        if(TcpController.isTcpConnected()){
            mTcpStatus = TcpClient.TcpConnectStatus.CONNECTED;
        }
        showSnackbar();
        initTcpControllerCallback();
    }

    private void registerReceiver(){
        //create receiver
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showSnackbar();
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ApplicationCore.FLAG_CONNECTION_CHANGE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filter);
    }

    public void showSnackbar(){
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        String msg = "";
        int color = R.color.colorBlack;
        int duration = Snackbar.LENGTH_SHORT;
        switch (mTcpStatus){
            case CONNECTED:
                msg = "Tcp connected";
                color = R.color.colorGreen;
                duration = Snackbar.LENGTH_SHORT;
                break;
            case CONNECTING:
                msg = "Tcp Connecting";
                color = R.color.colorOrange;
                duration = Snackbar.LENGTH_INDEFINITE;
                break;
            case DISCONNECTED:
                msg = "Tcp Disconnected";
                color = R.color.colorRed;
                duration = Snackbar.LENGTH_INDEFINITE;
                break;
        }

        if(ApplicationCore.isProductConnected()){
            msg += " - drone connected";
        } else {
            msg += " - drone not connected";
            color = R.color.colorRed;
            duration = Snackbar.LENGTH_INDEFINITE;
        }

        if(snackbarStatus != null && snackbarStatus.isShown()){
            snackbarStatus.dismiss();
        }

        snackbarStatus = Snackbar.make(drawer, msg, duration);
        //.setAction("Action", null);
        snackbarStatus.getView().setBackgroundColor(ContextCompat.getColor(this, color));
        snackbarStatus.show();
    }

    public void onClick(View view){

    }

    //interaction with menu fragment
    @Override
    public void onItemSelected(int pos) {
        Toast.makeText(this, "Called By Fragment A: position - "+ pos, Toast.LENGTH_SHORT).show();

        Fragment currentFragment;
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            currentFragment = getSupportFragmentManager().findFragmentById(R.id.flContainer2);
        } else {
            currentFragment = getSupportFragmentManager().findFragmentById(R.id.flContainer);
        }

        // Load New Fragment
        Fragment newFragment;
        switch (pos){
            case 0:
                if(currentFragment instanceof FragmentStatus){
                    return;
                }
                if(mFragment1 == null){
                    mFragment1 = new FragmentStatus();
                }
                newFragment = mFragment1;
                break;
            case 1:
                if(currentFragment instanceof FragmentLiveView){
                    return;
                }
                newFragment = new FragmentLiveView();
                break;
            case 2:
                if(currentFragment instanceof FragmentMission){
                    return;
                }

                //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                newFragment = new FragmentMission();
                break;
            default:
                return;
        }

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flContainer2, newFragment)
                    .commit();
        }else{
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flContainer, newFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void initTcpControllerCallback(){
        mTcpControllerCallback = new TcpController.TcpControllerCallback() {
            @Override
            public void onStatusChange(final TcpClient.TcpConnectStatus status, final String msg) {
                Log.i("tcp", "connect activity recieve status change");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (status){
                            case CONNECTED:
                                mTcpStatus = TcpClient.TcpConnectStatus.CONNECTED;
                                Toast.makeText(MainActivity.this, "Connection success", Toast.LENGTH_LONG).show();
                                showSnackbar();
                                break;
                            case CONNECTING:
                                mTcpStatus = TcpClient.TcpConnectStatus.CONNECTING;
                                Toast.makeText(MainActivity.this, "Connecting", Toast.LENGTH_LONG).show();
                                showSnackbar();
                                break;
                            case DISCONNECTED:
                                mTcpStatus = TcpClient.TcpConnectStatus.DISCONNECTED;
                                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_LONG).show();
                                showSnackbar();
                                break;
                        }
                    }
                });
            }

            @Override
            public void onMessageRecieved(String msg) { }

            @Override
            public void onCommandRecieved(Command com, JSONObject jsonObject) { }
        };
        TcpController.attachListener(mTcpControllerCallback);
    }





















    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
