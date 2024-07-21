package com.example.mst.mav2dvi;


//https://howtodoinjava.com/java/enum/java-enum-string-example/

import java.util.HashMap;
import java.util.Map;

public enum Command {
    //##############################################################################################
    //list of recognized commands
    UNKNOWN("unknown", "input was invalid"), //returned if no hit is found

    FC_STATE("fcState"),

    SET_HOME("setHome"),
    GO_HOME("goHome"),
    CANCEL_GO_HOME("cancelGoHome"),

    START_ENGINES("startEngines", "trying to start the engines"),
    STOP_ENGINES("stopEngines", "trying to stop the engines"),

    REPORT_BATTERY("batteryState"),

    MISSION_COORDINATES("missionWaypoints", "data to create a mission"),
    REPORT_MISSION("missionUpdate"),
    MISSION_UPLOAD("uploadMission"),
    MISSION_START("startMission"),
    MISSION_STOP("stopMission"),
    MISSION_PAUSE("pauseMission"),
    MISSION_RESUME("resumeMission"),

    LAND("land", "trying to land"),
    CANCEL_LAND("cancelLand"),
    START("start", "start and go to 1.2 meters height"),
    CANCEL_START("cancelStart");

    //##############################################################################################
    public String commandText;
    public String commandDescription;

    //Lookup table
    private static final Map<String, Command> lookup = new HashMap<>();

    //##############################################################################################
    Command(String _commandText, String _commandDescription){
        this.commandText = _commandText;
        this.commandDescription = _commandDescription;
    }

    Command(String _commandText){
        this.commandText = _commandText;
        this.commandDescription = "no Description set for " + _commandText;
    }


    //Populate the lookup table on loading time
    static{
        for(Command comm : Command.values()){
            lookup.put(comm.commandText, comm);
        }
    }

    //##############################################################################################
    /*
     * get command from String
     * eg.   Command cmd = Command.getCommandFromString(input);
     *       String cmdDescription = cmd.getDescription;
     * */

    public static Command getCommandFromString(String text) {
        Command res = lookup.get(text);
        if(res == null){
            return Command.UNKNOWN;
        } else {
            return res;
        }
    }
    //##############################################################################################
}