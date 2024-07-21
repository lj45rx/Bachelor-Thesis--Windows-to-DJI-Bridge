#include "tcpserver.h"

//#############################################################################
//#############################################################################

TcpServer::TcpServer(QObject *parent) : QObject(parent){
    timer = new QTimer(this);
    QObject::connect(timer, SIGNAL(timeout()),
              this, SLOT(reportMessage()));
}

TcpServer::~TcpServer(){

}

//#############################################################################
//#############################################################################

void TcpServer::startServer(QString ip, QString port){
    messageToMain("trying to start server at <" + ip + ":" + port + ">");
    if(server){
        messageToMain("server already running at <" + ip + ":" + port + ">");
        return;
    }
    server = new QTcpServer(this);
    QObject::connect(server, SIGNAL(newConnection()), this, SLOT(on_newConnection()));

    mIp = ip;
    mPort = port.toInt();

    QHostAddress hostAddr = QHostAddress(mIp);
    quint16 hostPort = abs(mPort);

    if(!server->listen(hostAddr, hostPort)){
        messageToMain("Server could not be started");
        newServerStatus("could not be started", false);
    } else {
        messageToMain("Server running at <" + ip + ":" + port + ">");
        newServerStatus(ip + ":" + port, true);
    }

    //timer->start(1000);
}

void TcpServer::stopServer(){
    if(socket){
        on_disconnected();
        socket = nullptr;
    }
    if(server){
        server->close();
        server = nullptr;
    }
    newServerStatus("not running", false);
    messageToMain("server stopped");
}

void TcpServer::on_newConnection(){
    if(!connected){
        socket = server->nextPendingConnection();
        clientIp = socket->peerAddress().toString();
        connected = true;
        QObject::connect(socket, SIGNAL(readyRead()), SLOT(on_readyRead()));
        QObject::connect(socket, SIGNAL(disconnected()), SLOT(on_disconnected()));
        messageToMain("connected to new client <" + clientIp + ">");
        newClientStatus(clientIp, true);
    } else {
        messageToMain("incomming connection ignored");
    }
}

qint32 TcpServer::ArrayToInt(QByteArray source){
    qint32 temp;
    QDataStream data(&source, QIODevice::ReadWrite);
    data >> temp;
    return temp;
}

void TcpServer::on_readyRead(){

    while (socket->bytesAvailable() > 0){
        buffer.append(socket->readAll());
        /*
        while ((bufferSize == 0 && buffer.size() >= 4) || (bufferSize > 0 && buffer.size() >= bufferSize)) //While can process data, process it
        {
            if (bufferSize == 0 && buffer.size() >= 4) //if size of data has received completely, then store it on our global variable
            {
                bufferSize = ArrayToInt(buffer.mid(0, 4));
                buffer.remove(0, 4);
            }
            if (bufferSize > 0 && buffer.size() >= bufferSize) // If data has received completely, then emit our SIGNAL with the data
            {
                data = buffer.mid(0, bufferSize);
                buffer.remove(0, bufferSize);
                bufferSize = 0;
                messageToMain(data);
                //emit dataReceived(data);
            }
        }
        */
    }

    //qDebug() << "recv: <" + buffer + ">";
    QString temp = buffer.replace("\r\n", "");
    if(temp.endsWith("\n")){
        temp = temp.mid(0, temp.length()-1);
    }
    if(temp.endsWith("\r")){
        temp = temp.mid(0, temp.length()-1);
    }


    //qDebug() << "recv_cop: <" + temp + ">";
    messageToMain(temp, true);
    buffer.clear();
}

void TcpServer::on_disconnected(){
    if(true || socket){ //TODO evt verschoenern
        messageToMain("client <" + clientIp + "> disconnected");
        clientIp = "";
        connected = false;
        socket->close();
        socket = nullptr;
        newClientStatus("not connected", false);
    }
}

void TcpServer::disconnect(){
    timer->stop();
    if(socket){
        socket->disconnectFromHost();
        socket = nullptr;
    } else {
        messageToMain("could not disconnect - no connection");
    }
}

void TcpServer::sendMessage(QString msg){
    if(socket){
        //QByteArray block;
        //QDataStream out(&block, QIODevice::WriteOnly);

        //out << msg + "\r\n";
        messageToMain("snd: " + msg);

        socket->write(msg.toLocal8Bit() + "\r\n");
        socket->flush();

        socket->waitForBytesWritten(3000);
    } else {
        messageToMain("could not send message <" + msg + "> not connected");
    }
}




//#############################################################################
//#############################################################################

void TcpServer::reportMessage(){
    messageToMain("server recieved: <blub>");
}

//#############################################################################
//#############################################################################
