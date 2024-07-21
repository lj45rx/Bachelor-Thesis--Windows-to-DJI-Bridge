#include "mainwindow.h"
#include "ui_mainwindow.h"

//#############################################################################
//#############################################################################

MainWindow::MainWindow(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::MainWindow)
{
    ui->setupUi(this);
    init();
    loadSettings();
}

MainWindow::~MainWindow(){
    delete ui;
}

void MainWindow::sendMessage(QString msg){
    server->sendMessage(msg);
}

void MainWindow::init(){
    coords = new CoordinateList();
    server = new TcpServer(this);
    connect(server, SIGNAL(messageToMain(QString, bool)),
            this, SLOT(on_msgFromServer(QString, bool)));
    connect(server, SIGNAL(newServerStatus(QString, bool)),
            this, SLOT(on_newServerStatus(QString, bool)));
    connect(server, SIGNAL(newClientStatus(QString, bool)),
            this, SLOT(on_newClientStatus(QString, bool)));

    statusDialog = new StatusDialog(this);
    statusDialog->show();

    droneDialog = new DroneDialog(this);
    connect(droneDialog, SIGNAL(sendMessage(QString, bool)),
            this, SLOT(on_msgFromDroneDialog(QString, bool)));
    droneDialog->show();
    parser = new Parser(droneDialog);
    connect(parser, SIGNAL(writeToStatus(QString)),
            this, SLOT(updateStatus(QString)));

    //design stuff etc
    ui->lineEdit_serverStatus->setStyleSheet("background-color: Salmon;");
    ui->lineEdit_clientStatus->setStyleSheet("background-color: Salmon;");
    initTableView();

    ui->groupBox_qr->setVisible(false);
}

void MainWindow::showQrCode(QString msg){
    ui->groupBox_qr->setVisible(true);
    QPixmap map(400,400);
    QPainter painter(&map);
    //paintQR(painter,QSize(400,400),"Hello World", QColor("white"));
    paintQR(painter,QSize(400,400), msg, QColor("white"));

    ui->label_qr->setPixmap(map);
}

void MainWindow::paintQR(QPainter &painter, const QSize sz, const QString &data, QColor fg){
    char *str=data.toUtf8().data();
    // NOTE: At this point you will use the API to get the encoding and format you want, instead of my hardcoded stuff:
    qrcodegen::QrCode qr = qrcodegen::QrCode::encodeText(str, qrcodegen::QrCode::Ecc::HIGH);
    const int s=qr.size>0?qr.size:1;
    const double w=sz.width();
    const double h=sz.height();
    const double aspect=w/h;
    const double size=((aspect>1.0)?h:w);
    const double scale=size/(s+2);
    // NOTE: For performance reasons my implementation only draws the foreground parts in supplied color.
    // It expects background to be prepared already (in white or whatever is preferred).
    painter.setPen(Qt::NoPen);
    painter.setBrush(fg);
    for(int y=0; y<s; y++) {
        for(int x=0; x<s; x++) {
            const int color=qr.getModule(x, y);  // 0 for white, 1 for black
            if(0x0==color) { //TODO war != ist damit invertiert, rand bleibt aber schwarz
                const double rx1=(x+1)*scale, ry1=(y+1)*scale;
                QRectF r(rx1, ry1, scale, scale);
                painter.drawRects(&r,1);
            }
        }
    }


    //haessliche loesung aber scheint so zu funktionieren
    for(int x = 0; x < s+2; x++){//oben
        const double rx1=(x)*scale, ry1=(0)*scale;
        QRectF r(rx1, ry1, scale, scale);
        painter.drawRects(&r,1);
    }
    for(int x = 0; x < s+2; x++){//unten
        const double rx1=(x)*scale, ry1=(s+1)*scale;
        QRectF r(rx1, ry1, scale, scale);
        painter.drawRects(&r,1);
    }
    for(int x = 0; x < s; x++){//links
        const double rx1=(0)*scale, ry1=(x+1)*scale;
        QRectF r(rx1, ry1, scale, scale);
        painter.drawRects(&r,1);
    }
    for(int x = 0; x < s; x++){//rechts
        const double rx1=(s+1)*scale, ry1=(x+1)*scale;
        QRectF r(rx1, ry1, scale, scale);
        painter.drawRects(&r,1);
    }
}

//#############################################################################
//tcp zeug teil blub
//#############################################################################

void MainWindow::updateStatus(QString string){
    statusDialog->updateData(string);
}

void MainWindow::setCurrentWaypoint(int index){
    for(int i = 0; i < tModel->rowCount(); i++){
        QStandardItem *neu;
        if(i == index){
            neu = new QStandardItem(QString("set"));
            tModel->setItem(i,5,neu);
        } else {
            //neu = new QStandardItem(QString("unset"));
        }
        //hier falls alle zeilen gleichzeitig geaendert
        //tModel->setItem(i,5,neu);
    }
}

//#############################################################################
//tcp zeug teil blub
//#############################################################################

void MainWindow::on_msgFromServer(QString msg, bool fromClient){
    if(fromClient){
        parser->parseMessage(msg);
    } else {
        updateStatus(msg);
    }
}

//#############################################################################
//table view
//#############################################################################

void MainWindow::initTableView(){
    tableView = new QTableView;
    tModel = new QStandardItemModel(0,6, this);
    tModel->setHorizontalHeaderItem(0, new QStandardItem(QString("latitude")));
    tModel->setHorizontalHeaderItem(1, new QStandardItem(QString("longitude")));
    tModel->setHorizontalHeaderItem(2, new QStandardItem(QString("altitude")));
    tModel->setHorizontalHeaderItem(3, new QStandardItem(QString("action")));
    tModel->setHorizontalHeaderItem(4, new QStandardItem(QString("option")));
    tModel->setHorizontalHeaderItem(5, new QStandardItem(QString("yaw")));

    ui->tableView->setModel(tModel);
    connect(ui->tableView->model(), SIGNAL(dataChanged(const QModelIndex&, const QModelIndex&)),
        this, SLOT(onDataChanged(const QModelIndex&, const QModelIndex&)));
}

void MainWindow::addTableElement(QString lat, QString lon, QString alt, QString act, int opt, int yaw){
    int nextRow = tModel->rowCount();

    QStandardItem *col1 = new QStandardItem(lat);
    tModel->setItem(nextRow,0,col1);

    QStandardItem *col2 = new QStandardItem(lon);
    tModel->setItem(nextRow,1,col2);

    QStandardItem *col3 = new QStandardItem(alt);
    tModel->setItem(nextRow,2,col3);

    QStandardItem *col4 = new QStandardItem(act);
    tModel->setItem(nextRow,3,col4);

    QStandardItem *col5 = new QStandardItem(QString(QString::number(opt)));
    tModel->setItem(nextRow,4,col5);

    QStandardItem *col6 = new QStandardItem(QString(QString::number(yaw)));
    tModel->setItem(nextRow,5,col6);
}

void MainWindow::onDataChanged(const QModelIndex &topLeft, const QModelIndex &){
    int row = topLeft.row();
    int col = topLeft.column();
    qDebug() << "changed " + QString::number(row) + " " + QString::number(col);

    if(col != 3){
        coords->setElement(row, col, topLeft.data().toDouble());
    } else {
        QString temp = topLeft.data().toString();
        if(temp == "none"){
            coords->setElement(row, col, 0);
        } else if(temp == "stay"){
            coords->setElement(row, col, 1);
        } else if(temp == "yaw"){
            coords->setElement(row, col, 2);
        } else if(temp == "photo"){
            coords->setElement(row, col, 3);
        }
    }
}

void MainWindow::on_pushButton_resetTable_clicked(){
    tModel->removeRows(0, tModel->rowCount());
    coords->clear();
    ui->textBrowser_missionOpts->clear();
    ui->label_selectedFile->setText("none selected");
}

//#############################################################################
//load file with coordinates
//#############################################################################

QString MainWindow::openFileDialog(){

    //show window to select dir
    QString filename = QFileDialog::getOpenFileName(this, tr("Select file"),
                       "");

    if(!filename.isEmpty()){ // nur wenn nicht leer
        QStringList splitPath = filename.split("/");
        QString dispStr = ".../" + splitPath[splitPath.length()-2] + "/" + splitPath[splitPath.length()-1];
        ui->label_selectedFile->setText(dispStr);
    }

    return filename;
}

void MainWindow::on_button_selectFile_clicked(){
    QString file = openFileDialog();
    parseFile(file);
}

void MainWindow::parseFile(QString fileName){

    int tableIndex = -1;

    QJsonObject missionObject;
    QJsonArray wpArray;
    missionObject.insert("type", command(COMMAND::SEND_MISSION_DATA));

    QJsonObject wpObject;
    QJsonArray wpActions;
    int wpActionIndex = 0;

    int currentWp = -1;
    bool workingWps = false;

    if(fileName.isEmpty()){
        return;
    }

    QFileInfo fileInfo(fileName);
    QString suffix = fileInfo.suffix();
    QFile file(fileName);

    updateStatus("file: <" + fileName + ">");

    if(suffix != "txt"){
        updateStatus("illegal file type");
        return;
    }

    //prepare views
    updateMissionOptions("", true);
    initCoordsTableView();

    if (file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        QTextStream stream(&file);
        while (true){
            QString line = stream.readLine();
            if (line.isNull()){
                break;
            } else {
                QStringList temp = line.split(" ");
                temp.removeAll("");
                qDebug() << temp;

                //leerzeilen ueberspringen
                if(temp.size() == 0){
                    continue;
                }

                //zeile beginnend mit # ueberspringen
                if(temp[0][0] == '#'){
                    continue;
                }

                //neuen waypoint gefunden
                if(temp[0] == "wp"){
                    workingWps = true;
                    //neuer wp --> alter fertig also ausgeben
                    if(currentWp != -1){
                        if(wpActionIndex != 0){ //nur wenn actions vorhanden
                            wpObject.insert("act", wpActions);
                            wpActions = QJsonArray(); //reset
                            wpActionIndex = 0; //reset
                        }
                        wpArray.insert(currentWp, wpObject);
                        wpObject =QJsonObject();
                    }

                    currentWp++;
                    QJsonArray wpCoordsArray = {temp[1].toDouble(),
                                                temp[2].toDouble(),
                                                temp[3].toDouble()};
                    wpObject.insert("crd", wpCoordsArray);
                    wpCoordsArray = QJsonArray(); //reset

                    addCoordstableElement(++tableIndex, 1, QString::number(currentWp));
                    addCoordstableElement(tableIndex, 2, temp[1]);
                    addCoordstableElement(tableIndex, 3, temp[2]);
                    addCoordstableElement(tableIndex, 4, temp[3]);
                    qDebug() << "working wp " << currentWp;
                    continue;
                }

                //suche nach optionen fuer mission
                if(!workingWps){

                    //spezialfall da parameter array ist
                    if(temp[0] == "pointOfInterest"){
                        QJsonArray poiCoords = {temp[1].toDouble(), temp[2].toDouble()};
                        missionObject.insert(temp[0], poiCoords);
                        updateMissionOptions(temp[0] + ": [" + temp[1] + ", " + temp[2] + "]");
                        continue;
                    }

                    //andere optionen einfach anfuegen
                    bool intParsable;
                    temp[1].toInt(&intParsable);
                    if(intParsable){
                        missionObject.insert(temp[0], temp[1].toInt());
                    } else {
                        missionObject.insert(temp[0], temp[1]);
                    }
                    updateMissionOptions(temp[0] + ": " + temp[1]);
                    continue;

                //suche nach optionen fuer waypoint
                } else {
                    if(temp[0] == "action"){
                        QJsonArray action;
                        action.insert(0, temp[1]);
                        if(temp.size() > 2){
                            action.insert(1, temp[2].toInt());
                        }
                        wpActions.insert(wpActionIndex++, action);

                        if(temp.size() > 2){
                            addCoordstableElement(++tableIndex, 2, temp[1]);
                            addCoordstableElement(tableIndex, 3, temp[2]);
                        } else {
                            addCoordstableElement(++tableIndex, 2, temp[1]);
                        }
                        continue;
                    }

                    //andere optionen einfach anfuegen
                    bool intParsable;
                    temp[1].toInt(&intParsable);
                    if(intParsable){
                        wpObject.insert(temp[0], temp[1].toInt());
                    } else {
                        wpObject.insert(temp[0], temp[1]);
                    }

                    //check fuer tabelle
                    if(temp[0] == "hdg"){
                        addCoordstableElement(tableIndex, 5, temp[1]);
                    } else if(temp[0] == "gim"){
                        addCoordstableElement(tableIndex, 6, temp[1]);
                    } else if(temp[0] == "spd"){
                        addCoordstableElement(tableIndex, 7, temp[1]);
                    }

                    continue;
                }
            }
        }
        //letzter wp
        if(wpActionIndex != 0){ //nur wenn actions vorhanden
            wpObject.insert("act", wpActions);
        }
        wpActionIndex = 0;
        wpArray.insert(currentWp, wpObject);

        missionObject.insert("waypoints", wpArray);
        qDebug() << "post loop";

        QJsonDocument document(missionObject);
        QString jsonString(document.toJson(QJsonDocument::Compact));
        qDebug().noquote() << document.toJson(QJsonDocument::Indented);
        //qDebug().noquote() << document.toJson(QJsonDocument::Compact);

        missionDocument = document;
        missionDocumentInitialized = true;
        file.close();
        //coords->printCoordinates();
    } else {
        //file could not be opened
        updateStatus("could not open file");
        qDebug() << "could not open file";
    }
}

void MainWindow::sendMissionToPhone(){
    if(missionDocumentInitialized){
        sendMessage(missionDocument.toJson(QJsonDocument::Compact));
    }

    return;
}

void MainWindow::initCoordsTableView(){
    tableView = new QTableView;
    tModel = new QStandardItemModel(0,8, this);
    tModel->setHorizontalHeaderItem(0, new QStandardItem(QString("")));
    tModel->setHorizontalHeaderItem(1, new QStandardItem(QString("")));
    tModel->setHorizontalHeaderItem(2, new QStandardItem(QString("lat")));
    tModel->setHorizontalHeaderItem(3, new QStandardItem(QString("lon")));
    tModel->setHorizontalHeaderItem(4, new QStandardItem(QString("alt")));
    tModel->setHorizontalHeaderItem(5, new QStandardItem(QString("hdg")));
    tModel->setHorizontalHeaderItem(6, new QStandardItem(QString("gim")));
    tModel->setHorizontalHeaderItem(7, new QStandardItem(QString("spd")));

    ui->tableView_2->setModel(tModel);
    ui->tableView_2->setColumnWidth(0, 60);
    ui->tableView_2->setColumnWidth(1, 20);
    ui->tableView_2->setColumnWidth(2, 60);
    ui->tableView_2->setColumnWidth(3, 60);
    ui->tableView_2->setColumnWidth(4, 60);
    ui->tableView_2->setColumnWidth(5, 40);
    ui->tableView_2->setColumnWidth(6, 40);
    ui->tableView_2->setColumnWidth(7, 40);
    ui->tableView_2->verticalHeader()->hide();
}

void MainWindow::addCoordstableElement(int row, int col, QString data){
    QStandardItem *item = new QStandardItem(data);
    tModel->setItem(row, col, item);
}

void MainWindow::highlightCoordstableRow(int row){
    qDebug() << "highlight" << row << "clicked";
    for(int i = 0; i < tModel->rowCount(); i++){
        QStandardItem *item = new QStandardItem(QString(""));
        tModel->setItem(i, 0, item);
    }
    QStandardItem *item = new QStandardItem(QString("######"));
    tModel->setItem(row, 0, item);
}

void MainWindow::updateMissionOptions(QString option, bool clear){
    if(clear){
        ui->textBrowser_missionOpts->clear();
        return;
    }
    ui->textBrowser_missionOpts->append(option);
}



//#############################################################################
//save and load settings
//#############################################################################

void MainWindow::on_aboutToQuit(){
    saveSettings();
    qDebug() << "signal aboutToQuit recieved";
}

void MainWindow::saveSettings(){
    //vorsicht nichts zu ueberschreiben
    QSettings settings(settingsFile, QSettings::IniFormat);

    settings.beginGroup("Window");
        settings.setValue("positionMain", this->geometry());
        settings.setValue("positionStatus", statusDialog->geometry());
        settings.setValue("positionDrone", droneDialog->geometry());
    settings.endGroup();

    settings.beginGroup("Data");
        settings.setValue("rememberedConnections", rememberedConnections);
    settings.endGroup();
}

void MainWindow::loadSettings(){
    //check of ini file is found
    QFileInfo check_file(settingsFile);
    // check if file exists and if yes: Is it really a file and no directory?
    if(!check_file.exists() || !check_file.isFile()) {
        qDebug() << "no ini file found";
        return;
    } else {
        qDebug() << "loading settings";
    }

    QSettings settings(settingsFile, QSettings::IniFormat);

    settings.beginGroup("Window");
        QRect positionMain = settings.value("positionMain").toRect();
        this->setGeometry(positionMain);
        QRect positionStatus = settings.value("positionStatus").toRect();
        statusDialog->setGeometry(positionStatus);
        QRect positionDrone = settings.value("positionDrone").toRect();
        droneDialog->setGeometry(positionDrone);
    settings.endGroup();

    settings.beginGroup("Data");
        rememberedConnections = settings.value("rememberedConnections").toStringList();
        if(!rememberedConnections.isEmpty()){
            ui->comboBox_connect->addItems(rememberedConnections);
        }
    settings.endGroup();
}

//#############################################################################
//#############################################################################

void MainWindow::on_pushButton_clicked(){
    droneDialog->setDrone("mavic oder so");
    statusDialog->updateData("sajdlkasldkaldjklajsdlakj");
}

void MainWindow::on_pushButton_2_clicked(){
    static int val = 23;
    val *= val;
    val += 7;
    val %= 100;
    droneDialog->updateBatteryValue(val);
    updateStatus("battery at " + QString::number(val) + "%");
    droneDialog->flash();
}

void MainWindow::on_msgFromDroneDialog(QString msg, bool sendToPc){
    if(sendToPc){
        QJsonObject object;
        object.insert("type", msg);

        QJsonDocument document(object);
        QString jsonString(document.toJson(QJsonDocument::Compact));

        sendMessage(jsonString);
    } else {
        updateStatus("droneDialog: " + msg);
    }
}

//#############################################################################
//Server ui functions
//#############################################################################

void MainWindow::on_pushButton_saveIP_clicked(){
    QString ip = ui->lineEdit_ip->text();
    QString port = ui->lineEdit_port->text();

    if(ip.isEmpty() || port.isEmpty()){
        updateStatus("ip or port was empty");
        qDebug() << "no element to save";
        mIp = "";
        mPort = "";
        return;
    }

    mIp = ip;
    mPort = port;

    QString temp = ip + ":" + port;

    if(!rememberedConnections.contains(temp)){
        rememberedConnections.append(temp);
        ui->comboBox_connect->addItem(temp);
    }

}

void MainWindow::on_pushButton_deleteIp_clicked(){
    int selected = ui->comboBox_connect->currentIndex();

    ui->lineEdit_ip->clear();
    ui->lineEdit_port->clear();

    if(ui->comboBox_connect->currentIndex() == -1){
        qDebug() << "no element to delete";
        return;
    }

    rememberedConnections.removeAll(ui->comboBox_connect->itemText(selected));
    ui->comboBox_connect->removeItem(selected);
}

//update view
void MainWindow::on_comboBox_connect_currentIndexChanged(int index){
    if(ui->comboBox_connect->currentIndex() == -1){
        mIp = "";
        mPort = "";
        return;
    }

    QStringList splitStr = ui->comboBox_connect->itemText(index).split(":");
    mIp = splitStr[0];
    mPort = splitStr[1];

    ui->lineEdit_ip->setText(mIp);
    ui->lineEdit_port->setText(mPort);
}

void MainWindow::on_pushButton_startServer_clicked(){
    if(mIp == "" || mPort == ""){
        updateStatus("could not start server - ip or port was empty");
        return;
    }

    //updateStatus("trying to connect to " + mIp + ":" + mPort);

    server->startServer(mIp, mPort);
    showQrCode(mIp+ ":" + mPort);
    //ui->groupBox->setEnabled(false);
}

void MainWindow::on_pushButton_disconnectClient_clicked(){
    ui->groupBox->setEnabled(true);
    if(server){
        server->disconnect();
    }
}

void MainWindow::on_pushButton_stopServer_clicked(){
    if(server){
        server->stopServer();
    }
}

void MainWindow::on_pushButton_sendMsg_clicked(){
    on_lineEdit_msg_returnPressed();
}

void MainWindow::on_lineEdit_msg_returnPressed(){
    QString temp = ui->lineEdit_msg->text();
    if(server){
        server->sendMessage(temp);
    }
    ui->lineEdit_msg->clear();
}

void MainWindow::on_newServerStatus(QString msg, bool connected){
    ui->lineEdit_serverStatus->setText(msg);
    if(!connected){
        ui->lineEdit_serverStatus->setStyleSheet("background-color: Salmon;");
    } else {
        ui->lineEdit_serverStatus->setStyleSheet("background-color: MediumSeaGreen ;");
    }
}

void MainWindow::on_newClientStatus(QString msg, bool connected){
    ui->lineEdit_clientStatus->setText(msg);
    if(!connected){
        ui->lineEdit_clientStatus->setStyleSheet("background-color: Salmon;");
    } else {
        ui->lineEdit_clientStatus->setStyleSheet("background-color: MediumSeaGreen ;");
    }
}

//#############################################################################
//Mission ui functions
//#############################################################################

void MainWindow::on_pushButton_sendMission_clicked(){
    //coords->printCoordinates();
    sendMissionToPhone();
}

void MainWindow::on_pushButton_checkMission_clicked(){
    on_msgFromDroneDialog(command(COMMAND::CHECK_MISSION), true);
}

void MainWindow::on_pushButton_uploadMission_clicked(){
    on_msgFromDroneDialog(command(COMMAND::UPLOAD_MISSION), true);
}

void MainWindow::on_pushButton_startMission_clicked(){
    on_msgFromDroneDialog(command(COMMAND::START_MISSION), true);
}

void MainWindow::on_pushButton_pauseMission_clicked(){
    on_msgFromDroneDialog(command(COMMAND::PAUSE_MISSION), true);
}

void MainWindow::on_pushButton_stopMission_clicked(){
    on_msgFromDroneDialog(command(COMMAND::STOP_MISSION), true);
}

void MainWindow::on_pushButton_resumeMission_clicked(){
    on_msgFromDroneDialog(command(COMMAND::RESUME_MISSION), true);
}

//#############################################################################
//#############################################################################

void MainWindow::on_pushButton_3_clicked()
{
    qDebug() << "clicked";
    static int row = 0;
    highlightCoordstableRow(row++);
}
