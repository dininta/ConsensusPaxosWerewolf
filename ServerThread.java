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
import java.net.InetAddress;

public class ServerThread extends Thread {
	protected static String dstAddress;
	protected static int dstPort;
	protected static InetAddress IPAddress;
	protected String response;
    protected int player_id;
    protected String username;
    protected Socket socket;
    protected BufferedReader in;
    private boolean running = true;

    public ServerThread(Socket socket, int id, String username) {
    	super("ServerThread");
        this.socket = socket;
        player_id = id;
        this.username = username;
        try{
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readResponse(){
        try{
            int c;
            response = in.readLine();
            System.out.println("Response: " + response);
            if (response.equals("{\"method\":\"disconnect\"}"))
                running = false;
        } catch (IOException e) {
            e.printStackTrace();
            response = "IOException: " + e.toString();
        }
    }

    public Socket getSocket(){
    	return socket;
    }

    public String getUsername(){
        return username;
    }
    
    public void run(){
        while (running)
    	   readResponse();
    }
}
            