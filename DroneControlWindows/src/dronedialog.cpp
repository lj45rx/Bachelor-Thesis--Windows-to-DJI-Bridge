#include "dronedialog.h"
#include "ui_dronedialog.h"

//#############################################################################
//#############################################################################

DroneDialog::DroneDialog(QWidget *parent) :
    QDialog(parent),
    ui(new Ui::DroneDialog)
{
    ui->setupUi(this);
    QDialog::setWindowTitle("Drone");
}

DroneDialog::~DroneDialog(){
    delete ui;
}

void DroneDialog::closeEvent(QCloseEvent *event){
    static int times = 0;
    times++;
    if(times < 5){
        event->ignore();
    }
}

//#############################################################################
//data from the outside
//#############################################################################

void DroneDialog::setDrone(QString drone){
    ui->lineEdit_drone->setText(drone);
}

void DroneDialog::setEnginesOn(bool status){
    if(status){
        ui->lineEdit_enginesOne->setText("engines running");
    } else {
        ui->lineEdit_enginesOne->setText("engines off");
    }
}

void DroneDialog::setIsFlying(bool status){
    if(status){
        ui->lineEdit_isFlying->setText("drone is flying");
    } else {
        ui->lineEdit_isFlying->setText("not flying");
    }
}

void DroneDialog::setSatelliteCount(int count){
    ui->lineEdit_satellite->setText("no satellites: " + QString::number(count));
}

void DroneDialog::setMissionOpState(QString str){
    ui->lineEdit_missionOp->setText(str);
}

void DroneDialog::setMissionState(QString str){
    ui->lineEdit_missionState->setText(str);
}

void DroneDialog::setWaypoint(QString str){
    ui->lineEdit_waypoint->setText(str);
}

void DroneDialog::updateBatteryValue(int percentage){
    qDebug() << "battery value " << percentage << "recieved";
    ui->progressBar_battery->setValue(percentage);
}

void DroneDialog::setDroneAttitude(QString str){
    ui->lineEdit_attitude->setText(str);
}

void DroneDialog::setDroneLocation(QString str){
    ui->lineEdit_location->setText(str);
}

void DroneDialog::setHomeLocation(QString str){
    ui->lineEdit_home->setText(str);
}

void DroneDialog::onConfirmLandingRequested(){
    flash();
    ui->pushButton_confirmland->setStyleSheet("background-color: HOTPINK;");
}

//#############################################################################
//flashing erlauben
//#############################################################################

void DroneDialog::toggleColor(bool clear){
    static int blub = 0;
    blub++;
    if(blub%2 == 0 || clear){
        //ui->lineEdit_3->setStyleSheet("background-color: none;");
        this->setStyleSheet("background-color: none;");
        blub = 0;
    } else {
        //ui->lineEdit_3->setStyleSheet("background-color: blue;");
        this->setStyleSheet("background-color: Salmon;");
    }
    this->repaint();
}

void DroneDialog::flash(int times, int ms){
    if(isFlashing){
        return;
    }
    isFlashing = true;

    if(timer == nullptr){
        qDebug() << "timer created";
        timer = new QTimer(this);
        connect(timer, SIGNAL(timeout()),
                  this, SLOT(flashH()));
    }
    flashTimes = times*2-1;
    toggleColor();
    timer->start(ms);
}

void DroneDialog::flashH(){
    toggleColor();
    flashTimes--;
    if(!flashTimes){
        timer->stop();
        isFlashing = false;
    }
}

//#############################################################################
//knoepfe
//#############################################################################

void DroneDialog::on_pushButton_setHome_clicked(){
    sendMessage(command(COMMAND::SET_HOME), true);
}

void DroneDialog::on_pushButton_goHome_clicked(){
    sendMessage(command(COMMAND::GO_HOME), true);
}

void DroneDialog::on_pushButton_confirmland_clicked(){
    ui->pushButton_confirmland->setStyleSheet("background-color: none;");
    sendMessage(command(COMMAND::CONFIRM_LAND), true);
}

void DroneDialog::on_pushButton_land_clicked(){
    sendMessage(command(COMMAND::LAND), true);
}

void DroneDialog::on_pushButton_startEngines_clicked(){
    sendMessage(command(COMMAND::START_MOTOR), true);
}

void DroneDialog::on_pushButton_stopEngines_clicked(){
    sendMessage(command(COMMAND::STOP_MOTOR), true);
}
