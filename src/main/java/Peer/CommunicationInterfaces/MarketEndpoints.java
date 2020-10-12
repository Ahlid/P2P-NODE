package Peer.CommunicationInterfaces;

public enum MarketEndpoints {
    SET_LEADER, PEER_CONNECTION, LEADER_HZ, PONG, JOB_ASSIGNED, SEND_PEERS_STATE;


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
            case JOB_ASSIGNED:
                return "JOB_ASSIGNED";
            case SEND_PEERS_STATE:
                return "SEND_PEERS_STATE";
            default:
                return "";
        }
    }
}
