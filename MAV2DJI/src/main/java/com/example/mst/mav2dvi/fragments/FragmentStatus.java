package com.example.mst.mav2dvi.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.mst.mav2dvi.MMissionController;
import com.example.mst.mav2dvi.R;

import org.json.JSONObject;

import dji.common.battery.BatteryState;
import dji.common.error.DJIError;
import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class FragmentStatus extends Fragment{

    private EditText editText_fcState = null;

    private BaseProduct mProduct = null;
    private FlightController mFlightController = null;
    private Battery mBattery = null;
    private String mBatteryLevel = "not initialized";

    MMissionController.MissionUpdateCallback mMissionUpdateCallback = null;

    public FragmentStatus() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //the following works for activities but will cause a crash in fragments
        //editText_fcState = getView().findViewById(R.id.f1_editText_fcState);

        mProduct = DJISDKManager.getInstance().getProduct();
        if(mProduct != null){
            initBatteryCallback();
            initFcStateCallback();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fragment1, container, false);

        editText_fcState = view.findViewById(R.id.f1_editText_fcState);

        mMissionUpdateCallback = new MMissionController.MissionUpdateCallback() {
            @Override
            public void onMissionUpdate(String operatorState, String missionState, int nextWp, boolean wpReached) {
                Log.i("missionController", "onMissionUpdate fragment1");

            }

            @Override
            public void onJsonToSend(JSONObject jsonObject) {

            }
        };
        MMissionController.attachListener(mMissionUpdateCallback);

        return view;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        MMissionController.detachListener(mMissionUpdateCallback);
    }

    private void onFcUpdate(final String status){
        if(!isVisible()){
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                editText_fcState.setText(status);
            }
        });
    }

    private void initBatteryCallback(){
        mBattery = mProduct.getBattery();

        mBattery.setStateCallback(new BatteryState.Callback() {
            @Override
            public void onUpdate(BatteryState batteryState) {
                mBatteryLevel = "" + batteryState.getChargeRemainingInPercent();
            }
        });
    }

    private void initFcStateCallback(){
        mFlightController = ((Aircraft)mProduct).getFlightController();

        try {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState djiFlightControllerState) {
                    LocationCoordinate3D pos = djiFlightControllerState.getAircraftLocation();
                    Attitude attitude = djiFlightControllerState.getAttitude();

                    //drone name and battery level
                    String newState = mProduct.getModel().getDisplayName();
                    newState += " (battery: " + mBatteryLevel + "%)";

                    //gps signal
                    newState += "\n" + "gps level: " + djiFlightControllerState.getGPSSignalLevel().name();
                    newState += " (" + djiFlightControllerState.getSatelliteCount() + ")";

                    //position
                    newState += "\n" + "lat: " + pos.getLatitude();
                    newState += "\n" + "lon: " + pos.getLongitude();
                    newState += "\n" + "alt: " + pos.getAltitude();

                    //isFLying/enginesOn
                    newState += "\n" + "isFlying: " + djiFlightControllerState.isFlying();
                    newState += " enginesOn: " + djiFlightControllerState.areMotorsOn();

                    //attitude
                    newState += "\n" + "roll: " + attitude.roll;
                    newState += " pitch: " + attitude.pitch;
                    newState += " raw: " + attitude.yaw;

                    onFcUpdate(newState);
                }
            });
        } catch (Exception ignored) {

        }
    }

    public CommonCallbacks.CompletionCallback getCallback(){
        return new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
            }
        };
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
