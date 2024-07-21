#ifndef COMMANDS_H
#define COMMANDS_H

#include <QString>
#include <QHash>

enum COMMAND{
        START_MOTOR,
        STOP_MOTOR,
        START,
        LAND,
        CONFIRM_LAND,
        SET_HOME,
        GO_HOME,
        CHECK_MISSION,
        UPLOAD_MISSION,
        START_MISSION,
        PAUSE_MISSION,
        STOP_MISSION,
        FC_STATE,
        BATTERY_UPDATE,
        MISSION_UPDATE,
        RESUME_MISSION,
        REPORT_HOME,
        SEND_MISSION_DATA
};

const int noCommands = 18;

const QString commandStrings[noCommands] = {
    "startEngines",
    "stopEngines",
    "start",
    "land",
    "confirmLanding",
    "setHome",
    "goHome",
    "checkMission",
    "uploadMission",
    "startMission",
    "pauseMission",
    "stopMission",
    "fcState",
    "batteryState",
    "missionUpdate",
    "resumeMission",
    "homeLocation",
    "missionWaypoints"
};

//um warnungen auszuschalten
static QString command(COMMAND programm) __attribute__ ((unused));

static QString command(COMMAND programm){
    if(noCommands <= programm){
        return "out of range";
    }
    return commandStrings[programm];
}

#endif // COMMANDS_H
