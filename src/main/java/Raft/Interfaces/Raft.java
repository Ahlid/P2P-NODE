package Raft.Interfaces;

import javafx.util.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 *
 */
public interface Raft extends Remote {
    /**
     * Invoked by candidates to gather votes (§5.2).
     * Arguments:
     * term candidate’s term
     * candidateId candidate requesting vote
     * lastLogIndex index of candidate’s last log entry (§5.4)
     * lastLogTerm term of candidate’s last log entry (§5.4)
     * Results:
     * term currentTerm, for candidate to update itself
     * voteGranted true means candidate received vote
     * Receiver implementation:
     * 1. Reply false if term < currentTerm (§5.1)
     * 2. If votedFor is null or candidateId, and candidate’s log is at
     * least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
     * @param term candidate's term
     * @param leaderId candidate's id
     * @param prevLogIndex index of candidate’s last log entry (§5.4)
     * @param entries
     * @param leaderCommit
     * @return      * term currentTerm, for candidate to update itself
     *      * voteGranted true means candidate received vote
     *      * Receiver implementation:
     *      * 1. Reply false if term < currentTerm (§5.1)
     *      * 2. If votedFor is null or candidateId, and candidate’s log is at
     *      * least as up-to-date as receiver’s log, grant vote (§5.2, §5.4)
     * @throws RemoteException - RMI exception
     */
    Pair<Long, Boolean> appendEntriesRPC(long term, String leaderId, long prevLogIndex, ArrayList<String> entries, long leaderCommit) throws RemoteException;

    /**
     * Invoked by leader to replicate log entries (§5.3); also used as
     * heartbeat (§5.2).
     * Arguments:
     * term leader’s term
     * leaderId so follower can redirect clients
     * prevLogIndex index of log entry immediately preceding
     * new ones
     * prevLogTerm term of prevLogIndex entry
     * entries[] log entries to store (empty for heartbeat;
     * may send more than one for efficiency)
     * leaderCommit leader’s commitIndex
     * Results:
     * term currentTerm, for leader to update itself
     * success true if follower contained entry matching
     * prevLogIndex and prevLogTerm
     * Receiver implementation:
     * 1. Reply false if term < currentTerm (§5.1)
     * 2. Reply false if log doesn’t contain an entry at prevLogIndex
     * whose term matches prevLogTerm (§5.3)
     * 3. If an existing entry conflicts with a new one (same index
     * but different terms), delete the existing entry and all that
     * follow it (§5.3)
     * 4. Append any new entries not already in the log
     * 5. If leaderCommit > commitIndex, set commitIndex =
     * min(leaderCommit, index of last new entry)
     * @param term leader’s term
     * @param candidateId so follower can redirect clients
     * @param lastLogIndex index of log entry immediately preceding new ones
     * @param lastLogTerm term of prevLogIndex entry
     * @return 1. Reply false if term < currentTerm (§5.1)
     *      * 2. Reply false if log doesn’t contain an entry at prevLogIndex
     *      * whose term matches prevLogTerm (§5.3)
     *      * 3. If an existing entry conflicts with a new one (same index
     *      * but different terms), delete the existing entry and all that
     *      * follow it (§5.3)
     *      * 4. Append any new entries not already in the log
     *      * 5. If leaderCommit > commitIndex, set commitIndex =
     *      * min(leaderCommit, index of last new entry)
     * @throws RemoteException
     */
    Pair<Long, Boolean> requestVoteRPC(long term, String candidateId, long lastLogIndex, long lastLogTerm) throws RemoteException;
}
