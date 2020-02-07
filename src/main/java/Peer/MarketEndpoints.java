package Peer;

public enum MarketEndpoints {
    SET_LEADER, PEER_CONNECTION, LEADER_HZ, PONG;


    @Override
    public String toString() {
        switch (this) {
            case SET_LEADER:
                return "SET_LEADER";
            case LEADER_HZ:
                return "LEADER_HZ";
            case PEER_CONNECTION:
                return "PEER_CONNECTION";
            case PONG:
                return "P2P_PONG";
            default:
                return "";
        }
    }
}
