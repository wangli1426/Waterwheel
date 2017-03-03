package indexingTopology.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by robert on 2/3/17.
 */
public class Client {

    Socket client;

    String serverHost;

    ObjectInputStream objectInputStream;

    ObjectOutputStream objectOutputStream;

    int port;

    public Client(String serverHost, int port) {
        this.serverHost = serverHost;
        this.port = port;
    }

    public void connect() throws IOException{
        client = new Socket(serverHost, port);
        objectOutputStream = new ObjectOutputStream((client.getOutputStream()));
        objectInputStream = new ObjectInputStream(client.getInputStream());
        System.out.println("Connected with " + serverHost);
    }

    public void close() throws IOException {
        objectInputStream.close();
        objectOutputStream.close();
        client.close();
    }

    public Response queryAPINumberOne() throws IOException, ClassNotFoundException {
        objectOutputStream.writeObject("string");
        return (Response)objectInputStream.readObject();
    }




    public static void main(String[] args) throws Exception {
        Client client = new Client("localhost", 10000);
        client.connect();
        Response response = client.queryAPINumberOne();
        System.out.println("Query one is submitted!");
        System.out.println(response);
    }

}
