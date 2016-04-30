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
    protected DatagramSocket listenSocket;
    protected int kpuPort;
    protected InetAddress kpuAddress;

    // variables
    private JSONObject jsonRequest; //new
    private JSONObject jsonResponse; //new
    protected String request; //new
	protected String response;
    protected String username;
    protected boolean isAlive;
    protected int playerId;
    protected int counterProposal = 0;
    protected ArrayList<Player> players;
    protected final ArrayList[] messageQueue = new ArrayList[1];
	protected int werewolfVote[];

    /*** KONSTRUKTOR ***/
	public Client(int port){
		players = new ArrayList<Player>();
		isAlive = false;

		try {
			// UDP Socket
			udpPort = port;
			InetAddress inetAddress = InetAddress.getLocalHost();
			udpAddress = inetAddress.getHostAddress();
			datagramSocket = new DatagramSocket();
			listenSocket = new DatagramSocket(udpPort);
			messageQueue[0] = new ArrayList<String>();

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

	public void processCommand(String command){
		if(command.equals("join")) {
			 joinGame();
		} else if(command.equals("ready")) {
			 readyUp();
		} else if(command.equals("client_address")) {
			 getListClient();
		} else if(command.equals("leave")) {
			 leave();
		} else if (command.equals("send_proposal")) {
			prepareProposal();
		} else{
			//command tidak dikenali
			try{
				jsonRequest = new JSONObject();
	        	jsonRequest.put("method", command);
        	} catch (org.json.JSONException e) {}
	    	
	    	// Send json
	    	System.out.println("Sending request: " + jsonRequest.toString());
	    	out.println(jsonRequest.toString());
			
			// Receive response
	    	readResponse();
	    	try {
			    	String status = jsonResponse.getString("status");
			    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
			  
			} catch (JSONException e) {}
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
		    	isAlive = true;
		    }
		    else { // status == "fail" or status == "error"
		    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
		    	
		    }
		} catch (JSONException e) {}
		
	}

	public void readyUp(){
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
		    	
		    }
		    else { // status == "fail" or status == "error"
		    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
		    	
		    }
		} catch (JSONException e) {}
		
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
		readResponse();
		try {
		    String status = jsonResponse.getString("status");
		    if (status.equals("ok")) {
		    	System.out.println("status: " + jsonResponse.getString("status"));
		    	isAlive = false;
		    }
		    else { // status == "fail" or status == "error"
		    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
		    	
		    }
		} catch (JSONException e) {}
    }

	public void disconnect(){
		// Tell server to disconnect
		try {
	        socket.close();
	        out.close();
	        datagramSocket.close();
        } catch (IOException e) {e.printStackTrace();}
	}

	/*** METHOD FOR PROPOSER ***/

	public void prepareProposal() {
		// Create json proposal
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "prepare_proposal");
	        JSONArray proposal_id = new JSONArray();
	        counterProposal++;
	        proposal_id.put(counterProposal);
	        proposal_id.put(playerId);
	        jsonRequest.put("proposal_id", proposal_id);
	    } catch (org.json.JSONException e) {}

	    // Send json to every acceptor
		byte[] sendData = jsonRequest.toString().getBytes();
		for (int i=0; i<players.size(); i++) {
			if (players.get(i).playerId != playerId && players.get(i).isAlive == 1) {
				try {
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(players.get(i).address), players.get(i).port);
					datagramSocket.send(sendPacket);
				} catch (UnknownHostException e){
				} catch (IOException e){}
			}
		}
	}

	public void acceptProposal() {
		// Create json proposal
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "accept_proposal");
	        JSONArray proposal_id = new JSONArray();
	        proposal_id.put(counterProposal);
	        proposal_id.put(playerId);
	        jsonRequest.put("proposal_id", proposal_id);
	        jsonRequest.put("kpu_id", playerId);
	    } catch (org.json.JSONException e) {}

	    // Send json to every acceptor
		byte[] sendData = jsonRequest.toString().getBytes();
		for (int i=0; i<players.size(); i++) {
			if (players.get(i).playerId != playerId && players.get(i).isAlive == 1) {
				try {
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(players.get(i).address), players.get(i).port);
					datagramSocket.send(sendPacket);
				} catch (UnknownHostException e){
				} catch (IOException e){}
			}
		}
	}

	/*** METHOD FOR WEREWOLF ***/
	
	public void killWerewolfVote() {
		// Get player ID to be killed
		BufferedReader inFromuser = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Which player do you want to kill? Insert player id: ");
		int target = 0;
		try {
			target = Integer.parseInt(inFromuser.readLine());
		} catch (IOException e) {}

		// Create json
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "vote_werewolf");
	        jsonRequest.put("player_id", target);
	    } catch (org.json.JSONException e) {}

	    // Send json to KPU
		byte[] sendData = jsonRequest.toString().getBytes();
		try {
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, kpuAddress, kpuPort);
			datagramSocket.send(sendPacket);
		} catch (UnknownHostException e){
		} catch (IOException e){}
	}
	
	/*** METHOD FOR ACCEPTOR ***/
	
	public void killCivilianVote() {
		// Get player ID to be killed
		BufferedReader inFromuser = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Which werewolf do you want to kill? Insert player id: ");
		int target = 0;
		try {
			target = Integer.parseInt(inFromuser.readLine());
		} catch (IOException e) {}

		// Create json
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "vote_civilian");
	        jsonRequest.put("player_id", target);
	    } catch (org.json.JSONException e) {}

	    // Send json to KPU
		byte[] sendData = jsonRequest.toString().getBytes();
		try {
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, kpuAddress, kpuPort);
			datagramSocket.send(sendPacket);
		} catch (UnknownHostException e){
		} catch (IOException e){}
	}

	/*** METHOD FOR KPU ***/

	public void calculateWerewolfVote() {
		// Menghitung hasil voting dari werewolf

		// Initialize array
		werewolfVote = null;
		werewolfVote = new int[players.size() + 1];
		for (int i=0; i< werewolfVote.length; i++)
			werewolfVote[i] = 0;

		// Read message from listener
		while (messageQueue[0].size() > 0) {
			try {
				JSONObject vote = new JSONObject((String) messageQueue[0].remove(0));
				String method = vote.getString("method");
				if (method.equals("vote_werewolf")) {
					int targetId = vote.getInt("player_id");
					werewolfVote[targetId]++;
				}
				else {

				}
			} catch (JSONException e) {}
		}

		// Get maximum
		int max = werewolfVote[0];


	}
	

	/*** MAIN ***/
	public static void main(String args[]) throws Exception
	{
		Scanner sc = new Scanner(System.in);
		System.out.print("Your PORT: ");
		int port = Integer.parseInt(sc.nextLine());
		Client client = new Client(port);

		// start listener thread
		UDPListenerThread udpListenerThread = new UDPListenerThread(client.listenSocket, client.messageQueue); //mungkin di main
		udpListenerThread.start();

		System.out.print("Command: ");
		String input = sc.nextLine();
		while (true){ // quit loop only by using System.exit(0)
			if(input.equals("quit")) {
				if(client.isAlive) {
					client.processCommand("leave");
					client.isAlive = false;
				}
				client.disconnect();
				System.exit(0);

			}

			client.processCommand(input);
			System.out.print("Command: ");
			input = sc.nextLine();

			//aku comment dulu ya, kalau dibutuhin uncomment
			// gameplay:
			// for (int i = 1; i <= 1; i++){ // one dummy loop
			// 	// join game
			// 	System.out.print("Command utama: ");
			// 	String input = sc.nextLine();
			// 	while (!input.equals("join")){ // if method != "join" then repeat
			// 		if (input.equals("quit")){
			// 			client.disconnect();
			// 			System.exit(0);
			// 		}
			// 		System.out.println("Unknown command: " + input + "!!!");
			// 		System.out.print("Command: ");
			// 		input = sc.nextLine();
			// 	}
			// 	while (client.joinGame() != 0){ // if status != "ok" then repeat
			// 		// nothing
			// 	}

			// 	// ready up
			// 	while (client.readyUp() != 0){ // if status != "ok" then repeat
			// 		// nothing
			// 	}

			// 	// list client
			// 	System.out.print("Command: ");
			// 	input = sc.nextLine();
			// 	while (!input.equals("client_address")){ // if method != "join" then repeat
			// 		if (input.equals("leave")){
			// 			client.leave(); // leave belum bisa looping
			// 			break gameplay;
			// 		}
			// 		System.out.println("Unknown command: " + input + "!!!");
			// 		System.out.print("Command: ");
			// 		input = sc.nextLine();
			// 	}
			// 	client.getListClient();
			// }
		}
	}
}
