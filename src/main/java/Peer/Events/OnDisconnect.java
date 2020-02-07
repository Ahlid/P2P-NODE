package Peer.Events;

import Peer.SocketIO;

public class OnDisconnect extends Event {

    public OnDisconnect(SocketIO socketIO) {
        super(socketIO);
    }

    @Override
    public void call(Object... args) {
        System.out.println("Socket disconnected");
    }
}
