package Peer.Events;

import Peer.SocketIO;
import io.socket.emitter.Emitter;

public abstract class Event implements Emitter.Listener {

    protected SocketIO socketIO;

    protected Event(SocketIO socketIO) {
        this.socketIO = socketIO;
    }

}
