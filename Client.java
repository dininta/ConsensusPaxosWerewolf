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

	class Player {
		public int playerId;
		public int isAlive;
		public String address;
		public int port;
		public String username;
		public String role;

		public Player(int playerId, int isAlive, String address, int port, String username) {
			this.playerId = playerId;
			this.isAlive = isAlive;
			this.address = address;
			this.port = port;
			this.username = username;
			this.role = "";
		}
		public Player(int playerId, int isAlive, String address, int port, String username, String role) {
			this.playerId = playerId;
			this.isAlive = isAlive;
			this.address = address;
			this.port = port;
			this.username = username;
			this.role = role;
		}
		public void print() {
			System.out.println(playerId + ";" + isAlive+ ";" + address+ ";" + port+ ";" + username+ ";" + role);
		}
	}

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
    protected ArrayList<Player> players;

	public Client(){
		players = new ArrayList<Player>();
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

	public int joinGame(){
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
		    	return 0;
		    }
		    else { // status == "fail" or status == "error"
		    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
		    	return 1;
		    }
		} catch (JSONException e) {}
		return 1;
	}

	public int readyUp(){
	// Send method ready
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "ready");
        } catch (org.json.JSONException e) {}
	    out.println(jsonRequest.toString());

	    // Receive response
	    readResponse();
	    try {
		    String status = jsonResponse.getString("status");
		    if (status.equals("ok")) {
		    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
		    	return 0;
		    }
		    else { // status == "fail" or status == "error"
		    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
		    	return 1;
		    }
		} catch (JSONException e) {}
		return 1;
	}

	public void getListClient() {
		// Send request
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "client_address");
        } catch (org.json.JSONException e) {}
	    out.println(jsonRequest.toString());

	    // Get server response
	    readResponse();
	    try {
		    String status = jsonResponse.getString("status");
		    if (status.equals("ok")) {
		    	JSONArray clients = jsonResponse.getJSONArray("clients");
		    	for (int i = 0; i < clients.length(); ++i) {
				    JSONObject client = clients.getJSONObject(i);
				    Player player;
				    if (client.has("role"))
				    	player = new Player (	client.getInt("player_id"),
				    							client.getInt("is_alive"),
				    							client.getString("address"),
				    							client.getInt("port"),
				    							client.getString("username"),
				    							client.getString("role") );
				    else
				    	player = new Player (	client.getInt("player_id"),
				    							client.getInt("is_alive"),
				    							client.getString("address"),
				    							client.getInt("port"),
				    							client.getString("username") );
				    players.add(player);
				}

				// Print players
				System.out.println("List of players received");
				for (int i=0; i<players.size(); i++) {
					players.get(i).print();
				}
		    }
		    else {
		    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
			}
		} catch (JSONException e) {}
	    
	}

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

    public void leave(){
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "leave");
	    } catch (org.json.JSONException e) {}
		out.println(jsonRequest.toString());
    }

	public void disconnect(){
		// Tell server to disconnect
		try {
	        socket.close();
	        out.close();
	        datagramSocket.close();
        } catch (IOException e) {e.printStackTrace();}
	}

	public static void main(String args[]) throws Exception
	{
		Client client = new Client();
		Scanner sc = new Scanner(System.in);
		while (true){ // quit loop only by using System.exit(0)
			// join game
			System.out.print("Command: ");
			String input = sc.nextLine();
			while (!input.equals("join")){ // if method != "join" then repeat
				if (input.equals("quit")){
					client.disconnect();
					System.exit(0);
				}
				System.out.println("Unknown command: " + input + "!!!");
				System.out.print("Command: ");
				input = sc.nextLine();
			}
			while (client.joinGame() != 0){ // if status != "ok" then repeat
				// nothing
			}

			// ready up
			while (client.readyUp() != 0){ // if status != "ok" then repeat
				// nothing
			}

			// list client
			System.out.print("Command: ");
			input = sc.nextLine();
			while (!input.equals("client_address")){ // if method != "join" then repeat
				if (input.equals("leave")){
					leave(); // leave belum bisa looping
				}
				System.out.println("Unknown command: " + input + "!!!");
				System.out.print("Command: ");
				input = sc.nextLine();
			}
		}
		client.disconnect();
	}
}
