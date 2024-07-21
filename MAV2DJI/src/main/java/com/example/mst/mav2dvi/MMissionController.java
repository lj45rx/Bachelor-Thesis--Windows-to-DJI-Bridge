package com.example.mst.mav2dvi;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointExecutionProgress;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecuteState;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.mission.waypoint.WaypointUploadProgress;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.sdkmanager.DJISDKManager;

public class MMissionController {

    //##############################################################################################
    //region variables
    //##############################################################################################

    private final String TAG = "tag";

    public interface MissionUpdateCallback {
        void onMissionUpdate(String operatorState, String missionState, int nextWp, boolean wpReached);
        void onJsonToSend(JSONObject jsonObject);
    }
    private static List<MissionUpdateCallback> mCallbacks;

    private static WaypointMission mMission;
    private WaypointMissionOperatorListener mListener;
    private static final double ONE_METER_OFFSET = 0.00000899322;

    private BaseProduct mProduct = null;
    private static WaypointMissionOperator mWaypointMissionOperator = null;

    private static int temp = 0;

    private String mOperatorStateInfo = "";
    private String mMissionStateInfo = "no Mission created";
    private int mNextWp = -1;
    private boolean mWpReached = false;

    //##############################################################################################
    //endregion
    //region constructor
    //##############################################################################################

    MMissionController(){
        Log.i("tag", "missionController created");

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
            initMissionOperator();
        }
    }

    void onMissionUpdate(){
        Log.i(TAG, "onMissionUpdate called");
        if(mWaypointMissionOperator != null){
            mOperatorStateInfo = mWaypointMissionOperator.getCurrentState().getName();
        } else {
            mOperatorStateInfo = "WaypointMissionOperator == null";
        }

        JSONObject object = getJsonFromMissionState();
        if(object != null){
            sendOnJsonToSend(object);
        }

        sendOnMissionUpdate();
    }

    //##############################################################################################
    //endregion
    //region mission creation
    //##############################################################################################

    private WaypointMission parseMissionFromJson(JSONObject jsonObj){

        //String jsonString = input;
        WaypointMission.Builder builder = new WaypointMission.Builder();
        try {
            //#####################################################################
            //set general mission settings

            if(jsonObj.has(StringConstants.C_MAX_FLIGHT_SPEED)){
                Log.i(TAG, "maxSpeed found: " + jsonObj.optDouble(StringConstants.C_MAX_FLIGHT_SPEED));
                builder.maxFlightSpeed((float)jsonObj.optDouble(StringConstants.C_MAX_FLIGHT_SPEED));
            }

            //auto flight speed bound by maxFlightSpeed
            //can be changed using controller
            //if autoFlightSpeed is 0 moved only by controller
            if(jsonObj.has(StringConstants.C_AUTO_FLIGHT_SPEED)){
                Log.i(TAG, "autoSpeed found: " + jsonObj.optDouble(StringConstants.C_AUTO_FLIGHT_SPEED));
                builder.autoFlightSpeed((float)jsonObj.optDouble(StringConstants.C_AUTO_FLIGHT_SPEED));
            }

            if(jsonObj.has(StringConstants.C_GOTO_FIRST_WP_MODE)){
                switch(jsonObj.getString(StringConstants.C_GOTO_FIRST_WP_MODE)){
                    case StringConstants.C_GOTO_FIRST_WP_MODE_SAFELY:
                        Log.i(TAG, "gotoFirstWpMode safely");
                        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
                        break;
                    case StringConstants.C_GOTO_FIRST_WP_MODE_POINT_TO_POINT:
                        Log.i(TAG, "gotoFirstWpMode pointToPoint");
                        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.POINT_TO_POINT);
                        break;
                }
            }

            if(jsonObj.has(StringConstants.C_POINT_OF_INTEREST)){
                JSONArray coords = jsonObj.getJSONArray(StringConstants.C_POINT_OF_INTEREST);
                double lat = coords.optDouble(0);
                double lon = coords.optDouble(1);
                Log.i(TAG, "pointOfInterest: " + lat + " " + lon);
            }

            if(jsonObj.has(StringConstants.C_HEADING_MODE)){
                switch(jsonObj.getString(StringConstants.C_HEADING_MODE)){
                    case StringConstants.C_HEADING_MODE_AUTO:
                        Log.i(TAG, "headingMode auto");
                        builder.headingMode(WaypointMissionHeadingMode.AUTO);
                        break;
                    case StringConstants.C_HEADING_MODE_WP_HEADING:
                        Log.i(TAG, "headingMode wpHeading");
                        builder.headingMode(WaypointMissionHeadingMode.USING_WAYPOINT_HEADING);
                        break;
                    case StringConstants.C_HEADING_MODE_REMOTE_CONTROLLER:
                        Log.i(TAG, "headingMode controller");
                        builder.headingMode(WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER);
                        break;
                    case StringConstants.C_HEADING_MODE_USING_INITIAL:
                        Log.i(TAG, "headingMode initial");
                        builder.headingMode(WaypointMissionHeadingMode.USING_INITIAL_DIRECTION);
                        break;
                    case StringConstants.C_HEADING_MODE_POINT_OF_INTEREST:
                        Log.i(TAG, "headingMode pointOfInterest");
                        builder.headingMode(WaypointMissionHeadingMode.TOWARD_POINT_OF_INTEREST);
                        break;
                }
            }

            if(jsonObj.has(StringConstants.C_GIMBAL_PITCH_ROTATION_ENABLED)){
                Log.i(TAG, "gimbalPitchRotationEnabled " + jsonObj.optBoolean(StringConstants.C_GIMBAL_PITCH_ROTATION_ENABLED));
                builder.setGimbalPitchRotationEnabled(jsonObj.optBoolean(StringConstants.C_GIMBAL_PITCH_ROTATION_ENABLED));
            }

            if(jsonObj.has(StringConstants.C_FLIGHT_PATH_MODE)){
                switch(jsonObj.getString(StringConstants.C_FLIGHT_PATH_MODE)){
                    case StringConstants.C_FLIGHT_PATH_MODE_NORMAL:
                        Log.i(TAG, "flightPathMode normal");
                        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
                        break;
                    case StringConstants.C_FLIGHT_PATH_MODE_CURVED:
                        Log.i(TAG, "flightPathMode curved");
                        builder.flightPathMode(WaypointMissionFlightPathMode.CURVED);
                        break;
                }
            }

            //repeatTimes = 1 --> run mission 2 times
            if(jsonObj.has(StringConstants.C_MISSION_REPEAT_TIMES)){
                Log.i(TAG, "repeatTimes " + jsonObj.optInt(StringConstants.C_MISSION_REPEAT_TIMES));
                builder.repeatTimes(jsonObj.optInt(StringConstants.C_MISSION_REPEAT_TIMES));
            }

            if(jsonObj.has(StringConstants.C_FINISHED_ACTION)){
                switch(jsonObj.getString(StringConstants.C_FINISHED_ACTION)){
                    case StringConstants.C_FINISHED_ACTION_NO_ACTION:
                        Log.i(TAG, "finishedAction noAction");
                        builder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
                        break;
                    case StringConstants.C_FINISHED_ACTION_GO_HOME:
                        Log.i(TAG, "finishedAction goHome");
                        builder.finishedAction(WaypointMissionFinishedAction.GO_HOME);
                        break;
                    case StringConstants.C_FINISHED_ACTION_CONTINUE:
                        Log.i(TAG, "finishedAction continue");
                        builder.finishedAction(WaypointMissionFinishedAction.CONTINUE_UNTIL_END);
                        break;
                    case StringConstants.C_FINISHED_ACTION_GO_FIRST_WP:
                        Log.i(TAG, "finishedAction goToFirstWp");
                        builder.finishedAction(WaypointMissionFinishedAction.GO_FIRST_WAYPOINT);
                        break;
                    case StringConstants.C_FINISHED_ACTION_AUTO_LAND:
                        Log.i(TAG, "finishedAction autoLand");
                        builder.finishedAction(WaypointMissionFinishedAction.AUTO_LAND);
                        break;
                }
            }

            if(jsonObj.has(StringConstants.C_EXIT_ON_RC_SIGNAL_LOST)){
                Log.i(TAG, "exitOnRcSignalLost " + jsonObj.optBoolean(StringConstants.C_EXIT_ON_RC_SIGNAL_LOST));
                builder.setExitMissionOnRCSignalLostEnabled(jsonObj.optBoolean(StringConstants.C_EXIT_ON_RC_SIGNAL_LOST));
            }

            //#####################################################################
            //create and add Waypoints
            int noCoords = jsonObj.getInt(StringConstants.C_MISSION_WP_COUNT);
            Log.i(TAG, "waypointCount: " + noCoords);

            //containing all waypoints
            JSONArray coordinates = jsonObj.getJSONArray(StringConstants.C_MISSION_WP_LIST);

            //create single Waypoints
            for(int i = 0; i < noCoords; i++){
                Log.i(TAG, " ");

                //contains data of single waypoint
                JSONObject wpData = coordinates.getJSONObject(i);
                //contains coordinate data of waypoint
                JSONArray wpCoords = wpData.getJSONArray(StringConstants.C_WP_COORDINATES);
                double lat = wpCoords.optDouble(0);
                double lon = wpCoords.optDouble(1);
                double alt = wpCoords.optDouble(2);
                Log.i(TAG, "wp " + i + ": " + lat + " " + lon + " " + alt);

                final Waypoint tempWp = new Waypoint(lat, lon, (float)alt);

                if(wpData.has(StringConstants.C_WP_HEADING)){
                    Log.i(TAG, "wpHeading " + wpData.optInt(StringConstants.C_WP_HEADING));
                    tempWp.heading = wpData.optInt(StringConstants.C_WP_HEADING); //int
                }

                if(wpData.has(StringConstants.C_WP_SPEED)){ //float
                    Log.i(TAG, "wpSpeed " + wpData.optDouble(StringConstants.C_WP_SPEED));
                    tempWp.speed = (float)wpData.optDouble(StringConstants.C_WP_SPEED);
                }
                if(wpData.has(StringConstants.C_WP_GIMBAL_PITCH)){ //float
                    Log.i(TAG, "has gimbalPitch");
                    Log.i(TAG, "wpGimbalPitch " + wpData.optDouble(StringConstants.C_WP_GIMBAL_PITCH));
                    tempWp.gimbalPitch = (float)wpData.optDouble(StringConstants.C_WP_GIMBAL_PITCH);
                }

               if(wpData.has(StringConstants.C_WP_ACTIONS)) {
                    //list of actions for point
                    JSONArray wpActions = wpData.getJSONArray(StringConstants.C_WP_ACTIONS);

                    for(int j = 0; j < wpActions.length(); j++){
                        //single action
                        JSONArray action = wpActions.getJSONArray(j);
                        String actionString = action.getString(0);
                        //some actions need no params - use default of 0 (will be ignored anyway)
                        int actionParam = action.optInt(1, 0);

                        WaypointAction tempWpAct = null;

                        switch (actionString){
                            case StringConstants.C_ACT_STAY:
                                Log.i(TAG, "wpAction Stay " + actionParam);
                                tempWpAct = new WaypointAction(WaypointActionType.STAY, actionParam);
                                break;
                            case StringConstants.C_ACT_ROTATE_AIRCRAFT:
                                Log.i(TAG, "wpAction Rotate " + actionParam);
                                tempWpAct = new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT, actionParam);
                                break;
                            case StringConstants.C_ACT_GIMBAL_PITCH:
                                Log.i(TAG, "wpAction Gimbal " + actionParam);
                                tempWpAct = new WaypointAction(WaypointActionType.GIMBAL_PITCH, actionParam);
                                break;
                            case StringConstants.C_ACT_START_TAKE_PHOTO:
                                Log.i(TAG, "wpAction Photo " + actionParam);
                                tempWpAct = new WaypointAction(WaypointActionType.START_TAKE_PHOTO, actionParam);
                                break;
                            case StringConstants.C_ACT_START_RECORD:
                                Log.i(TAG, "wpAction StartRecord " + actionParam);
                                tempWpAct = new WaypointAction(WaypointActionType.START_RECORD, actionParam);
                                break;
                            case StringConstants.C_ACT_STOP_RECORD:
                                Log.i(TAG, "wpAction StopRecord " + actionParam);
                                tempWpAct = new WaypointAction(WaypointActionType.STOP_RECORD, actionParam);
                                break;
                        }

                        if(tempWpAct != null) {
                            if (!tempWp.addAction(tempWpAct)) {
                                Log.e("MissionBuilder", "error on add waypoint");
                                //Action incorrect or maximum action reached
                            }
                        }
                    }
                }

                builder.addWaypoint(tempWp);
            }

            //#####################################################################
            //finalize and create mission
            WaypointMission mission = builder.build();
            Log.i(TAG, "mission created");

            //check created mission
            DJIError djiError = mission.checkParameters();
            if(djiError != null) {
                //error
                mMissionStateInfo = djiError.getDescription();
                onMissionUpdate();
                return null;
            } else {
                //no error
                mMissionStateInfo = "mission created successfully";
                Log.i(TAG, mMissionStateInfo);
                onMissionUpdate();
                return mission;
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            mMissionStateInfo = "exception on parse json";
            Log.i(TAG, mMissionStateInfo);
            onMissionUpdate();
            return null;
        }
    }

    private void loadMission(){
        if(mMission != null){
            DJIError djiError = mWaypointMissionOperator.loadMission(mMission);
            if(djiError == null){
                uploadMission();
            }
        }
    }

    private void uploadMission(){ //called automatically from loadMission if no error
        if(WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(mWaypointMissionOperator.getCurrentState())
                || WaypointMissionState.READY_TO_UPLOAD.equals(mWaypointMissionOperator.getCurrentState()))
        {
            mWaypointMissionOperator.uploadMission(getCallback());
        }
    }

    //##############################################################################################
    //endregion
    //region mission controll
    //##############################################################################################

    public void startMission(){
        if (mMission != null && mWaypointMissionOperator != null) {
            mWaypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {

                }
            });
        } else {
            //mission or operator not ready
        }
    }


    public void pauseMission(){
        if(mMission != null && mWaypointMissionOperator != null){
            mWaypointMissionOperator.pauseMission(getCallback());
        }
    }

    public void resumeMission(){
        if(mMission != null && mWaypointMissionOperator != null){
            mWaypointMissionOperator.resumeMission(getCallback());
        }
    }

    public void stopMission(){
        if(mMission != null && mWaypointMissionOperator != null){
            mWaypointMissionOperator.stopMission(getCallback());
        }
    }

    //##############################################################################################
    //endregion
    //region create jsonObjects
    //##############################################################################################

    private JSONObject getJsonFromMissionState(){
        JSONObject object = new JSONObject();
        try {
            object.put("type", Command.REPORT_MISSION.commandText);
            object.put(StringConstants.C_MISSIONOPERATOR_STATE, mOperatorStateInfo);
            object.put(StringConstants.C_MISSION_STATE, mMissionStateInfo);
            object.put(StringConstants.C_MISSION_NEXT_WP, mNextWp);
            object.put(StringConstants.C_MISSION_WP_REACHED, mWpReached);

            return object;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    //##############################################################################################
    //endregion
    //region initialize listeners
    //##############################################################################################

    private WaypointMissionOperatorListener getListener(){
        return new WaypointMissionOperatorListener() {
            @Override
            public void onDownloadUpdate(@NonNull WaypointMissionDownloadEvent waypointMissionDownloadEvent) {

            }

            @Override
            public void onUploadUpdate(@NonNull WaypointMissionUploadEvent waypointMissionUploadEvent) {
                WaypointUploadProgress uploadProgress = waypointMissionUploadEvent.getProgress();

                if(uploadProgress == null){
                    //null if there is an error during upload
                    DJIError error = waypointMissionUploadEvent.getError();

                    if(error == null){
                        mMissionStateInfo = "mission upload success";
                    } else {
                        mMissionStateInfo = "mission upload error " + error;
                    }
                } else {
                    int wpCount = uploadProgress.totalWaypointCount;
                    boolean isSummaryUploaded = uploadProgress.isSummaryUploaded;
                    int currentWp = uploadProgress.uploadedWaypointIndex; //-1 if none uploaded yet

                    if(isSummaryUploaded && currentWp == wpCount){ //upload finished
                        //report upload finished
                        mMissionStateInfo = "upload finished";
                    } else { //not finished yet
                        //report uploading wp currentWp+1
                        mMissionStateInfo = "uploading " + (currentWp+1) + "/" + wpCount;
                    }
                }

                DJIError error = waypointMissionUploadEvent.getError();
                if(error != null){
                    mMissionStateInfo = error.getDescription();
                }

                onMissionUpdate();
            }

            @Override
            public void onExecutionUpdate(@NonNull WaypointMissionExecutionEvent waypointMissionExecutionEvent) {

                WaypointExecutionProgress execProgress = waypointMissionExecutionEvent.getProgress();

                if(execProgress == null){
                    //null if there is an error during execution
                    DJIError error = waypointMissionExecutionEvent.getError();

                    //report error
                    mMissionStateInfo = "mission execution error " + error;

                } else {
                    mNextWp = execProgress.targetWaypointIndex;
                    mWpReached = execProgress.isWaypointReached;

                    WaypointMissionExecuteState execState = execProgress.executeState;
                    mMissionStateInfo = execState.name();

                    //(http://developer.dji.com/api-reference/android-api/Components/Missions/DJIWaypointMission.html#djiwaypointmission_djiwaypointmissionexecutestate_inline)

                    switch(execState){
                        case INITIALIZING:
                            //moving to first wp
                            break;
                        case RETURN_TO_FIRST_WAYPOINT:
                            //returning to first wp
                            break;
                        case MOVING:
                        case CURVE_MODE_MOVING:
                            //moving to next wp
                            break;
                        case CURVE_MODE_TURNING:
                            //turning
                            break;
                        case BEGIN_ACTION:
                        case DOING_ACTION:
                            //doing wp actions
                            break;
                        case FINISHED_ACTION:
                            //action finished
                            break;
                        case PAUSED:
                            //paused by user
                            break;
                    }
                }

                onMissionUpdate();
            }

            @Override
            public void onExecutionStart() {
                //report start
                mMissionStateInfo = "missionStarted";
                onMissionUpdate();
            }

            @Override
            public void onExecutionFinish(@Nullable DJIError djiError) {
                if(djiError != null){
                    //report error
                    mMissionStateInfo = "mission finished on error " + djiError.getDescription(); //TODO + error
                } else {
                    //report finished
                    mMissionStateInfo = "mission finished on success";
                }
                onMissionUpdate();
            }
        };
    }

    public void initTcpControllerCallback(){
        TcpController.TcpControllerCallback mTcpControllerCallback = new TcpController.TcpControllerCallback() {
            @Override
            public void onStatusChange(TcpClient.TcpConnectStatus status, String msg) {

            }

            @Override
            public void onMessageRecieved(String msg) {

            }

            @Override
            public void onCommandRecieved(Command com, JSONObject jsonObject) {
                Log.i("tcp", "MMissionController command recieved " + com.name());

                //if MissionOperator is null try to create it
                if(mWaypointMissionOperator == null){
                    if(mProduct != null && mProduct.isConnected()){
                        initMissionOperator();
                        Log.i("TODO", "waypointMissionOperator in conCommandRecieved initialisiert");
                    }
                }

                //cases that can be executed without MissionOperator
                switch (com){
                    case MISSION_COORDINATES:
                        mMission = parseMissionFromJson(jsonObject);
                        break;
                }

                //cases that need MissionOperator
                if(mWaypointMissionOperator != null){
                    switch(com){
                        case MISSION_UPLOAD:
                            loadMission();
                            //uploadMission();
                            break;
                        case MISSION_START:
                            startMission();
                            break;
                        case MISSION_STOP:
                            stopMission();
                            break;
                        case MISSION_PAUSE:
                            pauseMission();
                            break;
                        case MISSION_RESUME:
                            resumeMission();
                            break;
                    }

                }



            }
        };
        TcpController.attachListener(mTcpControllerCallback);
    }

    //##############################################################################################
    //endregion
    //region MissionUpdateCallback methods
    //##############################################################################################

    private void sendOnMissionUpdate(){
        for(int i = 0; i < mCallbacks.size(); i++){
            mCallbacks.get(i).onMissionUpdate(mOperatorStateInfo, mMissionStateInfo, mNextWp, mWpReached);
        }
    }

    private void sendOnJsonToSend(JSONObject object){
        Log.i("tag", "missionController sendOnJsonToSend");
        for(int i = 0; i < mCallbacks.size(); i++){
            mCallbacks.get(i).onJsonToSend(object);
        }
    }

    public static void attachListener(MissionUpdateCallback missionUpdateCallback){
        if(!mCallbacks.contains(missionUpdateCallback)){
            mCallbacks.add(missionUpdateCallback);
        }
    }

    public static void detachListener(MissionUpdateCallback missionUpdateCallback){
        if(mCallbacks.contains(missionUpdateCallback)){
            mCallbacks.remove(missionUpdateCallback);
        }
    }

    //##############################################################################################
    //endregion
    //region utils
    //##############################################################################################

    private CommonCallbacks.CompletionCallback getCallback(){
        return new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError != null){

                } else {

                }
            }
        };
    }

    //##############################################################################################
    //endregion
    //##############################################################################################

    private void initMissionOperator(){
        mWaypointMissionOperator = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        Log.i("missionController", "missionOperator created");
        mListener = getListener();
        mWaypointMissionOperator.addListener(mListener);
        onMissionUpdate();
    }

    //#########################################################################
    //#########################################################################
}