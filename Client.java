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
    protected int kpuId = 0;
    protected int[] previousProposal = {0, 0};
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
	protected String role;
	protected String time;

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
			 getListClient(true);
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

	public int playersActive() {
		// Return the number of active players
		int count = 0;
		for (int i=0; i<players.size(); i++)
			if (players.get(i).isAlive == 1)
				count++;
		return count;
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
		    	startGame();
		    }
		    else { // status == "fail" or status == "error"
		    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
		    }
		} catch (JSONException e) {}
		
	}

	public void startGame(){
		readResponse();
		try {
		    String method = jsonResponse.getString("method");
		    if (method.equals("start")) {
		    	time = jsonResponse.getString("time");
		    	role = jsonResponse.getString("role");
		    	System.out.println("time: " + jsonResponse.getString("time"));
		    	System.out.println("role: " + jsonResponse.getString("role"));
		    	System.out.println("description: " + jsonResponse.getString("description"));
		    	if (role.equals("werewolf")){
		    		System.out.println("friend: " + jsonResponse.getJSONArray("friend"));
		    	}
				jsonRequest = new JSONObject();
			    jsonRequest.put("status", "ok");

			    System.out.println("IN STARTGAME Request: " + jsonRequest.toString());
			    out.println(jsonRequest.toString());
			    getListClient(true);
			    startElection();
			    
		    }
		    else { // status == "fail" or status == "error"
		    	System.out.println("ERROR!!!");
		    	//System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
		    }
		} catch (JSONException e) {}
	}


	public void getListClient(boolean print) {
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
		    	players.clear();
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

				if(print){
					// Print players
					System.out.println("List of players received");
					for (int i=0; i<players.size(); i++) {
						players.get(i).print();
					}
				}

		    }
		    else {
		    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
			}
		} catch (JSONException e) {}
	    
	}

	public void waitProposal() {
		
		if (messageQueue[0].size() > 0){
			try{
				response = messageQueue[0].remove(0).toString();
				jsonResponse = new JSONObject(response);
				System.out.println("masuk nih" + response);
				if (jsonResponse.getString("method").equals("prepare_proposal")){
					System.out.println("IN WAITPROPOSAL, METHOD: PREPARE_PROPOSAL");
					int a = jsonResponse.getJSONArray("proposal_id").getInt(0);
					int b = jsonResponse.getJSONArray("proposal_id").getInt(1);
					int c = previousProposal[0];
					int d = previousProposal[1];
					System.out.println("ABCD: "+ a + " " + b + " " + c + " " + d);
					if (previousProposal[0] == 0 && previousProposal[1] == 0){	
						jsonRequest = new JSONObject();
					    jsonRequest.put("status", "ok");
				        jsonRequest.put("description", "accepted");
				        previousProposal[0] = a;
				        previousProposal[1] = b;
				    } else if ((a > c) || (a == c && b > d)) {
				    	jsonRequest = new JSONObject();
					    jsonRequest.put("status", "ok");
				        jsonRequest.put("description", "accepted");
				        jsonRequest.put("previous_accepted", previousProposal);
				        previousProposal[0] = a;
				        previousProposal[1] = b;
				    } else {
				    	jsonRequest = new JSONObject();
					    jsonRequest.put("status", "fail");
				        jsonRequest.put("description", "rejected");
				    }

				    // send response to proposer
				    InetAddress proposerAddress = null;
				    int proposerPort = 0;
				    findID:
				    for (int i=0; i<players.size(); i++) {
				    	if (players.get(i).playerId == jsonResponse.getJSONArray("proposal_id").getInt(1)) {
				    		try {
				    			proposerAddress = InetAddress.getByName(players.get(i).address);
				    			proposerPort = players.get(i).port;
				    		} catch (UnknownHostException e) {}
				    		break findID;
				    	}
				    }
					byte[] sendData = jsonRequest.toString().getBytes();
					try {
						System.out.println("Sending: " + jsonRequest.toString());
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, proposerAddress, proposerPort);
						datagramSocket.send(sendPacket);
					} catch (UnknownHostException e){
					} catch (IOException e){}


				} else if (jsonResponse.getString("method").equals("accept_proposal")){
					System.out.println("IN WAITPROPOSAL, METHOD: ACCEPT_PROPOSAL");

					// send response to proposer
					int a = jsonResponse.getJSONArray("proposal_id").getInt(0);
					int b = jsonResponse.getJSONArray("proposal_id").getInt(1);
					int c = previousProposal[0];
					int d = previousProposal[1];
					System.out.println("ABCD: "+ a + " " + b + " " + c + " " + d);
					if (previousProposal[0] == 0 && previousProposal[1] == 0){	
						jsonRequest = new JSONObject();
					    jsonRequest.put("status", "ok");
				        jsonRequest.put("description", "accepted");
				    } else if ((a > c) || (a == c && b > d)) {
				    	jsonRequest = new JSONObject();
					    jsonRequest.put("status", "ok");
				        jsonRequest.put("description", "accepted");
				        jsonRequest.put("previous_accepted", previousProposal);
				    } else {
				    	jsonRequest = new JSONObject();
					    jsonRequest.put("status", "fail");
				        jsonRequest.put("description", "rejected");
				    }
				    //get proposer ID
				    InetAddress proposerAddress = null;
				    int proposerPort = 0;
				    findID:
				    for (int i=0; i<players.size(); i++) {
				    	if (players.get(i).playerId == jsonResponse.getJSONArray("proposal_id").getInt(1)) {
				    		try {
				    			proposerAddress = InetAddress.getByName(players.get(i).address);
				    			proposerPort = players.get(i).port;
				    		} catch (UnknownHostException e) {}
				    		break findID;
				    	}
				    }
					byte[] sendData = jsonRequest.toString().getBytes();
					try {
						System.out.println("Sending: " + jsonRequest.toString());
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, proposerAddress, proposerPort);
						datagramSocket.send(sendPacket);
					} catch (UnknownHostException e){
					} catch (IOException e){}

					// send to server
					try{
						jsonRequest = new JSONObject();
			        	jsonRequest.put("method", "accepted_proposal");
			        	jsonRequest.put("kpu_id", previousProposal[1]);
			        	jsonRequest.put("description", "Kpu is selected");
		        	} catch (org.json.JSONException e) {}
			    	
			    	// Send json
			    	System.out.println("IN WAITPROPOSAL Request: " + jsonRequest.toString());
			    	//System.out.println("Sending request: " + jsonRequest.toString());
			    	out.println(jsonRequest.toString());

					// read from server
					readResponse();
					System.out.println("IN WAITPROPOSAL Response: " + jsonResponse.toString());
					// ini untuk nerima ok, tapi bisa aja error? TODO HANDLE THIS
					readResponse();
					System.out.println("IN WAITPROPOSAL Response: " + jsonResponse.toString());
				   	try {
				    	String method = jsonResponse.getString("method");
				    	if (method.equals("kpu_selected")){
				    		kpuId = jsonResponse.getInt("kpu_id");
				    	}
				    		 
					} catch (JSONException e) {}

				} else {

				}
			} catch (org.json.JSONException e) {}
		}
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
		try {
	        socket.close();
	        out.close();
	        datagramSocket.close();
        } catch (IOException e) {e.printStackTrace();}
	}

	/*** METHOD FOR PROPOSER ***/

	public boolean prepareProposal() {
		System.out.println("Prepare Proposal");
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
	    System.out.println("UDP Request: " + jsonRequest);
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

		int counterAccepted = 0;
	    int counterRejected = 0;
	    boolean timeout = false;
	    while (!timeout && ((counterAccepted < playersActive()/2) && (counterRejected < playersActive()/2))){
		    if (messageQueue[0].size() > 0) {
		    	String response = messageQueue[0].remove(0).toString();
		    	try {
		    		jsonResponse = new JSONObject (response);
		    		if (jsonResponse.getString("status").equals("ok")){
		    			counterAccepted++;
		    		} else {
		    			counterRejected++;
		    		}
		    	} catch (org.json.JSONException e) {}
	    	}
		}	
		if (timeout){
			return false;
		}
		else if (counterAccepted == counterRejected){
			return false;
		} 
		else if (counterAccepted >= playersActive()/2) {
			return true;
		}
		else if (counterRejected >= playersActive()/2) {
			return false;
		} else {
			return false;
		}
	}

	public void acceptProposal() {
		System.out.println("IN ACCEPTPROPOSAL");
		// Create json proposal
		try{
			jsonRequest = new JSONObject();
	        jsonRequest.put("method", "accept_proposal");
	        JSONArray proposal_id = new JSONArray();
	        proposal_id.put(counterProposal);
	        proposal_id.put(playerId);
	        jsonRequest.put("proposal_id", proposal_id);
	        jsonRequest.put("kpu_id", playerId);
	        System.out.println("PROPOSAL ID" + jsonRequest.toString());
	    } catch (org.json.JSONException e) {}

	    // Send json to every acceptor
	    System.out.println("UDP Request: " + jsonRequest);
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

		boolean timeout = false;
	    while (!timeout){
		    if (messageQueue[0].size() > 0) {
		    	String response = messageQueue[0].remove(0).toString();
		    	try {
		    		jsonResponse = new JSONObject (response);
		    	} catch (org.json.JSONException e) {}
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

	public void startElection(){
		//cuma buat print doang
		boolean cek =true;

		System.out.println("IN STARTELECTION");
		System.out.println("KPUID: " + kpuId);
		while (kpuId==0){
			//System.out.println("KPUID: " + kpuId);
			if (playerId >= players.size() - 1){//ntar jadi players.size() yhaa
				System.out.println("I'm proposer");
			    boolean success = prepareProposal();
			    if(success) {
			    	System.out.println("success");
			    	acceptProposal();
			    	// Wait KPU id from server
			   		readResponse();
			   		try {
			    		String method = jsonResponse.getString("method");
			    		if (method.equals("kpu_selected"))
			    			kpuId = jsonResponse.getInt("kpu_id"); 
					} catch (JSONException e) {}
			    } else{
			    	System.out.println("tidak success");
			    }
			}
			else{
				//cuma buat print 
				if(cek){
					System.out.println("I'm acceptor");
					cek = false;
				}	
			    waitProposal();
			}
		}
		System.out.println("KPU terpilih: " + kpuId);
	}
	

	
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

	public boolean calculateWerewolfVote() {
		// Menghitung hasil voting dari werewolf
		// True if werewolf sudah sepakat

		int vote1 = 0, vote2 = 0;

		// Read message from listener
		while (vote1 == 0 && vote2 == 0) {
			try {
				JSONObject vote = new JSONObject((String) messageQueue[0].remove(0));
				String method = vote.getString("method");
				if (method.equals("vote_werewolf")) {
					int targetId = vote.getInt("player_id");
					if (vote1 == 0)
						vote1 = targetId;
					else
						vote2 = targetId;

					// Send response to werewolf
					jsonRequest = new JSONObject();
			        jsonRequest.put("status", "ok");
			        jsonRequest.put("description", "");
					byte[] sendData = jsonRequest.toString().getBytes();
					try {
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(vote.getString("udp_address")), vote.getInt("udp_port"));
						datagramSocket.send(sendPacket);
					} catch (UnknownHostException e){
					} catch (IOException e){}
				}
				else {

				}
			} catch (JSONException e) {}
		}
		if (vote1 == vote2) {
			// Send to server
			try{
				jsonRequest = new JSONObject();
	        	jsonRequest.put("method", "vote_result_werewolf");
	        	jsonRequest.put("vote_status", 1);
	        	jsonRequest.put("player_killed", vote1);
	        	JSONArray array = new JSONArray();
	        	array.put(vote1);
	        	array.put(2);
	        	jsonRequest.put("vote_result", array);
        	} catch (org.json.JSONException e) {}	    	
	    	System.out.println("Sending request: " + jsonRequest.toString());
	    	out.println(jsonRequest.toString());
			return true;
		}
		else
			return false;
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

		}
	}
}
