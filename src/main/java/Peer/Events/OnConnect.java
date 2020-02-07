package Peer.Events;

import Peer.SocketIO;

public class OnConnect extends Event {

    public OnConnect(SocketIO socketIO) {
        super(socketIO);
    }

    @Override
    public void call(Object... args) {
        System.out.println("Socket Connected");
        this.socketIO.connected();
    }
}
