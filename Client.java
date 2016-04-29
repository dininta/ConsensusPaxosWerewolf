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
    protected int playerId;

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

	    // Receive response
	    readResponse();
	    try {
		    String status = jsonResponse.getString("status");
		    if (status.equals("ok")) {
		    	playerId = jsonResponse.getInt("player_id");
		    	System.out.println("Your player ID is: " + playerId);
		    }
		    else {
		    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
		    }
		} catch (JSONException e) {}

		// Send method ready
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "ready");
        } catch (org.json.JSONException e) {}
	    out.println(jsonRequest.toString());
	}



/*	public void getListClient() {
		// Send request
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "client_address");
        } catch (org.json.JSONException e) {}
	    out.println(json.toString());

	    // Get server response
	    readResponse();
	    //String status = jsonResponse.getJSONObject("LabelData").getString("slogan");
	    
	}*/

	public void readResponse(){
        try{
            int c;
            response = in.readLine();
            jsonResponse = new JSONObject(response);
        } catch (IOException e) {
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } catch (org.json.JSONException e) {}
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
			} else {
				System.out.println("Unknown command: " + input + "!!!");
			}
			System.out.print("Command: ");
			input = sc.nextLine();
		}
		client.disconnect();
	}
}
