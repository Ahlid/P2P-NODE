package Raft;

import javafx.util.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface Raft extends Remote {
    Pair<Long, Boolean> appendEntriesRPC(long term, String leaderId, long prevLogIndex, ArrayList<String> entries, long leaderCommit) throws RemoteException;
    Pair<Long, Boolean> requestVoteRPC(long term, String candidateId, long lastLogIndex, long lastLogTerm) throws RemoteException;
}
