package Peer.Events;

import Peer.SocketIO;

public class OnHello extends Event {

    public OnHello(SocketIO socketIO) {
        super(socketIO);
    }

    @Override
    public void call(Object... args) {
        this.socketIO.peerConnection();
    }
}
