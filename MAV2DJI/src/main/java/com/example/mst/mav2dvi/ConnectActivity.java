package com.example.mst.mav2dvi;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mst.mav2dvi.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import dji.sdk.base.BaseProduct;

public class ConnectActivity extends AppCompatActivity {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    private int mPort;
    private String mIp;

    protected BroadcastReceiver mReceiver;

    private ProgressDialog progressDialog;
    private String progressDialogMessage;

    private TextView textView_permissions = null;
    private TextView textView_registration = null;
    private TextView textView_drone = null;
    private TextView textView_ipPort = null;
    private TextView textView_tcpStatus = null;
    private Button button_permissions = null;
    private Button button_open = null;

    private TcpController mTcpController = null;
    private TcpController.TcpControllerCallback mTcpControllerCallback = null;

    //get permissions
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };
    private List<String> missingPermission;
    private static final int REQUEST_PERMISSION_CODE = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        registerReceiver();
        findInitUiElements();

        mTcpController = ApplicationCore.getTcpController();

        if (savedInstanceState != null) {
            restoreSaveInstance(savedInstanceState);
        } else {
            checkAndRequestPermissions();
        }
    }

    @Override
    protected void onDestroy() {
        Log.e("tag", "onDestroy");
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void registerReceiver(){
        //create receiver
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("tag", "ConnectActivity onRecieve");
                progressDialog.dismiss();

                if(intent.getAction() != null){
                    switch (intent.getAction()){
                        case ApplicationCore.FLAG_REGISTRATION_OK:
                            Toast.makeText(ConnectActivity.this, "registration ok: " + intent.getAction(), Toast.LENGTH_LONG).show();
                            textView_registration.setText("-- OK! --");
                            break;
                        case ApplicationCore.FLAG_REGISTRATION_FAILED:
                            Toast.makeText(ConnectActivity.this, "registration failed: " + intent.getAction(), Toast.LENGTH_LONG).show();
                            textView_registration.setText(intent.getStringExtra(ApplicationCore.FLAG_REGISTRATION_FAILED_DESC));
                            break;
                        case ApplicationCore.FLAG_CONNECTION_CHANGE:
                            Log.i("tag", "connectActivity connection change recv");
                            onConnectionChange();
                    }
                }
            }
        };

        //tell receiver what to listen for
        IntentFilter filter = new IntentFilter();
        filter.addAction(ApplicationCore.FLAG_REGISTRATION_OK);
        filter.addAction(ApplicationCore.FLAG_REGISTRATION_FAILED);
        filter.addAction(ApplicationCore.FLAG_CONNECTION_CHANGE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filter);
        //registerReceiver(mReceiver, filter); //global
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
                            case CONNECTING:
                                if(progressDialog.isShowing()){
                                    if(progressDialogMessage.equals("")){
                                        progressDialogMessage = msg;
                                    } else {
                                        progressDialogMessage += "\n" + msg;
                                    }
                                    progressDialog.setMessage(progressDialogMessage);
                                }
                                textView_tcpStatus.setText("-- Trying to connect --");
                                break;
                            case CONNECTING_SUCCESS:
                                if(progressDialog.isShowing()){
                                    progressDialog.dismiss();
                                }
                                textView_tcpStatus.setText("-- Connected --");
                                Toast.makeText(ConnectActivity.this, "Connection success", Toast.LENGTH_LONG).show();
                                break;
                            case CONNECTING_FAILED:
                                if(progressDialog.isShowing()){
                                    progressDialog.dismiss();
                                }
                                textView_tcpStatus.setText("-- Disconnected --");
                                showAlertDialog("Connection failed", msg);
                                break;
                            case DISCONNECTED:
                                if(progressDialog.isShowing()){
                                    progressDialog.dismiss();
                                }
                                textView_tcpStatus.setText("-- Disconnected --");
                                showAlertDialog("Disconnected", msg);
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

    private void onConnectionChange(){
        BaseProduct mProduct = ApplicationCore.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {
            button_open.setEnabled(true);

            //String str = mProduct instanceof Aircraft ? "DJIAircraft" : "DJIHandHeld";
            //mTextConnectionStatus.setText("Status: " + str + " connected");

            if (null != mProduct.getModel()) {
                textView_drone.setText("" + mProduct.getModel().getDisplayName());
            } else {
                textView_drone.setText("unknown model");
            }

        } else {
            button_open.setEnabled(false);

            textView_drone.setText("not connected/connection lost");
            //mTextConnectionStatus.setText(R.string.connection_loose);
        }
    }

    //restore ui when orientation changes
    private void restoreSaveInstance(Bundle savedInstanceState){
        textView_permissions.setText(savedInstanceState.getString("permText"));
        textView_registration.setText(savedInstanceState.getString("regText"));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("permText", textView_permissions.getText().toString());
        outState.putString("regText", textView_registration.getText().toString());
        super.onSaveInstanceState(outState);
    }

    private void findInitUiElements(){
        textView_permissions = (TextView)findViewById(R.id.textView_permissions);
        textView_registration = (TextView)findViewById(R.id.textView_registration);
        textView_drone = (TextView)findViewById(R.id.textView_drone);
        textView_ipPort = (TextView)findViewById(R.id.textView_tcp);
        textView_tcpStatus = (TextView)findViewById(R.id.textView_tcpStatus);

        if(mTcpController != null){
            if(TcpController.isTcpConnected()){
                textView_tcpStatus.setText("-- Connected --");
            } else {
                textView_tcpStatus.setText("-- Not Connected --");
            }
        }

        button_permissions = (Button)findViewById(R.id.button_permissions);
        button_permissions.setVisibility(View.INVISIBLE);

        button_open = (Button)findViewById(R.id.button_openApp);
        button_open.setEnabled(false);

        progressDialog = new ProgressDialog(this);

        pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        if(pref.contains("ip") && pref.contains("port")){
            mIp = pref.getString("ip", "notSet");
            mPort = pref.getInt("port", -1);
            textView_ipPort.setText(mIp + ":" + mPort);
        } else {
            textView_ipPort.setText("-- not set --");
        }
        onConnectionChange();
    }

    public void onClick(View view){
        switch(view.getId()){
            case R.id.button_permissions:
                checkAndRequestPermissions();
                break;
            case R.id.button_setIpPort:
                setIpPortDialog();
                break;
            case R.id.button_connectTcp:
                if(mTcpController != null){
                    if(mTcpControllerCallback == null){
                        initTcpControllerCallback();
                    }

                    textView_tcpStatus.setText("-- Trying to connect --");
                    progressDialog.setTitle("Connecting via Tcp");
                    progressDialogMessage = mIp + " " + mPort;
                    progressDialog.setMessage(progressDialogMessage);

                    progressDialog.setCancelable(true); // disable dismiss by tapping outside of the dialog
                    progressDialog.show();
                    mTcpController.startConnect(mIp, mPort);
                }
                break;
            case R.id.button_disconnectTcp:
                if(mTcpController != null){
                    mTcpController.disconnect();
                }
                break;
            case R.id.button_openApp:
                Intent m = new Intent(this.getApplicationContext(), MainActivity.class);
                startActivity(m);
                break;
        }
    }

    private void checkIpPort(String _ip, String _port){
        String cleanIp = _ip.replace(" ", "");
        String cleanPort = _port.replace(" ", "");
        int port;

        //try to parse int from String
        try{
            port = Integer.parseInt(cleanPort);
        } catch (Exception e){
            port = -1;
        }

        //check validity
        if(port >= 0 && port <= 65535 //is port valid
            && Patterns.IP_ADDRESS.matcher(cleanIp).matches()){ //is ip valid
            mIp = cleanIp;
            mPort = port;
            textView_ipPort.setText(mIp + ":" + mPort);
            //remember for next time
            editor = pref.edit();
            editor.putString("ip", mIp);
            editor.putInt("port", mPort);
            editor.apply();
        } else {
            showAlertDialog("Error", "Ip or port invalid");
        }
    }

    private void setIpPortDialog(){

        LayoutInflater factory = LayoutInflater.from(this);

        //text_entry is an Layout XML file containing two text field to display in alert dialog
        final View textEntryView = factory.inflate(R.layout.text_entry, null);

        final EditText input1 = (EditText) textEntryView.findViewById(R.id.EditText1);
        final EditText input2 = (EditText) textEntryView.findViewById(R.id.EditText2);

        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Set Ip and Port:")
            .setView(textEntryView)
            .setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        checkIpPort(input1.getText().toString(), input2.getText().toString());
                    }
                })
            .setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                });
        alert.show();
    }

    private void showAlertDialog(String title, String msg){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ConnectActivity.this);

        // set title
        alertDialogBuilder.setTitle(title);

        // set dialog message
        alertDialogBuilder
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Ok",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    //##############################################################################################
    //region registration
    //##############################################################################################

    private void startRegistration(){
        progressDialogMessage += " - OK\nChecking registration";
        progressDialog.setMessage(progressDialogMessage);

        progressDialogMessage = "Permissions ok\n\nchecking registration";
        progressDialog.setTitle("Initializing");
        progressDialog.setMessage(progressDialogMessage);
        progressDialog.setCancelable(true);
        progressDialog.show();

        Intent intent = new Intent(ApplicationCore.FLAG_START_REGISTRATION);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        Log.i("tag", "broadcast sent startRegistration");
    }

    //##############################################################################################
    //endregion
    //region permissions
    //##############################################################################################

    //go thorugh list of reqested permissions - list specified here not in manifest
    //many permissions will be automatically granted - ask for others here
    private void checkAndRequestPermissions() {

        // on SDK lower than 23 permissions are asked at installation
        if (Build.VERSION.SDK_INT >= 23) {
            if(missingPermission == null){
                missingPermission = new ArrayList<>();
            } else {
                missingPermission.clear();
            }

            // Check for missing permissions
            for (String eachPermission : REQUIRED_PERMISSION_LIST) {
                if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                    missingPermission.add(eachPermission);
                }
            }
            //request missing permissions
            if (missingPermission.isEmpty()) {
                startRegistration();
                textView_permissions.setText("-- OK! --");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this,
                        missingPermission.toArray(new String[missingPermission.size()]),
                        REQUEST_PERMISSION_CODE);
            }
        } else {
            // Pre-Marshmallow
            startRegistration();
        }
    }

    //this is called when requestPermissions returns - once for all requested
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Check for granted permission and remove from missing list
        boolean neverAskAgainSelected = false;

        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {

                if (Build.VERSION.SDK_INT >= 23) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        // user rejected the permission
                        String permission = permissions[i];
                        boolean showRationale = shouldShowRequestPermissionRationale(permission);
                        if (!showRationale) { //never ask again was selected
                            Log.i("sdkBrige", "never ask again selected");
                            neverAskAgainSelected = true;
                        } else { //permission denied but can be asked again
                            Log.i("sdkBrige", "never ask again NOT selected");
                        }
                    }
                }

                //if permission was granted remove from list of missing
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }

        // report result
        // Never again selected
        if(neverAskAgainSelected){
            showAlertDialog("Missing Permissions", "Don't ask again selected for one ore more necessary permissions\n" +
                    "App will not work without permissions\nPlease grant permissions in Phone Settings");
            button_permissions.setVisibility(View.INVISIBLE);

        // Missing permissions exist
        } else if (!missingPermission.isEmpty()) {
            textView_permissions.setText("-- Not OK! --");
            button_permissions.setVisibility(View.VISIBLE);
            showAlertDialog("Missing Permissions", "App will not work without requested permissions\n" +
                    "\nPlease grant permissions");

        // Permissions ok - start registration
        } else {
            textView_permissions.setText("-- OK! --");

            button_permissions.setVisibility(View.INVISIBLE);

            startRegistration();
        }
    }

    //##############################################################################################
    //endregion
    //##############################################################################################

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connect, menu);
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
}
