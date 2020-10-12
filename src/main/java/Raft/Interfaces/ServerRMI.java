package Raft.Interfaces;

import Raft.State.State;
import Raft.State.StateType;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerRMI extends Remote, Raft, ServerMembership {
    State state = null;

    void setStateFromMarket(StateType state) throws RemoteException;
}
