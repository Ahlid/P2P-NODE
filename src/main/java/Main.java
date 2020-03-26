import Peer.Peer;
import Raft.Server;
import Raft.ServerRMI;
import Raft.StateType;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONObject;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        try {

            int port = 8200;
            boolean online = false;
            Server obj = null;

            while (!online) {

                try {
                    port++;

                    ArrayList<Integer> hosts = new ArrayList<>();
                    hosts.add(port);

                    obj = new Server(hosts, port);
                    ServerRMI stub = (ServerRMI) UnicastRemoteObject.exportObject(obj, port);


                    // Bind the remote object's stub in the registry
                    LocateRegistry.createRegistry(port);
                    Registry registry = LocateRegistry.getRegistry(port);
                    registry.bind("Raft.Server" + port, stub);


                    Thread.sleep(1000);

                    obj.startServer();
                    online = true;
                } catch (ExportException e) {
                    //  e.printStackTrace();
                }
            }

            System.err.println("Raft.Server ready");

            Peer peer = new Peer("http://localhost:8082", "localhost", port, obj);
            peer.start();


        } catch (Exception e) {
            System.err.println("Raft.Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
