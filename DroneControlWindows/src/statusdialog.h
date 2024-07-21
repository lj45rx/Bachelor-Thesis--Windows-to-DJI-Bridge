#ifndef STATUSDIALOG_H
#define STATUSDIALOG_H

#include <QDialog>
#include <QDebug>
#include <QCloseEvent>

namespace Ui {
class StatusDialog;
}

class StatusDialog : public QDialog
{
    Q_OBJECT

public:
    explicit StatusDialog(QWidget *parent = 0);
    void updateData(QString string);
    ~StatusDialog();

private slots:
    void on_pushButton_clear_clicked();

private:
    Ui::StatusDialog *ui;
    //QStringList data;
    void closeEvent(QCloseEvent *event);
};

#endif // STATUSDIALOG_H
