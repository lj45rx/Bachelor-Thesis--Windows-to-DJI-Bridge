package com.example.mst.mav2dvi;

public class StringConstants {

    private StringConstants(){}

    public static final String C_MISSION_WP_COUNT = "waypointCount";
    public static final String C_MISSION_WP_LIST = "waypoints";

    public static final String C_AUTO_FLIGHT_SPEED = "autoSpeed";
    public static final String C_MAX_FLIGHT_SPEED = "maxSpeed";

    public static final String C_GOTO_FIRST_WP_MODE = "goToFirstWaypointMode";
    public static final String C_GOTO_FIRST_WP_MODE_SAFELY = "safely";
    public static final String C_GOTO_FIRST_WP_MODE_POINT_TO_POINT = "pointToPoint";

    public static final String C_POINT_OF_INTEREST = "pointOfInterest";

    public static final String C_HEADING_MODE = "headingMode";
    public static final String C_HEADING_MODE_AUTO = "auto";
    public static final String C_HEADING_MODE_USING_INITIAL = "initial";
    public static final String C_HEADING_MODE_REMOTE_CONTROLLER = "remote";
    public static final String C_HEADING_MODE_WP_HEADING = "waypoint";
    public static final String C_HEADING_MODE_POINT_OF_INTEREST = "pointOfInterest";

    public static final String C_GIMBAL_PITCH_ROTATION_ENABLED = "gimbalPitchRotationEnabled";

    public static final String C_FLIGHT_PATH_MODE = "flightPathMode";
    public static final String C_FLIGHT_PATH_MODE_NORMAL = "normal";
    public static final String C_FLIGHT_PATH_MODE_CURVED = "curved";

    public static final String C_FINISHED_ACTION = "finishedAction";
    public static final String C_FINISHED_ACTION_NO_ACTION = "noAction";
    public static final String C_FINISHED_ACTION_GO_HOME = "goHome";
    public static final String C_FINISHED_ACTION_AUTO_LAND = "autoLand";
    public static final String C_FINISHED_ACTION_GO_FIRST_WP = "goToFirstWp";
    public static final String C_FINISHED_ACTION_CONTINUE = "continue";

    public static final String C_MISSION_REPEAT_TIMES = "repeatTimes";

    public static final String C_EXIT_ON_RC_SIGNAL_LOST = "exitOnRcSignalLost";

    //WaypointMissionAction
    public static final String C_WP_COORDINATES = "crd";
    public static final String C_WP_HEADING = "hdg";
    public static final String C_WP_ACTIONS = "act";
    public static final String C_WP_SPEED = "spd";
    public static final String C_WP_GIMBAL_PITCH = "gim";

    public static final String C_ACT_STAY = "stay";
    public static final String C_ACT_START_TAKE_PHOTO = "photo";
    public static final String C_ACT_START_RECORD = "strtRec";
    public static final String C_ACT_STOP_RECORD = "stopRec";
    public static final String C_ACT_ROTATE_AIRCRAFT = "rotate";
    public static final String C_ACT_GIMBAL_PITCH = "gimPitch";

    //WaypointMission report update
    public static final String C_MISSIONOPERATOR_STATE = "missionOpState";
    public static final String C_MISSION_STATE = "missionState";
    public static final String C_MISSION_NEXT_WP = "nextWp";
    public static final String C_MISSION_WP_REACHED = "wpReached";
}
