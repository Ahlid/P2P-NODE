package Raft;

import javafx.util.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

public interface ServerRMI extends Remote, Raft, ServerMembership {
    State state = null;

    void setStateFromMarket(StateType state) throws RemoteException;
}
