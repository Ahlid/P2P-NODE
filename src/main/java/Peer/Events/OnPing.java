package Peer.Events;

import Peer.SocketIO;

public class OnPing extends Event {

    public OnPing(SocketIO socketIO) {
        super(socketIO);
    }

    @Override
    public void call(Object... args) {
        this.socketIO.ping();
    }
}
