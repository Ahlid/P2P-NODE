package Peer.Events;

import Peer.SocketIO;
import org.json.JSONObject;

public class OnSessionToken extends Event {

    public OnSessionToken(SocketIO socketIO) {
        super(socketIO);
    }

    @Override
    public void call(Object... args) {
        JSONObject obj = (JSONObject) args[0];
        this.socketIO.setToken(obj);
    }
}
