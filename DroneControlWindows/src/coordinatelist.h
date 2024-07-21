#ifndef COORDINATELIST_H
#define COORDINATELIST_H

#include <QList>
#include <QDebug>
#include "actions.h"

class CoordinateList
{
public:
    CoordinateList();
    void addCoordinate(double lat, double lon, double heigth, ACTION action = ACTION::NONE, int actionOption = 0, int yaw = 0);
    int noOfCoordinates();
    void printCoordinates();
    void clear();

    double getElement(int row, int col);
    ACTION getAction(int row);
    int getActionOption(int row);
    int getYaw(int row);
    double setElement(int row, int col, double element);
    QString getCoordinateLine(int line);

private:
    int noOfPoints;
    QList<double> latitudes;
    QList<double> longitudes;
    QList<double> altitudes;
    QList<ACTION> actions;
    QList<int> actionOptions;
    QList<int> yaws;
};

#endif // COORDINATELIST_H
