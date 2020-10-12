import Peer.Peer;
import Raft.Interfaces.ServerRMI;
import Raft.Server;

import java.net.URISyntaxException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class Main {

    static Server obj = null;
    static Peer peer;

    public static void main(String[] args) {
        try {

            int machine = 0;
            boolean online = false;
            String token = null;
            String machineToken = null;
            int port = 8200;

            ArrayList<String> machines = new ArrayList();
            machines.add("45692db4-c1ef-4be1-b151-f68810907a7b");
            machines.add("4781a80c-b298-4317-b299-e113362f9b67");
            machines.add("148e125d-5934-4f5e-b17a-fe1587a57427");
            machines.add("8f24fb90-24c1-4cd8-a31b-ddefb3d7937a");
            machines.add("4db49ed0-3a70-4b1a-8af1-7936f26729bc");
            machines.add("297a3248-499f-453f-9391-d5dded0ae790");
            machines.add("f84dcd9f-fd0f-43e9-9f4b-8042b4b7f0af");
            machines.add("a5ce847a-5405-4260-966c-3faa54456fbf");
            machines.add("e2815a90-d30d-4b39-a365-e92a2514eabe");
            machines.add("0c3e4b0a-f6e5-4434-a701-5bca6e7fd342");
            machines.add("63f53e57-8c58-4d36-bc50-b41d145de0cc");

            String machineId = machines.get(0);


            System.out.println(args.length);
            if (args.length == 4) {
                System.out.println(args[0]);
                System.out.println(args[1]);
                System.out.println(args[2]);
                System.out.println(args[3]);
                token = args[0];
                port = Integer.parseInt(args[1]);
                machineId = args[2];
                machineToken = args[3];
            }


            while (!online) {

                try {

                    obj = new Server(port);
                    ServerRMI stub = (ServerRMI) UnicastRemoteObject.exportObject(obj, port);


                    // Bind the remote object's stub in the registry
                    LocateRegistry.createRegistry(port);
                    Registry registry = LocateRegistry.getRegistry(port);
                    registry.bind("Raft.Server" + port, stub);


                    Thread.sleep(1000);


                    online = true;
                } catch (ExportException e) {
                    e.printStackTrace();
                    machine++;
                }
            }


            if(token != null) {
                peer = new Peer("http://192.168.1.72:8082", obj, "localhost", port, token, machineToken, machineId);
            }else{
                peer = new Peer("http://192.168.1.72:8082", "localhost", port, obj, "tese@ist.com", "tese@ist.com", machineId);
            }


            System.err.println("Raft.Server ready" + port);
            peer.start();

            obj.startServer();


        } catch (Exception e) {
            System.err.println("Raft.Server exception: " + e.toString());
            e.printStackTrace();
        }

        System.err.println("==================================================");
    }


    public static void stop() throws URISyntaxException {
        peer.stop();
        obj = null;
        peer = null;
        System.gc();
    }
}
