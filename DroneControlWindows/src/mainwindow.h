#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include "coordinatelist.h"
#include "statusdialog.h"
#include "dronedialog.h"
#include "tcpserver.h"
#include "parser.h"
#include "commands.h"
#include "actions.h"

#include "libs/libqr/qr/QRPainter.hpp"
#include "libs/libqr/qr/QrCode.hpp"
#include "libs/libqr/qr/QrSegment.hpp"
#include "libs/libqr/qr/BitBuffer.hpp"

#include <QMainWindow>
#include <QFileDialog>
#include <QDebug>
#include <QFileInfo>
#include <QSettings>
#include <QTableView>
#include <QStandardItemModel>

#include <QJsonObject>
#include <QJsonArray>
#include <QJsonDocument>


namespace Ui {
class MainWindow;
}

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = nullptr);
    ~MainWindow();

public slots:
    void on_aboutToQuit();

private: //variables
    Ui::MainWindow *ui;
    StatusDialog *statusDialog = nullptr;
    DroneDialog *droneDialog = nullptr;
    TcpServer *server = nullptr;
    Parser *parser = nullptr;
    CoordinateList *coords = nullptr;
    QTableView *tableView = nullptr;
    QStandardItemModel *tModel = nullptr;
    //QStringList textBrowserData;
    QStringList rememberedConnections;
    bool missionDocumentInitialized = false;
    QJsonDocument missionDocument;

    QString settingsFile = "settings.ini";

    QString mIp = "";
    QString mPort = "";

    int fileReadMode = 0; //0 coords, 1 meter diff


private slots:
    void on_msgFromServer(QString msg, bool fromClient);
    void on_newServerStatus(QString msg, bool connected);
    void on_newClientStatus(QString msg, bool connected);

    void on_msgFromDroneDialog(QString msg, bool sendToPc);

    void on_button_selectFile_clicked();
    void on_pushButton_saveIP_clicked();
    void on_pushButton_deleteIp_clicked();
    void on_comboBox_connect_currentIndexChanged(int index);
    void on_pushButton_clicked();
    void on_pushButton_2_clicked();
    void on_pushButton_startServer_clicked();
    void on_pushButton_disconnectClient_clicked();
    void on_pushButton_sendMsg_clicked();
    void on_pushButton_stopServer_clicked();

    void on_lineEdit_msg_returnPressed();

    void onDataChanged(const QModelIndex&, const QModelIndex&);

    void on_pushButton_resetTable_clicked();
    void on_pushButton_sendMission_clicked();
    void on_pushButton_checkMission_clicked();
    void on_pushButton_uploadMission_clicked();
    void on_pushButton_startMission_clicked();
    void on_pushButton_pauseMission_clicked();
    void on_pushButton_stopMission_clicked();

    void on_pushButton_resumeMission_clicked();

    void updateStatus(QString string);

    void on_pushButton_3_clicked();

private: //functions
    void showQrCode(QString msg);
    void paintQR(QPainter &painter, const QSize sz, const QString &data, QColor fg);
    void init();
    void initTableView();
    void addTableElement(QString lat, QString lon, QString alt, QString act, int opt, int yaw);
    QString openFileDialog();
    void parseFile(QString file);
    void initCoordsTableView();
    void addCoordstableElement(int row, int col, QString data);
    void highlightCoordstableRow(int row);
    void updateMissionOptions(QString option, bool clear = false);
    void saveSettings();
    void loadSettings();

    void setCurrentWaypoint(int index);

    void sendMessage(QString msg);
    void sendMissionToPhone();
};

#endif // MAINWINDOW_H
