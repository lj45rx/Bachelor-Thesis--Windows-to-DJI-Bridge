#include "coordinatelist.h"

CoordinateList::CoordinateList(){
    noOfPoints = 0;
}

void CoordinateList::addCoordinate(double lat, double lon, double alt, ACTION action, int actionOption, int yaw){
    noOfPoints++;
    latitudes.append(lat);
    longitudes.append(lon);
    altitudes.append(alt);
    actions.append(action);
    actionOptions.append(actionOption);
    yaws.append(yaw);
}

int CoordinateList::noOfCoordinates(){
    return noOfPoints;
}

void CoordinateList::printCoordinates(){
    for(int i = 0; i < noOfPoints; i++){
        qDebug() << i << ": (" << latitudes[i] << longitudes[i] << altitudes[i] << actions[i] << actionOptions[i] << yaws[i] << ")";
    }
}

void CoordinateList::clear(){
    latitudes.clear();
    longitudes.clear();
    altitudes.clear();
    actions.clear();
    actionOptions.clear();
    yaws.clear();
    noOfPoints = 0;
}

double CoordinateList::getElement(int row, int col){
    if(noOfPoints < row){
        return -1;
    } else {
        switch(col){
        case 0:
            return latitudes[row];
            break;
        case 1:
            return longitudes[row];
            break;
        case 2:
            return altitudes[row];
            break;
        case 4:
            return actionOptions[row];
            break;
        }
    }
}

ACTION CoordinateList::getAction(int row){
    return actions[row];
}

int CoordinateList::getActionOption(int row){
    return actionOptions[row];
}

int CoordinateList::getYaw(int row){
    return yaws[row];
}

double CoordinateList::setElement(int row, int col, double element){
    if(noOfPoints < row){
        return -1;
    } else {
        switch(col){
        case 0:
            latitudes[row] = element;
            break;
        case 1:
            longitudes[row] = element;
            break;
        case 2:
            altitudes[row] = element;
            break;
        case 4:
            actionOptions[row] = element;
            break;
        }
    }
    return 0;
}

QString CoordinateList::getCoordinateLine(int line){
    return QString("(%1, %2, %3, %4, %5, %6, %7)")
            .arg(line)
            .arg(latitudes[line])
            .arg(longitudes[line])
            .arg(altitudes[line])
            .arg(actions[line])
            .arg(actionOptions[line])
            .arg(yaws[line]);
}

