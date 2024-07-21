#ifndef ACTIONS_H
#define ACTIONS_H

enum ACTION{
    NONE,
    PHOTO,
    YAW,
    STAY
};

const int noActions = 4;

const QString actionStrings[noActions] = {
    "none",
    "photo",
    "yaw",
    "stay"
};

//um warnungen auszuschalten
static QString action(ACTION action) __attribute__ ((unused));

static QString action(ACTION action){
    if(noActions <= action){
        return "out of range";
    }
    return actionStrings[action];
}

#endif // ACTIONS_H
