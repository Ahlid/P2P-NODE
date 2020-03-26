package Peer.Events;

import Peer.SocketIO;
import Raft.StateType;
import io.socket.emitter.Emitter;
import org.json.JSONObject;

public class OnSetAsLeader extends Event {

    public OnSetAsLeader(SocketIO socketIO) {
        super(socketIO);
    }

    @Override
    public void call(Object... args) {
        JSONObject obj = (JSONObject) args[0];
        System.out.println("new leader");
        this.socketIO.setLeader(obj);
    }
}
