package com.example.mst.mav2dvi;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

class TcpClient {
    public enum TcpConnectStatus{
        DISCONNECTED,
        CONNECTING,
        CONNECTING_SUCCESS,
        CONNECTING_FAILED,
        CONNECTED
    }

    public interface TcpClientCallback {
        void messageReceived(String message);
        void statusChanged(TcpConnectStatus status, String msg);
    }

    private static String SERVER_IP; //server IP address
    private static int SERVER_PORT;

    private TcpConnectStatus mTcpConnectStatus = TcpConnectStatus.DISCONNECTED;

    // message to send to the server
    private String mServerMessage;
    // sends message received notifications
    private TcpClientCallback mMessageListener = null;
    // while this is true, the server will continue running
    private boolean mRun = false;
    private boolean mStarting = false;
    // used to send messages
    private PrintWriter mBufferOut;
    // used to read messages from the server
    private BufferedReader mBufferIn;

    private Socket mSocket;

    private static Process process_ping;

    TcpClient(TcpClientCallback listener, String ip, int port) {
        mMessageListener = listener;
        SERVER_IP = ip;
        SERVER_PORT = port;
    }

    void sendMessage(final String message) {
        if (mBufferOut != null && !mBufferOut.checkError()) {
            Thread thread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        mBufferOut.println(message);
                        mBufferOut.flush();
                    } catch (Exception e) {
                        Log.e("tcp", "error on send: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }

    void stopClient() {

        mRun = false;

        if (mBufferOut != null) {
            mBufferOut.flush();
            mBufferOut.close();
        }

        mMessageListener = null;
        mBufferIn = null;
        mBufferOut = null;
        mServerMessage = null;
        try {
            if(process_ping != null){
                process_ping.destroy();
            }
            if(mSocket != null){
                mSocket.close();
            }
        } catch (Exception e){
            //do nothing
        }
    }

    private boolean sendPing(String ip){

        boolean returnValue = false;

        String cmd = "ping -c 5 -w 5 " + ip;

        try {
            process_ping = Runtime.getRuntime().exec(cmd);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process_ping.getInputStream()));

            String s;
            while ((s = stdInput.readLine()) != null) {
                Log.i("tcp", s);
            }
            process_ping.waitFor();
            if(process_ping.exitValue() == 0){
                returnValue = true;
            }
            Log.i("tcp", "ping return value: " + process_ping.exitValue());
            process_ping.destroy();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("tcp", e.toString());
        }

        return returnValue;
    }

    private void emitStatusChanged(TcpConnectStatus status, String message){
        mTcpConnectStatus = status;
        if(mMessageListener != null) {
            mMessageListener.statusChanged(status, message);
        }
    }


    boolean isConnected(){
        return (mStarting || mSocket.isConnected());
    }

    void run() {
        mStarting = true;
        mRun = true;

        emitStatusChanged(TcpConnectStatus.CONNECTING, "Sending Ping");

        //check if server is reachable
        if(!sendPing(SERVER_IP)){
            emitStatusChanged(TcpConnectStatus.CONNECTING_FAILED, "Ping failed - Check connection");
            Log.i("tcp", "ping failed");
            return;
        } else {
            emitStatusChanged(TcpConnectStatus.CONNECTING, "Ping Success");
            Log.i("tcp", "ping success");
        }

        try {
            //create sockets
            emitStatusChanged(TcpConnectStatus.CONNECTING, "creating socket");
            InetAddress serverAddr_send = InetAddress.getByName(SERVER_IP);
            mSocket = new Socket(serverAddr_send, SERVER_PORT);
            Log.i("tcp", "mSocket created");
            emitStatusChanged(TcpConnectStatus.CONNECTING, "socket created");

            try {
                //initialize buffers
                mBufferOut = new PrintWriter(mSocket.getOutputStream());
                mBufferIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                int charsRead;
                char[] buffer = new char[10240]; //choose your buffer size if you need other than 1024

                //check connection one last time
                long startTime = System.currentTimeMillis();
                long timeout = 10000;
                boolean success = false;
                while( System.currentTimeMillis()-startTime < timeout ){
                    if(mSocket.isConnected()){
                        success = true;
                        mStarting = false;
                        break;
                    }
                }
                if (success){
                    Log.i("tcpClient", "connected succesfully");
                    emitStatusChanged(TcpConnectStatus.CONNECTING_SUCCESS, "Socket created");
                    emitStatusChanged(TcpConnectStatus.CONNECTED, "");
                } else {
                    Log.i("tcpClient", "connection failed");
                    emitStatusChanged(TcpConnectStatus.CONNECTING_FAILED, "Socket not created");
                    emitStatusChanged(TcpConnectStatus.DISCONNECTED, "");
                    stopClient();
                }

                while (mRun) {
                    charsRead = mBufferIn.read(buffer);
                    mServerMessage = new String(buffer).substring(0, charsRead);
                    if (mMessageListener != null) {
                        mMessageListener.messageReceived(mServerMessage);
                    }
                    mServerMessage = null;
                }
                //exception while connection is running
            } catch (Exception e) {
                Log.i("tcp", "exception to string: " + e.toString());
                emitStatusChanged(TcpConnectStatus.DISCONNECTED, e.toString());

                Log.e("TCP", "S: Error", e);
            } finally {
                //the sockets must be closed. It is not possible to reconnect
                if(mTcpConnectStatus != TcpConnectStatus.DISCONNECTED){
                    emitStatusChanged(TcpConnectStatus.DISCONNECTED, "onFinally");
                }
                mSocket.close();
            }

            //exception on creation of connection
        } catch (Exception e) {
            Log.e("TCP", "C: Error Connect Exception");
            emitStatusChanged(TcpConnectStatus.DISCONNECTED, e.getMessage());
        }
    }
}