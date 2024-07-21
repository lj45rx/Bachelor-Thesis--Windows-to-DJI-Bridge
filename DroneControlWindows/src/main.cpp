#include "mainwindow.h"
#include <QApplication>

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);
    MainWindow w;
    w.show();

    QObject::connect(&a, &QCoreApplication::aboutToQuit, &w, &MainWindow::on_aboutToQuit);

    return a.exec();
}
