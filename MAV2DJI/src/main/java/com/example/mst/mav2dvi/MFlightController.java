package com.example.mst.mav2dvi;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import dji.common.battery.BatteryState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

class MFlightController {

    //##############################################################################################
    //region variables
    //##############################################################################################

    public interface FlightControllerUpdateCallback{
        void onFlightControllernUpdate(String status);
        void onBatteryUpdate(int percentage);
        void onJsonToSend(JSONObject jsonObject);
    }
    private static List<FlightControllerUpdateCallback> mCallbacks;

    private BaseProduct mProduct;

    private FlightController mFlightController;
    private FlightControllerState mFcState;

    private Battery mBattery;
    private BatteryState mBatteryState;
    private int lastBatteryPercentage = 777; //TODO evt eleganter

    //##############################################################################################
    //endregion
    //region constructor
    //##############################################################################################

    MFlightController(){
        Log.i("tag", "flightController created");

        if(mCallbacks == null){
            mCallbacks = new ArrayList<>();
        }
    }

    //##############################################################################################
    //endregion
    //region handle events
    //##############################################################################################

    void onConnectChange(){
        mProduct = ApplicationCore.getProductInstance();
        if (null != mProduct && mProduct.isConnected()) {
            initBatteryState();
            initFlightController();
        }
    }

    private void onBatteryUpdate(){ //only report when percentage changes
        if(mBatteryState.getChargeRemainingInPercent() != lastBatteryPercentage
            && mBatteryState.getChargeRemainingInPercent() != 0)
        {
            lastBatteryPercentage = mBatteryState.getChargeRemainingInPercent();
            sendOnBatteryUpdate(lastBatteryPercentage);
            Log.i("tag", "remaining voltage: " + mBatteryState.getChargeRemainingInPercent());

            JSONObject object = getJsonFromBatteryState();
            if(object != null){
                sendOnJsonToSend(object);
            }
        }
    }

    private void onFlightControllerUpdate(){
        sendOnFlightControllernUpdate("att: " + mFcState.getAttitude().pitch + " " + mFcState.getAttitude().roll + " " + mFcState.getAttitude().yaw);
        Log.i("tag", "att: " + mFcState.getAttitude().pitch + " " + mFcState.getAttitude().roll + " " + mFcState.getAttitude().yaw);

        JSONObject object = getJsonFromFcState();
        if(object != null){
            sendOnJsonToSend(object);
        }
    }

    //##############################################################################################
    //endregion
    //region create jsonObjects
    //##############################################################################################

    private JSONObject getJsonFromBatteryState(){
        if(mBatteryState == null){
            return null;
        }

        JSONObject object = new JSONObject();
        try {
            object.put("type", Command.REPORT_BATTERY.commandText);
            object.put("value", mBatteryState.getChargeRemainingInPercent());

            return object;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getJsonFromFcState(){
        if(mFcState == null){
            return null;
        }

        JSONObject object = new JSONObject();
        try {
            object.put("type", Command.FC_STATE.commandText);
            object.put("name", mProduct.getModel().getDisplayName());

            object.put("areMotorsOn", Boolean.valueOf(mFcState.areMotorsOn()));
            object.put("isFlying", Boolean.valueOf(mFcState.isFlying()));

            object.put("satelliteCount", Integer.valueOf((mFcState.getSatelliteCount())));

            if(mFcState.isHomeLocationSet()){
                object.put("homeLocation", String.valueOf(mFcState.getHomeLocation().getLatitude() + " " + mFcState.getHomeLocation().getLongitude()));
            } else {
                object.put("homeLocation", "not set");
            }

            JSONArray attitude = new JSONArray();
            attitude.put(0, mFcState.getAttitude().pitch);
            attitude.put(1, mFcState.getAttitude().roll);
            attitude.put(2, mFcState.getAttitude().yaw);
            object.put("attitude", attitude);

            JSONArray location = new JSONArray();
            if(mFcState.getGPSSignalLevel() != GPSSignalLevel.NONE
                    && mFcState.getGPSSignalLevel() != GPSSignalLevel.LEVEL_0){
                location.put(0, mFcState.getAircraftLocation().getLatitude());
                location.put(1, mFcState.getAircraftLocation().getLongitude());
                location.put(2, mFcState.getAircraftLocation().getAltitude());
                object.put("location", location);
                object.put("locationSet", Boolean.valueOf(true));
            } else {
                object.put("locationSet", Boolean.valueOf(false));
            }
            return object;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //##############################################################################################
    //endregion
    //region initialize listeners
    //##############################################################################################

    private void initBatteryState(){
        mBattery = mProduct.getBattery();
        try {
            mBattery.setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(BatteryState djiBatteryState) {
                    mBatteryState = djiBatteryState;
                    onBatteryUpdate();
                }
            });
        } catch (Exception ignored) {

        }
    }

    private void initFlightController(){
        mFlightController = ((Aircraft)mProduct).getFlightController();

        try {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState djiFlightControllerState) {
                    mFcState = djiFlightControllerState;
                    onFlightControllerUpdate();
                }
            });
        } catch (Exception ignored) {

        }
    }

    void initTcpControllerCallback(){
        TcpController.TcpControllerCallback mTcpControllerCallback = new TcpController.TcpControllerCallback() {
            @Override
            public void onStatusChange(TcpClient.TcpConnectStatus status, String msg) {

            }

            @Override
            public void onMessageRecieved(String msg) {

            }

            @Override
            public void onCommandRecieved(Command com, JSONObject jsonObject) {
                Log.i("tcp", "MFlightController command recieved " + com.name());

                if(mFlightController == null){ //TODO evt hier init erlauben
                    return;
                }

                switch (com){
                    case START_ENGINES:
                        mFlightController.turnOnMotors(getCallback());
                        break;
                    case STOP_ENGINES:
                        mFlightController.turnOffMotors(getCallback());
                        break;
                    case LAND:
                        mFlightController.startLanding(getCallback());
                        break;
                    case CANCEL_LAND:
                        mFlightController.cancelLanding(getCallback());
                        break;
                    case START:
                        mFlightController.startTakeoff(getCallback());
                        break;
                    case CANCEL_START:
                        mFlightController.cancelTakeoff(getCallback());
                    case GO_HOME:
                        mFlightController.startGoHome(getCallback());
                        break;
                    case CANCEL_GO_HOME:
                        mFlightController.cancelGoHome(getCallback());
                }
            }
        };
        TcpController.attachListener(mTcpControllerCallback);
    }

    //##############################################################################################
    //endregion
    //region FlightControllerUpdateCallback methods
    //##############################################################################################

    private void sendOnFlightControllernUpdate(String msg){
        for(int i = 0; i < mCallbacks.size(); i++){
            mCallbacks.get(i).onFlightControllernUpdate(msg);
        }
    }

    private void sendOnBatteryUpdate(int percentage){
        for(int i = 0; i < mCallbacks.size(); i++){
            mCallbacks.get(i).onBatteryUpdate(percentage);
        }
    }

    private void sendOnJsonToSend(JSONObject object){
        for(int i = 0; i < mCallbacks.size(); i++){
            mCallbacks.get(i).onJsonToSend(object);
        }
    }

    public static void attachListener(FlightControllerUpdateCallback fcUpdateCallback){
        if(!mCallbacks.contains(fcUpdateCallback)){
            mCallbacks.add(fcUpdateCallback);
        }
    }

    public static void detachListener(FlightControllerUpdateCallback fcUpdateCallback){
        if(mCallbacks.contains(fcUpdateCallback)){
            mCallbacks.remove(fcUpdateCallback);
        }
    }

    //##############################################################################################
    //endregion
    //region utils
    //##############################################################################################

    public CommonCallbacks.CompletionCallback getCallback(){
        return new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError != null){
                    Log.i("tag", djiError.getDescription());
                } else {

                }
            }
        };
    }

    //##############################################################################################
    //endregion
    //##############################################################################################
}