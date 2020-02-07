package Peer.Events;

import Peer.SocketIO;
import org.json.JSONObject;

public class OnNewPeer extends Event {

    public OnNewPeer(SocketIO socketIO) {
        super(socketIO);
    }

    @Override
    public void call(Object... args) {
        JSONObject obj = (JSONObject) args[0];
        this.socketIO.newPeer(obj);
    }
}
