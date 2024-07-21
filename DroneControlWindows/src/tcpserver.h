#ifndef TCPSERVER_H
#define TCPSERVER_H

#include <QObject>
#include <QTimer>
#include <QDebug>
#include <QTcpServer>
#include <QTcpSocket>
#include <QDataStream>
#include <math.h>

class TcpServer : public QObject
{
    Q_OBJECT
public:
    TcpServer(QObject *parent = 0);
    ~TcpServer();
    void startServer(QString ip, QString port);
    void stopServer();
    void disconnect();
    void sendMessage(QString msg);

public slots:
    void on_newConnection();
    void on_readyRead();
    void on_disconnected();

private:
    QTcpServer *server = nullptr;
    QTcpSocket *socket = nullptr;
    QTimer *timer = nullptr;
    bool connected = false;
    QString mIp;
    int mPort;
    QString clientIp = "";

    QByteArray buffer;
    //qint32 bufferSize;
    //QByteArray data;

private: //functions
    qint32 ArrayToInt(QByteArray source);

private slots:
    void reportMessage();

signals:
    void messageToMain(QString msg, bool fromClient = false);
    void newServerStatus(QString msg, bool connected);
    void newClientStatus(QString msg, bool connected);
};

#endif // TCPSERVER_H
