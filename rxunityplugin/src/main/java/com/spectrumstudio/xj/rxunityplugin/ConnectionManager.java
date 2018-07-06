package com.spectrumstudio.xj.rxunityplugin;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class ConnectionManager implements IConnectionManager, IConnection.Callback{
    private final String TAG = "ConnectionManager";

    private ArrayList<Callback> callbacks;
    private IConnection controllerConnection;


    public ConnectionManager(){
        callbacks = new ArrayList<>();
    }

    @Override
    public void addCallback(Callback callback) {
        if(callbacks == null){
            callbacks = new ArrayList<>();
        }
        callbacks.add(callback);
    }

    @Override
    public void removeCallback(Callback callback) {
        callbacks.remove(callback);
    }


    private void invokeOnControllerConnectionEstablishResult(IConnection connection, IConnection.ConnectionEstablishState connectionEstablishState){
        for(Callback callback : callbacks){
            callback.onControllerConnectionEstablishResult(connection, connectionEstablishState);
        }
    }

    private void invokeOnControllerConnectionReceiveMessage(IConnection connection ,byte[] message){
        for(Callback callback:callbacks){
            callback.onControllerConnectionReceiveMessage(connection, message);
        }
    }

    @Override
    public void onConnectionEstablishResult(IConnection connection, IConnection.ConnectionEstablishState state) {
        if(connection == controllerConnection){
            invokeOnControllerConnectionEstablishResult(connection, state);
        }
    }

    @Override
    public IConnection getControllerConnection() {
        if(controllerConnection != null && (controllerConnection.getConnectionEstablishState()== IConnection.ConnectionEstablishState.Abort || controllerConnection.getConnectionEstablishState()== IConnection.ConnectionEstablishState.Disconnect)){
            controllerConnection = null;
        }

        return controllerConnection;
    }
    @Override
    public void setControllerConnection(IConnection connection) {
        if(controllerConnection != null){
            controllerConnection.removeCallback(this);
        }
        controllerConnection = connection;
        if(controllerConnection != null){
            controllerConnection.addCallback(this);
        }
    }
}
