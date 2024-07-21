#include "statusdialog.h"
#include "ui_statusdialog.h"

StatusDialog::StatusDialog(QWidget *parent) :
    QDialog(parent),
    ui(new Ui::StatusDialog)
{
    ui->setupUi(this);
    QDialog::setWindowTitle("Status");
    setWindowFlags(Qt::Dialog | Qt::WindowTitleHint);
}

StatusDialog::~StatusDialog(){
    delete ui;
}

void StatusDialog::updateData(QString string){
    //data << string;
    //ui->textBrowser->setText(data.join("\n"));

    ui->textBrowser->append(string);
}

void StatusDialog::closeEvent(QCloseEvent *event){
    static int times = 0;
    times++;
    if(times < 5){
        event->ignore();
    }
}

void StatusDialog::on_pushButton_clear_clicked(){
    ui->textBrowser->clear();
}
