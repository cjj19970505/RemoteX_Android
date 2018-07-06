package com.spectrumstudio.xj.rxunityplugin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BluetoothConnectionManager {
    BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothClientConnection> bluetoothConnections;



    public BluetoothConnectionManager(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothConnections = new ArrayList<>();

    }

    public boolean isSupported(){
        return bluetoothAdapter != null;
    }

    public BluetoothClientConnection createRfcommClientConnection(BluetoothDevice device, UUID uuid){
        BluetoothClientConnection clientConnection = new BluetoothClientConnection(device, uuid);
        bluetoothConnections.add(clientConnection);
        return clientConnection;
    }

    public BluetoothDevice GetBluetoothDevice(byte[] macAddress){
        return bluetoothAdapter.getRemoteDevice(macAddress);
    }
    public BluetoothDevice GetBluetoothDevice(String macAddress){
        return bluetoothAdapter.getRemoteDevice(macAddress);
    }



    private class BluetoothClientConnection implements IConnection{
        private BluetoothDevice device;
        private UUID sdpUuid;
        private ConnectionEstablishState mConnectionEstablishState;
        private BluetoothSocket bluetoothSocket;
        private InputStream inputStream;
        private OutputStream outputStream;
        private boolean abortConnecting;

        private Timer sendControlCodeTimer;
        private SendProbeControlCodeTimerTask sendProbeControlCodeTimerTask;

        private ArrayList<Callback> callbacks;

        @Override
        public ConnectionEstablishState getConnectionEstablishState() {
            return mConnectionEstablishState;
        }

        public BluetoothClientConnection(BluetoothDevice device, UUID uuid){
            this.device = device;
            this.sdpUuid = uuid;
            abortConnecting = false;
            callbacks = new ArrayList<>();
            sendProbeControlCodeTimerTask = new SendProbeControlCodeTimerTask();

            this.mConnectionEstablishState = ConnectionEstablishState.NoEstablishment;

        }

        private byte[] connectionLayerPack(int controlCode, byte[] message){
            byte[] controlCodeBytes = ByteUtil.getBytes(controlCode);
            byte[] dataLengthBytes = ByteUtil.getBytes(message.length);
            byte[] packedMessage = ByteUtil.mergeBytes(controlCodeBytes, dataLengthBytes, message);
            return packedMessage;
        }

        @Override
        public void send(byte[] message) {
            send(2, message);
        }

        public void send(int controlCode, byte[] message){
            byte[] packedMessage = packMessage(controlCode, message);
            SendMessageTask sendMessageTask = new SendMessageTask(packedMessage);
            sendMessageTask.execute();
        }

        public byte[] packMessage(int controlCode, byte[] message){
            byte[] controlCodeBytes = ByteUtil.getBytes(controlCode);
            byte[] messageLengthBytes = ByteUtil.getBytes(message.length);
            byte[] packedMessage = ByteUtil.mergeBytes(controlCodeBytes, messageLengthBytes, message);
            return packedMessage;
        }

        @Override
        public void abortConnecting() {
            this.abortConnecting = true;
        }

        @Override
        public void addCallback(Callback callback) {
            callbacks.add(callback);
        }

        @Override
        public void removeCallback(Callback callback) {
            callbacks.remove(callback);
        }
        private void invokeOnConnectionEstablishResult(IConnection connection, IConnection.ConnectionEstablishState state){
            for(Callback callback:callbacks){
                callback.onConnectionEstablishResult(connection, state);
            }
        }
        public void connect(){
            this.mConnectionEstablishState = ConnectionEstablishState.Connecting;
            EstablishConnectionTask establishConnectionTask = new EstablishConnectionTask();

            establishConnectionTask.execute();
        }
        class EstablishConnectionTask extends AsyncTask<Void, Integer, Boolean> {
            private final String TAG = "EstablishConnectionTask";

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                bluetoothConnections.add(BluetoothClientConnection.this);
                invokeOnConnectionEstablishResult(BluetoothClientConnection.this, ConnectionEstablishState.Connecting);
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                ConnectionEstablishState state;
                do {
                    state = establishConnection();
                }while (state == ConnectionEstablishState.failed && !abortConnecting);

                if(abortConnecting){
                    return false;
                }
                else {
                    return true;
                }
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                boolean result = aBoolean.booleanValue();
                if(result){
                    mConnectionEstablishState = ConnectionEstablishState.Succeed;
                    sendControlCodeTimer = new Timer();
                    sendProbeControlCodeTimerTask = new SendProbeControlCodeTimerTask();
                    sendControlCodeTimer.schedule(sendProbeControlCodeTimerTask, 0, 500);
                    invokeOnConnectionEstablishResult(BluetoothClientConnection.this, ConnectionEstablishState.Succeed);

                }else{
                    mConnectionEstablishState = ConnectionEstablishState.Abort;
                    invokeOnConnectionEstablishResult(BluetoothClientConnection.this, ConnectionEstablishState.Abort);
                }

            }

            private ConnectionEstablishState establishConnection()
            {
                BluetoothSocket tmp;
                try{
                    tmp = device.createInsecureRfcommSocketToServiceRecord(sdpUuid);

                }catch (IOException e){
                    return ConnectionEstablishState.failed;
                }
                bluetoothSocket = tmp;
                bluetoothAdapter.cancelDiscovery();
                try{
                    bluetoothSocket.connect();
                }catch (Exception connectionException){
                    try{
                        bluetoothSocket.close();
                    }catch (IOException socketCloseException){
                        return ConnectionEstablishState.failed;
                    }
                    return ConnectionEstablishState.failed;
                }
                try{
                    inputStream = bluetoothSocket.getInputStream();
                    outputStream = bluetoothSocket.getOutputStream();
                }catch (IOException streamException){
                    try {
                        bluetoothSocket.close();
                    }
                    catch (IOException socketCloseException){
                        return ConnectionEstablishState.failed;
                    }
                }
                return ConnectionEstablishState.Succeed;
            }
        }

        class SendProbeControlCodeTimerTask extends TimerTask{
            //TimerTask Don't run on UI Thread by default
            @Override
            public void run() {
                byte[] packedMessage = packMessage(0, new byte[]{0});
                SendMessageTask sendMessageTask = new SendMessageTask(packedMessage);
                boolean result = sendMessageTask.doInBackground();
                if(!result){
                    cancel();
                    connect();
                }
            }
        }

        class SendMessageTask extends AsyncTask<Void, Integer, Boolean> {
            byte[] mMessage;
            public SendMessageTask(String message) {
                this.mMessage = message.getBytes();
            }
            public SendMessageTask(byte[] data) {
                this.mMessage = data.clone();
            }

            @Override
            protected void onPreExecute() {
            }
            @Override
            protected Boolean doInBackground(Void... voids) {
                byte[] buffer = mMessage.clone();
                try{
                    outputStream.write(buffer);
                    return true;
                }catch (IOException exception){
                    Log.i("SendProbeControlCode", exception.getMessage());
                    return false;
                }
            }
        }

        //class SendAsyncTask extends
    }
}
