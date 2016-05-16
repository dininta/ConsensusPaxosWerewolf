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
	//color text
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

	protected static String dstAddress;
	protected static int dstPort;
	protected static InetAddress IPAddress;
    protected BufferedReader in;
    protected PrintWriter out;

    protected ServerSocket serverSocket = null;
    public static ArrayList<ServerThread> clients;
    public static ArrayList<String> usernames;
    protected static boolean isRunning;
    protected static boolean isReady;
    protected static int kpuId;
    protected static ArrayList<Integer> kpuCounter;
    public static boolean changePhase;

    public Server(){
        changePhase = false;
        clients = new ArrayList<ServerThread>();
        usernames = new ArrayList<String>();
        dstAddress = "localhost";
        dstPort = 9876; 
        isReady = false;
        isRunning = false;
        kpuCounter = new ArrayList<Integer>();
        kpuId =0;
    }

    public void run(){
        try (ServerSocket serverSocket = new ServerSocket(dstPort)) { 
            System.out.println(ANSI_YELLOW + "Server is running" + ANSI_RESET);
            while (true) {
                ServerThread client = new ServerThread(serverSocket.accept());
                client.start();
                System.out.println(ANSI_YELLOW + "A client just connected, details: " + client.getSocket() + ANSI_RESET);
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + dstPort);
            System.exit(-1);
        }
    }

    public void disconnect(){
        try {
            out.close();
            serverSocket.close();
            System.out.println("Server stopped");
        } catch (IOException e) {e.printStackTrace();}

    }
	public static void main(String args[]) throws Exception
	{
	   Server server = new Server();
       server.run();          
	}
}
