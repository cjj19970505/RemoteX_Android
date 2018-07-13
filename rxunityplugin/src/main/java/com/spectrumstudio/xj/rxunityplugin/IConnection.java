package com.spectrumstudio.xj.rxunityplugin;

public interface IConnection {
    interface Callback{
        void onConnectionEstablishResult(IConnection connection, IConnection.ConnectionEstablishState state);
        void onReceiveMessage(IConnection connection, byte[] message);
    }

    enum ConnectionEstablishState { NoEstablishment ,Succeed, failed, Connecting, Abort, Disconnect}
    ConnectionEstablishState getConnectionEstablishState();

    void addCallback(Callback callback);
    void removeCallback(Callback callback);
    void send(byte[] message);
    void connect();
    void abortConnecting();
}
