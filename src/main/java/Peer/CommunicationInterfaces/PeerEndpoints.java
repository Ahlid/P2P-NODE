package Peer.CommunicationInterfaces;

public enum PeerEndpoints {
    HELLO, SET_LEADER, MARKET_PING, PING, SET_NODE, NEW_PEER, REQUEST_JOB_ASSIGN, SEND_SESSION_TOKEN;

    @Override
    public String toString() {
        switch (this) {
            case SET_LEADER:
                return "SET_LEADER";
            case HELLO:
                return "HELLO";
            case MARKET_PING:
                return "MARKET_PING";
            case PING:
                return "P2P_PING";
            case SET_NODE:
                return "SET_NODE";
            case NEW_PEER:
                return "NEW_PEER";
            case REQUEST_JOB_ASSIGN:
                return "REQUEST_JOB_ASSIGN";
            case SEND_SESSION_TOKEN:
                return "SEND_SESSION_TOKEN";
            default:
                return "";
        }
    }
}
