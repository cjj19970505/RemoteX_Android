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
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

        //模拟C#事件机制专用
        private ArrayList<Callback> callbacks;

        //接收数据，并将数据放入MessagePacksQueue里
        private  ReveiveMessageAsyncTask receiveMessageAsyncTask;

        //处理消息队列中的数据，比如延迟处理啥的
        private MessageBufferHandlerAsyncTask messageBufferHandlerAsyncTask;

        //消息缓存队列
        private Queue<MessagePack> messagePacksBufferQueue;

        //用来防止messagePacksBufferQueue出现多线程问题的锁
        private Lock messageBufferLock;

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
            messagePacksBufferQueue = new LinkedList<>();
            messageBufferLock = new ReentrantLock();
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
        private void invokeOnReceiveMessage(IConnection connection, byte[] message){
            for(Callback callback:callbacks){
                callback.onReceiveMessage(connection, message);
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
                boolean result = aBoolean;
                if(result){
                    mConnectionEstablishState = ConnectionEstablishState.Succeed;
                    sendControlCodeTimer = new Timer();
                    sendProbeControlCodeTimerTask = new SendProbeControlCodeTimerTask();
                    sendControlCodeTimer.schedule(sendProbeControlCodeTimerTask, 0, 500);
                    receiveMessageAsyncTask = new ReveiveMessageAsyncTask();
                    receiveMessageAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    messageBufferHandlerAsyncTask = new MessageBufferHandlerAsyncTask();
                    messageBufferHandlerAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                    Log.e(TAG, "POS1:"+e.getMessage());
                    return ConnectionEstablishState.failed;
                }
                bluetoothSocket = tmp;
                bluetoothAdapter.cancelDiscovery();
                try{
                    bluetoothSocket.connect();
                }catch (Exception connectionException){
                    Log.e(TAG, "POS2:"+connectionException.getMessage());
                    try{
                        bluetoothSocket.close();
                    }catch (IOException socketCloseException){
                        Log.e(TAG, "POS3:"+socketCloseException.getMessage());
                        return ConnectionEstablishState.failed;
                    }
                    return ConnectionEstablishState.failed;
                }
                try{
                    inputStream = bluetoothSocket.getInputStream();
                    outputStream = bluetoothSocket.getOutputStream();
                }catch (IOException streamException){
                    Log.e(TAG, "POS4:"+streamException.getMessage());
                    try {
                        bluetoothSocket.close();
                    }
                    catch (IOException socketCloseException){
                        Log.e(TAG, "POS5:"+socketCloseException.getMessage());
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
                    if(exception.getMessage().equals("Broken pipe") ){
                        receiveMessageAsyncTask.cancel(true);
                        messageBufferHandlerAsyncTask.cancel(true);
                    }
                    return false;
                }
            }
        }

        class ReveiveMessageAsyncTask extends AsyncTask<Void, MessagePack, Boolean>{
            private final String TAG = "ReveiveMessageAsyncTask";

            private boolean taskCancelled;

            public ReveiveMessageAsyncTask() {
                super();
                taskCancelled = false;
            }

            @Override
            protected void onPreExecute() {

            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                while(true){
                    if(isCancelled()){
                        break;
                    }
                    try {
                        byte[] rawBytes = getBytes(inputStream);
                        MessagePack[] messagePacks = MessagePack.unpackMessages(rawBytes);
                        publishProgress(messagePacks);
                    }catch (Exception e){
                        if(isCancelled()){
                            break;
                        }
                        else
                        {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(MessagePack... values) {
                super.onProgressUpdate(values);
                for(MessagePack messagePack:values){
                    MessagePack copyedMessagePack = messagePack.getCopy();
                    messageBufferLock.lock();
                    if(copyedMessagePack != null){
                        messagePacksBufferQueue.offer(messagePack.getCopy());
                    }else{
                        Log.e(TAG, "（入队）没有解决的奇怪的null问题");
                    }
                    messageBufferLock.unlock();

                }
            }

            @Override
            protected void onCancelled(Boolean aBoolean) {
                super.onCancelled(aBoolean);
                try{
                    taskCancelled = true;
                    inputStream.close();
                }catch (IOException e){
                }
            }

            private byte[] getBytes(InputStream inputStream) throws IOException{
                final int LOAD_BYTE_COUNT = 990;
                ArrayList<byte[]> finalDataBytesArrayList = new ArrayList<>();
                byte[] currentDataBytes;
                boolean needNextLoad = true;
                int totalBytesCount = 0;
                while (needNextLoad){
                    currentDataBytes = new byte[LOAD_BYTE_COUNT];
                    int readByteCount = inputStream.read(currentDataBytes);
                    if(readByteCount < LOAD_BYTE_COUNT){
                        needNextLoad = false;
                    }else {
                        needNextLoad = true;
                    }
                    currentDataBytes = ByteUtil.getBytesRange(currentDataBytes, 0, readByteCount);
                    finalDataBytesArrayList.add(currentDataBytes);
                    totalBytesCount+=currentDataBytes.length;
                }

                byte[] finalBytes = new byte[totalBytesCount];
                int currPos = 0;
                for(int i = 0;i<finalDataBytesArrayList.size();i++){
                    for(int j = 0;j<finalDataBytesArrayList.get(i).length;j++){
                        finalBytes[currPos] = finalDataBytesArrayList.get(i)[j];
                        currPos++;
                    }
                }

                return finalBytes;
            }
        }

        class MessageBufferHandlerAsyncTask extends AsyncTask<Void, MessagePack, Void>{
            private final String TAG = "MessageBuffer";

            public MessageBufferHandlerAsyncTask() {
                super();
                Log.i(TAG,"CREATE");
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Log.i(TAG,"PRE");
            }

            @Override
            protected Void doInBackground(Void... voids) {
                while(true){
                    messageBufferLock.lock();
                    if(messagePacksBufferQueue.isEmpty()){
                        if(isCancelled()){
                            messageBufferLock.unlock();
                            break;
                        }
                        messageBufferLock.unlock();
                        continue;
                    }
                    MessagePack messagePack = messagePacksBufferQueue.poll();
                    if(messagePack !=null){
                        publishProgress(messagePack.getCopy());
                    }else {
                        Log.e(TAG, "（出队）没有解决的奇怪的null问题 ");
                    }
                    messageBufferLock.unlock();
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(MessagePack... values) {
                super.onProgressUpdate(values);
                for(MessagePack messagePack:values){
                    invokeOnReceiveMessage(BluetoothClientConnection.this, messagePack.getMessage());
                }
            }

            @Override
            protected void onCancelled(Void aVoid) {
                super.onCancelled(aVoid);
                messageBufferLock.lock();
                messagePacksBufferQueue.clear();
                messageBufferLock.unlock();
            }
        }
    }


    static class MessagePack{
        private byte[] message;
        private int controlCode;

        public MessagePack(int controlCode, byte[] message){
            this.controlCode = controlCode;
            this.message = message.clone();
        }

        public byte[] getMessage(){
            return message.clone();
        }
        public int getControlCode(){
            return controlCode;
        }
        public MessagePack getCopy(){
            byte[] copyedMessage = message.clone();
            return new MessagePack(controlCode, copyedMessage);
        }
        public static MessagePack[] unpackMessages(byte[] packedMessages){
            ArrayList<MessagePack> unpackedMessages = new ArrayList<>();
            int currIndex = 0;
            while (currIndex < packedMessages.length){
                byte[] msgControlCodeBytes = new byte[Integer.BYTES];
                for(int i = 0;i<Integer.BYTES;i++){
                    msgControlCodeBytes[i] = packedMessages[i+currIndex];
                }
                currIndex+=Integer.BYTES;
                int controlCode = ByteUtil.getInt(msgControlCodeBytes);

                byte[] msgLengthBytes = new byte[Integer.BYTES];
                for(int i = 0;i<Integer.BYTES;i++){
                    msgLengthBytes[i] = packedMessages[i+currIndex];
                }
                currIndex+=Integer.BYTES;
                int msgLength = ByteUtil.getInt(msgLengthBytes);

                byte[] message = new byte[msgLength];
                for(int i = 0;i<msgLength;i++){
                    message[i] = packedMessages[currIndex+i];
                }
                currIndex += msgLength;
                MessagePack msgPack = new MessagePack(controlCode, message);
                unpackedMessages.add(msgPack);
            }
            MessagePack[] unpackedMessagesArray = new MessagePack[unpackedMessages.size()];
            unpackedMessages.toArray(unpackedMessagesArray);
            return unpackedMessagesArray;
        }

    }
}
