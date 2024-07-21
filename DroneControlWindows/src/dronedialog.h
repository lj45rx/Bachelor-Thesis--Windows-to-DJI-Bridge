#ifndef DRONEDIALOG_H
#define DRONEDIALOG_H

#include <QDialog>
#include <QDebug>
#include <QCloseEvent>
#include <QTimer>
#include <QThread>
#include "commands.h"

namespace Ui {
class DroneDialog;
}

class DroneDialog : public QDialog
{
    Q_OBJECT

public:
    explicit DroneDialog(QWidget *parent = 0);
    ~DroneDialog();
    void setDrone(QString drone);
    void updateBatteryValue(int percentage);
    void setDroneAttitude(QString str);
    void setDroneLocation(QString str);
    void setHomeLocation(QString str);
    void setEnginesOn(bool status);
    void setIsFlying(bool status);
    void setSatelliteCount(int count);
    void setMissionOpState(QString str);
    void setMissionState(QString str);
    void setWaypoint(QString str);
    void onConfirmLandingRequested();
    void flash(int times = 3, int ms = 100);

private slots:

    void on_pushButton_setHome_clicked();

    void on_pushButton_goHome_clicked();

    void on_pushButton_confirmland_clicked();

    void on_pushButton_land_clicked();

    void flashH();

    void on_pushButton_startEngines_clicked();

    void on_pushButton_stopEngines_clicked();

private:
    Ui::DroneDialog *ui;
    QTimer *timer = nullptr;
    void toggleColor(bool clear = false);
    bool isFlashing = false;
    int flashTimes = 0;
    void closeEvent(QCloseEvent *event);

signals:
    void sendMessage(QString msg, bool sendToPc);
};

#endif // DRONEDIALOG_H
