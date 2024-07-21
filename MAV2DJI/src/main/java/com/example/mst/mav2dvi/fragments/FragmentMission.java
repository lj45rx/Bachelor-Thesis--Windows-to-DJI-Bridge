package com.example.mst.mav2dvi.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.mst.mav2dvi.ApplicationCore;
import com.example.mst.mav2dvi.MMissionController;
import com.example.mst.mav2dvi.R;

import org.json.JSONObject;

import java.util.Objects;

public class FragmentMission extends Fragment implements View.OnClickListener {
    private Button button_missionStart = null;
    private Button button_missionPause = null;
    private Button button_missionResume = null;
    private Button button_missionStop = null;

    private EditText editText_missionStatus = null;

    MMissionController.MissionUpdateCallback mMissionUpdateCallback = null;
    MMissionController mMissionController = null;

    public FragmentMission() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_mission, container, false);

        findInitUiElements(view);
        mMissionController = ApplicationCore.getMMissionController();

        return view;
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        MMissionController.detachListener(mMissionUpdateCallback);
    }

    private void findInitUiElements(View view){
        button_missionStart = (Button) view.findViewById(R.id.button_missionStart);
        button_missionStart.setOnClickListener(this);
        button_missionPause = (Button) view.findViewById(R.id.button_missionPause);
        button_missionPause.setOnClickListener(this);
        button_missionResume = (Button) view.findViewById(R.id.button_missionResume);
        button_missionResume.setOnClickListener(this);
        button_missionStop = (Button) view.findViewById(R.id.button_missionStop);
        button_missionStop.setOnClickListener(this);

        editText_missionStatus = (EditText)view.findViewById(R.id.editText_missionStatus);

        mMissionUpdateCallback = new MMissionController.MissionUpdateCallback() {
            @Override
            public void onMissionUpdate(String operatorState, String missionState, int nextWp, boolean wpReached) {
                Log.i("missionController", "onMissionUpdate fragmentMission");

                String msg = operatorState
                        + "\n" + missionState;
                if(wpReached){
                    msg += "\nWaypoint " + nextWp;
                } else {
                    msg += "\ngoing to Waypoint " + nextWp;
                }

                updateMissionStatus(msg);
            }

            @Override
            public void onJsonToSend(JSONObject jsonObject) {

            }
        };
        MMissionController.attachListener(mMissionUpdateCallback);
    }

    private void updateMissionStatus(final String msg){
        Objects.requireNonNull(getActivity()).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(editText_missionStatus != null){
                    editText_missionStatus.setText(msg);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        Log.i("tag", "FragmentMission - buttonClicked ");
        switch (view.getId()){
            case R.id.button_missionStart:
                mMissionController.startMission();
                break;
            case R.id.button_missionPause:
                mMissionController.pauseMission();
                break;
            case R.id.button_missionResume:
                mMissionController.resumeMission();
                break;
            case R.id.button_missionStop:
                mMissionController.stopMission();
                break;
        }

        editText_missionStatus.setText(editText_missionStatus.getText() + "\nclicked");
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
