#ifndef PARSER_H
#define PARSER_H

#include <QString>
#include <QJsonDocument>
#include <QJsonArray>
#include <QJsonObject>
#include "dronedialog.h"

class Parser : public QObject
{
    Q_OBJECT

public:
    Parser(DroneDialog *dd);

    int parseMessage(QString message);

private:
    DroneDialog *droneDialog = nullptr;

signals:
    void writeToStatus(QString msg);
};

#endif // PARSER_H
