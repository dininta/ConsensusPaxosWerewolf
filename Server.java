import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.*;
import java.net.InetAddress;

public class Server {
	
	protected static String dstAddress;
	protected static int dstPort;
	protected static InetAddress IPAddress;
    protected ServerSocket serverSocket = null;
    protected ArrayList<ServerThread> clients;

    public Server(){
        clients = new ArrayList<ServerThread>();
        dstAddress = "localhost";
        dstPort = 9876;
        try (ServerSocket serverSocket = new ServerSocket(dstPort)) { 
            System.out.println("Server is running");
            while (true) {
                ServerThread client = new ServerThread(serverSocket.accept());
                client.start();
                clients.add(client);
                System.out.println("client : " + client.getSocket());
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + dstPort);
            System.exit(-1);
        }
    }

    public void disconnect(){
        try {
            serverSocket.close();
        } catch (IOException e) {e.printStackTrace();}

    }
	public static void main(String args[]) throws Exception
	{
	   Server server = new Server();         
       
	}
}
