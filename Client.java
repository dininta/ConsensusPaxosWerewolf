import org.json.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.*;
import java.net.InetAddress;

public class Client {
	// TCP
	protected String dstAddress;
	protected int dstPort;
	protected InetAddress IPAddress;
	protected Socket socket = null;
    protected PrintWriter out; //new
    protected BufferedReader in;

    // UDP
    protected int udpPort;
    protected String udpAddress;
    protected DatagramSocket datagramSocket;

    // variables
    private JSONObject jsonRequest; //new
    private JSONObject jsonResponse; //new
    protected String request; //new
	protected String response;
    protected String username;

	public Client(){
		try {
			// UDP Socket
			udpPort = 9999;
			InetAddress inetAddress = InetAddress.getLocalHost();
			udpAddress = inetAddress.getHostAddress();
			datagramSocket = new DatagramSocket();

			// TCP Socket
			dstAddress = "localhost";
			IPAddress = InetAddress.getByName(dstAddress);
			dstPort = 9876;
			socket = new Socket(dstAddress, dstPort);
            System.out.println("Server has connected");
	        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);      
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	public void joinGame(){
		// Get username
		BufferedReader inFromuser = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Username: ");
		try {
			username = inFromuser.readLine();
		} catch (IOException e) {}

		// Create json
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "join");
	        jsonRequest.put("username", username);
	        jsonRequest.put("udp_address", udpAddress);
	        jsonRequest.put("udp_port", udpPort);
        } catch (org.json.JSONException e) {}

        // Send json
	    System.out.println("Sending request: " + jsonRequest.toString());
	    out.println(jsonRequest.toString());
	    readResponse();
	}

	public void readResponse(){
        try{
            int c;
            response = in.readLine();
            System.out.println("Response: " + response);
            //sendResponse(); send another request(?)
        } catch (IOException e) {
            e.printStackTrace();
            response = "IOException: " + e.toString();
        }
    }

	public void disconnect(){
		// Tell server to disconnect
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "leave");
        } catch (org.json.JSONException e) {}
	    out.println(jsonRequest.toString());

		try {
	        socket.close();
	        out.close();
	        datagramSocket.close();
        } catch (IOException e) {e.printStackTrace();}
	}

	public static void main(String args[]) throws Exception
	{
		Client client = new Client();
		System.out.print("Command: ");
		Scanner sc = new Scanner(System.in);
		String input = sc.nextLine();
		while(!input.equals("quit")) {
			if(input.equals("join")) {
				client.joinGame();
			} else{
				System.out.println("Unknown command: " + input + "!!!");
			}
			System.out.print("Command: ");
			input = sc.nextLine();
		}
		client.disconnect();
	}
}
