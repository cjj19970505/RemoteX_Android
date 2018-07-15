package com.spectrumstudio.xj.rxunityplugin;

public interface IConnection {
    interface Callback{
        void onConnectionEstablishResult(IConnection connection, IConnection.ConnectionEstablishState state);
        void onReceiveMessage(IConnection connection, byte[] message);
    }

    enum ConnectionEstablishState {
        Created(0) , Succeeded(2), Failed(6), Connecting(1), Abort(4), Disconnected(5), Cancelled(3);

        private int stateCode;
        private ConnectionEstablishState(int stateCode){
            this.stateCode = stateCode;
        }

        public int getStateCode(){
            return stateCode;
        }
    }
    ConnectionEstablishState getConnectionEstablishState();

    void addCallback(Callback callback);
    void removeCallback(Callback callback);
    void send(byte[] message);
    void connect();
    void abortConnecting();
    void cancel();
}
