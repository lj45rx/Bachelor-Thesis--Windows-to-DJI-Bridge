package com.example.mst.mav2dvi;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;

public class ApplicationCore extends Application{

    public static final String FLAG_START_REGISTRATION = "startRegistration";
    public static final String FLAG_REGISTRATION_OK = "registrationOk";
    public static final String FLAG_REGISTRATION_FAILED = "registrationFailed";
    public static final String FLAG_REGISTRATION_FAILED_DESC = "registrationFailedDesc";
    public static final String FLAG_CONNECTION_CHANGE = "connectionChange";

    protected BroadcastReceiver mReceiver;

    private DJISDKManager.SDKManagerCallback mSdkManagerCallback = null;
    private static BaseProduct mProduct = null;
    public Handler mHandler;

    private Application instance;
    private static MMissionController mMissionController = null;
    private static MFlightController mMFlightController = null;
    private static TcpController mTcpController = null;

    public void setContext(Application application) {
        instance = application;
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    public ApplicationCore(){

    }

    @Override
    public void onCreate(){
        super.onCreate();
        mMFlightController = new MFlightController();
        mMissionController = new MMissionController();
        mTcpController = new TcpController();
        mMFlightController.initTcpControllerCallback();
        mMissionController.initTcpControllerCallback();
        mSdkManagerCallback = getSdkManagerCallback();
        registerReceiver();
    }

    /**
     * This function is used to get the instance of DJIBaseProduct.
     * If no product is connected, it returns null.
     */
    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    public static synchronized boolean isProductConnected(){
        return (getProductInstance() != null) && mProduct.isConnected();
    }

    public static MMissionController getMMissionController(){
        return mMissionController;
    }

    public static TcpController getTcpController(){
        return mTcpController;
    }

    private DJISDKManager.SDKManagerCallback getSdkManagerCallback(){
        return new DJISDKManager.SDKManagerCallback() {

            //Listens to the SDK registration result
            @Override
            public void onRegister(final DJIError djiError) {
                if(djiError == DJISDKError.REGISTRATION_SUCCESS) {
                    Log.i("tag", "registration success " + djiError.toString());
                    sendLocalBroadcast(ApplicationCore.FLAG_REGISTRATION_OK);
                    DJISDKManager.getInstance().startConnectionToProduct();
                } else {
                    Log.i("tag", "registration failed " + djiError.toString());

                    Intent intent = new Intent(ApplicationCore.FLAG_REGISTRATION_FAILED);
                    intent.putExtra(ApplicationCore.FLAG_REGISTRATION_FAILED_DESC, djiError.toString());
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    Log.i("tag", "broadcast sent " + ApplicationCore.FLAG_REGISTRATION_FAILED);

                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Register failed: " + djiError.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
                Log.e("TAG", djiError.toString());
            }

            @Override
            public void onProductDisconnect() {
                Log.d("TAG", "onProductDisconnect");
                showToast("drohe disconnected");
                mProduct = null;
                mMFlightController.onConnectChange();
                mMissionController.onConnectChange();
                sendLocalBroadcast(ApplicationCore.FLAG_CONNECTION_CHANGE);
            }
            @Override
            public void onProductConnect(BaseProduct baseProduct) {
                Log.d("TAG", String.format("onProductConnect newProduct:%s", baseProduct));
                showToast("drone connected");
                mProduct = DJISDKManager.getInstance().getProduct();
                mMFlightController.onConnectChange();
                mMissionController.onConnectChange();
                sendLocalBroadcast(ApplicationCore.FLAG_CONNECTION_CHANGE);

            }
            @Override
            public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                          BaseComponent newComponent) {
                if (newComponent != null) {
                    newComponent.setComponentListener(new BaseComponent.ComponentListener() {

                        @Override
                        public void onConnectivityChange(boolean isConnected) {
                            Log.d("TAG", "onComponentConnectivityChanged: " + isConnected);
                            sendLocalBroadcast(ApplicationCore.FLAG_CONNECTION_CHANGE);
                        }
                    });
                }

                Log.d("TAG",
                        String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                componentKey,
                                oldComponent,
                                newComponent));
            }
        };
    }

    private void showToast(final String msg){
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registerReceiver(){
        //create reciever
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("tag", "ApplicationCore onRecieve");
                DJISDKManager.getInstance().registerApp(getApplicationContext(), mSdkManagerCallback);
            }
        };

        //tell receiver what to listen for
        IntentFilter filter = new IntentFilter();
        filter.addAction(ApplicationCore.FLAG_START_REGISTRATION);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filter);
        //getApplicationContext().registerReceiver(mReceiver, filter);
    }

    private void sendLocalBroadcast(String type){
        Intent intent = new Intent(type);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        //getApplicationContext().sendBroadcast(intent);
        Log.i("tag", "broadcast sent " + type);
    }

}
