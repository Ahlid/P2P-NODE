package Peer.Events;

import Peer.SocketIO;
import Raft.StateType;
import io.socket.emitter.Emitter;

public class OnSetAsLeader extends Event {

    public OnSetAsLeader(SocketIO socketIO) {
        super(socketIO);
    }

    @Override
    public void call(Object... args) {

        System.out.println("new leader");
        this.socketIO.setLeader();
    }
}
