package com.example.mst.mav2dvi;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TcpController {

    //##############################################################################################
    //region variables
    //##############################################################################################

    interface TcpControllerCallback {
        void onStatusChange(TcpClient.TcpConnectStatus status, String msg);
        void onMessageRecieved(String msg);
        void onCommandRecieved(Command com, JSONObject jsonObject);
    }
    private static List<TcpControllerCallback> mCallbacks;

    private static TcpClient mTcpClient = null;
    private static ConnectTask mTask;
    private static String mIp;
    private static int mPort;
    public static TcpClient.TcpConnectStatus mTcpStatus = TcpClient.TcpConnectStatus.DISCONNECTED;

    private static TcpClient.TcpClientCallback mTcpClientCallback = null;

    //##############################################################################################
    //endregion
    //region constructor
    //##############################################################################################

    TcpController(){
        Log.i("tag", "TcpController created");
        if(mCallbacks == null){
            mCallbacks = new ArrayList<>();
        }
        initializeCallbacks();
    }

    //##############################################################################################
    //endregion
    //region connect/disconnect
    //##############################################################################################

    void startConnect(String _ip, int _port){
        mIp = _ip;
        mPort = _port;
        mTask = (ConnectTask) new ConnectTask().execute("");
    }

    void disconnect(){
        if(mTcpClient != null) {
            mTcpClient.stopClient();
        }
    }

    void sendTcpMessage(String message){
        if(mTcpClient != null){
            mTcpClient.sendMessage(message);
        }
        //Log.i("missionController", "TcpController sendTcpMessage");
    }


    //##############################################################################################
    //endregion
    //region parse and handle recieved messages
    //##############################################################################################

    private void handleRecievedMessage(String message){
        try {
            JSONObject jsonObj = new JSONObject(message);

            String type = jsonObj.optString("type");

            if(type == null){
                return;
            }

            Command com = Command.getCommandFromString(type);

            if(com == Command.UNKNOWN){
                sendTcpMessage("unknown command: " + type);
                return;
            }

            sendOnCommandRecieved(com, jsonObj);


        } catch (JSONException e) {
            Log.i("tcp", "TcpController handleMessage exception " + e.getMessage());
            sendTcpMessage("command could not be parsed as json");
        }
    }

    //##############################################################################################
    //endregion
    //region asyncTask
    //##############################################################################################
    public static class ConnectTask extends AsyncTask<String, String, TcpClient> {
        @Override
        protected TcpClient doInBackground(String... message) {
            //we create a TCPClient object
            mTcpClient = new TcpClient(mTcpClientCallback, mIp, mPort);
            mTcpClient.run(); // will throw network exception if not started in asynctask
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Log.d("tcp", "onProgressUpdate " + values[0]);
        }
    }

    //##############################################################################################
    //endregion
    //region initialize listeners
    //##############################################################################################

    private void initializeCallbacks(){
        mTcpClientCallback = new TcpClient.TcpClientCallback() {
            @Override
            public void messageReceived(String message) {
                Log.i("tcp", "TcpController message recieved " + message);
                handleRecievedMessage(message);
            }

            @Override
            public void statusChanged(TcpClient.TcpConnectStatus status, String msg) {
                Log.i("tcp", "TcpController: " + status.name() + " - " + msg);
                if(status == TcpClient.TcpConnectStatus.CONNECTED
                    || status == TcpClient.TcpConnectStatus.CONNECTING_SUCCESS){
                    mTcpStatus = TcpClient.TcpConnectStatus.CONNECTED;
                } else {
                    mTcpStatus = TcpClient.TcpConnectStatus.DISCONNECTED;
                }

                sendOnStatusChange(status, msg);
            }
        };

        MMissionController.MissionUpdateCallback mMissionUpdateCallback = null;
        MFlightController.FlightControllerUpdateCallback mFlightControllerUpdateCallback = null;

        mMissionUpdateCallback = new MMissionController.MissionUpdateCallback() {
            @Override
            public void onMissionUpdate(String operatorState, String missionState, int nextWp, boolean wpReached) {
                Log.i("missionController", "onMissionUpdate TcpController");
            }

            @Override
            public void onJsonToSend(JSONObject jsonObject) {
                sendTcpMessage(jsonObject.toString());
            }
        };
        mFlightControllerUpdateCallback = new MFlightController.FlightControllerUpdateCallback() {
            @Override
            public void onFlightControllernUpdate(String status) {
            }

            @Override
            public void onBatteryUpdate(int percentage) {
            }

            @Override
            public void onJsonToSend(JSONObject jsonObject) {
                sendTcpMessage(jsonObject.toString());
            }
        };

        MMissionController.attachListener(mMissionUpdateCallback);
        MFlightController.attachListener(mFlightControllerUpdateCallback);
    }

    //##############################################################################################
    //endregion
    //region TcpControllerCallback methods
    //##############################################################################################

    private void sendOnStatusChange(TcpClient.TcpConnectStatus status, String msg){
        for(int i = 0; i < mCallbacks.size(); i++){
            mCallbacks.get(i).onStatusChange(status, msg);
        }
    }

    private void sendOnMessageReciever(String msg){
        for(int i = 0; i < mCallbacks.size(); i++){
            mCallbacks.get(i).onMessageRecieved(msg);
        }
    }

    private void sendOnCommandRecieved(Command com, JSONObject jsonObject){
        for(int i = 0; i < mCallbacks.size(); i++){
            mCallbacks.get(i).onCommandRecieved(com, jsonObject);
        }
    }

    public static void attachListener(TcpControllerCallback tcpControllerCallback){
        if(!mCallbacks.contains(tcpControllerCallback)){
            mCallbacks.add(tcpControllerCallback);
        }
    }

    public static void detachListener(TcpControllerCallback tcpControllerCallback){
        if(mCallbacks.contains(tcpControllerCallback)){
            mCallbacks.remove(tcpControllerCallback);
        }
    }

    //##############################################################################################
    //endregion
    //region utils
    //##############################################################################################

    public static boolean isTcpConnected(){
        return (mTcpStatus == TcpClient.TcpConnectStatus.CONNECTED);
    }

    //##############################################################################################
    //endregion
    //##############################################################################################
}
