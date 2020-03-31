package Peer.Events;

import Peer.SocketIO;

public class OnNode extends Event {

    public OnNode(SocketIO socketIO) {
        super(socketIO);
    }

    /**
     * Calls the setNodeState of the peer to make it change to Node state
     * @param args socket io args
     */
    @Override
    public void call(Object... args) {
        System.out.println("Socket Connected");
        this.socketIO.setNodeState();
    }
}
