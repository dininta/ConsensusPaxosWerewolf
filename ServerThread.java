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
    protected String request; //new
	protected String response;
    protected int player_id;
    protected String username;
    protected Socket socket;
    protected PrintWriter out; //new
    protected BufferedReader in;
    private boolean running = true;
    private JSONObject jsonRequest; //new
    private JSONObject jsonResponse; //new

    public ServerThread(Socket socket, int id, String username) {
    	super("ServerThread");
        this.socket = socket;
        player_id = id;
        this.username = username;
        try{
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readRequest(){
        try{
            int c;
            request = in.readLine();
            System.out.println("Request: " + request);
            if (request.equals("{\"method\":\"leave\"}")){
                running = false;
            } else {
                sendResponse();
            }
        } catch (IOException e) {
            e.printStackTrace();
            request = "IOException: " + e.toString();
        }
    }

    public void sendResponse(){

        // Create json
        try{
            jsonResponse = new JSONObject();
            jsonResponse.put("status", "ok");
            jsonResponse.put("player_id", "dummy");
        } catch (org.json.JSONException e) {}

        // Send json
        System.out.println("Sending response: " + jsonResponse.toString());
        out.println(jsonResponse.toString());
    }

    public Socket getSocket(){
    	return socket;
    }

    public String getUsername(){
        return username;
    }
    
    public void run(){
        while (running)
    	   readRequest();
        out.close();
    }
}
            