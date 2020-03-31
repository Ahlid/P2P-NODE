package Raft.Interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import Raft.State.State;
import Raft.State.StateType;

public interface ServerRMI extends Remote, Raft, ServerMembership {
    State state = null;

    void setStateFromMarket(StateType state) throws RemoteException;
}
