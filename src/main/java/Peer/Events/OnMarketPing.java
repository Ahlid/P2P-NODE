package Peer.Events;

import Peer.SocketIO;
import Raft.StateType;

public class OnMarketPing extends Event {

    public OnMarketPing(SocketIO socketIO) {
        super(socketIO);
    }

    @Override
    public void call(Object... args) {
        System.out.println("Market HZ");
        this.socketIO.leaderHZ();
    }
}
