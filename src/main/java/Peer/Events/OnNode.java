package Peer.Events;

import Peer.SocketIO;

public class OnNode extends Event {

    public OnNode(SocketIO socketIO) {
        super(socketIO);
    }

    @Override
    public void call(Object... args) {
        System.out.println("Socket Connected");
        this.socketIO.setNodeState();
    }
}
