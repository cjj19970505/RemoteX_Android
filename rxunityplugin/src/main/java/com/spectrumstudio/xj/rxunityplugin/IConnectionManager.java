package com.spectrumstudio.xj.rxunityplugin;

public interface IConnectionManager {

    interface Callback{
        void onControllerConnectionEstablishResult(IConnection connection, IConnection.ConnectionEstablishState connectionEstablishState);
        void onControllerConnectionReceiveMessage(IConnection connection ,byte[] message);
    }
    void addCallback(Callback callback);
    void removeCallback(Callback callback);
    IConnection getControllerConnection();
    void setControllerConnection(IConnection connection);

}
