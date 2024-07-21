#include "parser.h"

Parser::Parser(DroneDialog *dd){
    droneDialog = dd;
}

int Parser::parseMessage(QString message){

    //if multiple jsons contained in message split and work on individual ones
    QStringList list = message.split("}", Qt::SkipEmptyParts);
    for(int i = 0; i < list.size()-1; i++){
        parseMessage(list[i] + "}");
    }
    message = list[list.size()-1] + "}";


    QJsonDocument jsonDoc = QJsonDocument::fromJson(message.toUtf8());
    QJsonObject jsonObj;

    if(!jsonDoc.isNull()){
        if(jsonDoc.isObject()){
            jsonObj = jsonDoc.object();
        }
        else{
            qDebug() << "Document is not an object" << Qt::endl;
        }
    }else{
        qDebug() << "Invalid JSON...\n" << Qt::endl;
        return 0;
    }

    QJsonValue typeValue = jsonObj.value("type");
    QString typeString = "";
    if(typeValue != QJsonValue::Undefined){
         typeString = typeValue.toString();



        if(typeString == command(COMMAND::FC_STATE)){
            qDebug() << "fcstate wird geparst";

            if(jsonObj.value("name") != QJsonValue::Undefined){
                droneDialog->setDrone(jsonObj.value("name").toString());
            }

            if(jsonObj.value("isFlying") != QJsonValue::Undefined){
                droneDialog->setIsFlying(jsonObj.value("isFlying").toBool());
            }

            if(jsonObj.value("areMotorsOn") != QJsonValue::Undefined){
                droneDialog->setEnginesOn(jsonObj.value("areMotorsOn").toBool());
            }

            if(jsonObj.value("satelliteCount") != QJsonValue::Undefined){
                droneDialog->setSatelliteCount(jsonObj.value("satelliteCount").toInt());
            }

            if(jsonObj.value("homeLocation") != QJsonValue::Undefined){
                droneDialog->setHomeLocation(jsonObj.value("homeLocation").toString());
            }

            if(jsonObj.value("attitude") != QJsonValue::Undefined){
                QJsonArray array = jsonObj.value("attitude").toArray();
                QString str = "r: " + QString::number(array.at(0).toDouble());
                str += " y: " + QString::number(array.at(1).toDouble());
                str += " p: " + QString::number(array.at(2).toDouble());
                droneDialog->setDroneAttitude(str);
            }

            if(jsonObj.value("location") != QJsonValue::Undefined){
                QJsonArray array = jsonObj.value("location").toArray();
                QString str = "r: " + QString::number(array.at(0).toDouble());
                str += " y: " + QString::number(array.at(1).toDouble());
                str += " p: " + QString::number(array.at(2).toDouble());
                droneDialog->setDroneLocation(str);
            } else {
                droneDialog->setDroneLocation("unknown");
            }
        } else if(typeString == command(COMMAND::BATTERY_UPDATE)){
            if(jsonObj.value("value") != QJsonValue::Undefined){
                droneDialog->updateBatteryValue(jsonObj.value("value").toInt());
            }
        } else if(typeString == command(COMMAND::MISSION_UPDATE)){
            //operator state
            if(jsonObj.value("missionOpState") != QJsonValue::Undefined){
                droneDialog->setMissionOpState(jsonObj.value("missionOpState").toString());
            }
            //mission state
            if(jsonObj.value("missionState") != QJsonValue::Undefined){
                droneDialog->setMissionState(jsonObj.value("missionState").toString());
            }

            //waypoint info
            QString msg = "";
            if(jsonObj.value("wpReached") != QJsonValue::Undefined){
                if(!jsonObj.value("wpReached").toBool()){
                    msg += "Moving to ";
                }
            }

            if(jsonObj.value("nextWp") != QJsonValue::Undefined){
                msg += "Waypoint " + QString::number(jsonObj.value("nextWp").toInt());
            }

            droneDialog->setWaypoint(msg);
        }
    }

    if(typeString != command(COMMAND::FC_STATE)){
        //only write if is not fcStatus
       writeToStatus(message);
    }

    return 0;
}
