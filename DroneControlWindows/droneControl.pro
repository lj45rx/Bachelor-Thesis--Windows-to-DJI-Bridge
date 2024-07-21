
QT       += core gui network

greaterThan(QT_MAJOR_VERSION, 4): QT += widgets

TARGET = droneControl
TEMPLATE = app


SOURCES += src/main.cpp\
        src/mainwindow.cpp \
    src/coordinatelist.cpp \
    src/statusdialog.cpp \
    src/dronedialog.cpp \
    src/tcpserver.cpp \
    src/parser.cpp \
    src/libs/libqr/qr/BitBuffer.cpp\
    src/libs/libqr/qr/QrCode.cpp\
    src/libs/libqr/qr/QRPainter.cpp\
    src/libs/libqr/qr/QrSegment.cpp\

HEADERS  += src/mainwindow.h \
    src/coordinatelist.h \
    src/statusdialog.h \
    src/dronedialog.h \
    src/tcpserver.h \
    src/parser.h \
    src/commands.h \
    src/actions.h \
    src/libs/libqr/qr/BitBuffer.hpp\
    src/libs/libqr/qr/QrCode.hpp\
    src/libs/libqr/qr/QRPainter.hpp\
    src/libs/libqr/qr/QrSegment.hpp\

FORMS    += ui/mainwindow.ui \
    ui/statusdialog.ui \
    ui/dronedialog.ui
