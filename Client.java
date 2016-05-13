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
import java.util.Timer;

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
			System.out.println(playerId + ";" + isAlive+ ";" + address+ ";" + port+ ";" + username);
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
	protected CheckTimeout checkTimeout;
	protected boolean gameOver = false;

	// constants
	protected final long maxTime = 3000;

    /*** KONSTRUKTOR ***/
	public Client(String ip, int port){
		players = new ArrayList<Player>();
		isAlive = false;

		try {
			// UDP Socket
			udpPort = port;
			InetAddress inetAddress = InetAddress.getLocalHost();
			//udpAddress = inetAddress.getHostAddress();
			udpAddress = "192.168.43.101";
			datagramSocket = new DatagramSocket();
			listenSocket = new DatagramSocket(udpPort);
			messageQueue[0] = new ArrayList<String>();

			// TCP Socket
			dstAddress = ip;
			IPAddress = InetAddress.getByName(dstAddress);
			dstPort = 9876;
			socket = new Socket(dstAddress, dstPort);
            System.out.println("Server has connected");
	        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            // init time
            checkTimeout = new CheckTimeout(); // ini by default akan diisi durasi = 0 ya sis     
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
		for (int i=0; i<players.size(); i++) {
			if (players.get(i).isAlive == 1)
				count++;
		}	
		return count;
	}

	public int werewolfActive() {
		// Return the number of active werewolf
		
		int count = 0;
		for (int i=0; i<players.size(); i++) {
			if (players.get(i).role.equals("werewolf") && players.get(i).isAlive==1)
				count++;
		}
			
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
			    startPlaying();
			    
		    }
		    else { // status == "fail" or status == "error"
		    	
		    	System.out.println(jsonResponse.getString("status") + ": " + jsonResponse.getString("description"));
		    }
		} catch (JSONException e) {}
	}

	public boolean isProposer() {
		// true if i'm proposer
		int i = players.size()-1;
		int found = 0;
		boolean answer = false;
		while(i >= 0 && found < 2 && !answer){
			if(players.get(i).isAlive == 1){
				if(players.get(i).playerId == playerId){
					answer = true;
				}
				found++;
			}
			i--;
		}
		return answer;
	}

	public void startElection(){
		messageQueue[0].clear();
		//cuma buat print doang
		boolean cek =true;
		kpuId =0;

		while (kpuId==0){
			//System.out.println("KPUID: " + kpuId);
			if (isProposer()) {
				System.out.println("I'm proposer");
			    boolean success = prepareProposal();
			    if(success) {
			    	System.out.println("success");
			    	acceptProposal();

			   		readResponse();
			   		System.out.println("Response:" + jsonResponse.toString());
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
		boolean found = false;
		int i = 0;
		while (!found) {
			if (players.get(i).playerId == kpuId) {
				found = true;
				try {
					kpuAddress = InetAddress.getByName(players.get(i).address);
					kpuPort = players.get(i).port;
				} catch (UnknownHostException e) {}
			}
			i++;
		}
		messageQueue[0].clear();

		// send response to server
		try {
			jsonRequest = new JSONObject();
			jsonRequest.put("status", "ok");
			jsonRequest.put("description", "");
			
			out.println(jsonRequest.toString());
		} catch (JSONException e) {}
	}

	public void startPlaying() {
		boolean consensus = true;
		while (isAlive) {
			getListClient(true);
			startElection();
			// day
			readResponse();
			consensus=waitToVote(1);

			//kalau belum mencapai kesepakatan, vote ulang
			if(!consensus)
				consensus = waitToVote(2);
			// check if game over
			if (gameOver) {
				reset();
				return;
			}
			// change phase
			changePhase();

			System.out.println("isAlive: " + isAlive);
			if (!isAlive) {
				System.out.println("You're dead");
				break;
			}
			else {
				getListClient(true);

				// night
				readResponse();
				consensus=waitToVote(1);

				while(!consensus)
					consensus = waitToVote(1);
				// check if game over
				if (gameOver) {
					reset();
					return;
				}
				// change phase
				changePhase();

				getListClient(true);
				if (!isAlive) {
					System.out.println("You're dead");
					break;
				}
			}
		}
			
	}

	public void reset() {
		username = "";
    	isAlive = false;
    	playerId = 0;
    	counterProposal = 0;
    	players.clear();
		role = "";
		time = "";
		gameOver = false;
	}

	public void changePhase() {
		// // Get from server
		// System.out.println("Waiting for change phase");
		// readResponse();
		// System.out.println("SUMBER MASALAH : " + jsonResponse.toString());
		// if (playerId == kpuId)
		// 	readResponse();
		
		try {
			String method = jsonResponse.getString("method");
			if (method.equals("change_phase")) {
		    	time = jsonResponse.getString("time");

		    	if (jsonResponse.getInt("last_killed") == playerId){
		    		isAlive = false;
		    	}
		    	System.out.println("Change phase. Now is " + time);
			}

			// send back to server
			jsonRequest = new JSONObject();
			jsonRequest.put("status", "ok");
			out.println(jsonRequest.toString());
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

				    if ((client.getInt("player_id") == playerId) && (isAlive) && (client.getInt("is_alive") == 0)){ // my player data
				    	isAlive = false;
				    }
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

	public boolean waitToVote(int count) {
		
		try {
			String method = jsonResponse.getString("method");
			
			if (method.equals("vote_now")) {
				if (time.equals("day")) {
					killCivilianVote();
					if (kpuId == playerId)
						calculateCivilianVote(count);
				}
				else {	// time == "night"
					
					if (role.equals("civilian")) {
						System.out.println("It's night. Waiting...");
						if (kpuId == playerId){
							calculateWerewolfVote();
							readResponse(); //read ok
						}
					}
					else {	// werewolf
						killWerewolfVote();
						if (kpuId == playerId) {
							calculateWerewolfVote();
							readResponse(); //read ok
						}

						
					}
				}
			}
			else if (method.equals("game_over")) {
				gameOver = true;
				System.out.println("Game Over. Winner: " + jsonResponse.getString("winner"));
				return true;
			}
			
			readResponse();
			method = jsonResponse.getString("method");
			if(method.equals("vote_now"))
				return false;
			else if(method.equals("change_phase"))
				return true;
			else if (method.equals("game_over")) {
				gameOver = true;
				System.out.println("Game Over. Winner: " + jsonResponse.getString("winner"));
				return true;
			}
			else
				return false;
		} catch (JSONException e) {}
		return false;
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
	    //boolean timeout = false;
	    checkTimeout = new CheckTimeout(maxTime);
	    while (!checkTimeout.isTimeout() && ((counterAccepted < playersActive()/2) && (counterRejected < playersActive()/2))){
		    if (messageQueue[0].size() > 0) {
		    	String response = messageQueue[0].remove(0).toString();
		    	try {
		    		jsonResponse = new JSONObject (response);
		    		if(jsonResponse.has("status")) {
			    		if (jsonResponse.getString("status").equals("ok")){
			    			counterAccepted++;
			    		} else {
			    			counterRejected++;
			    		}
		    		}
		    		else if(jsonResponse.has("kpu")) {
		    			kpuId = jsonResponse.getInt("kpu");
		    			// response = messageQueue[0].remove(0).toString();
		    			// while(jsonResponse.has("kpu") && messageQueue[0].size() > 0)
		    			// 	response = messageQueue[0].remove(0).toString();
		    			

		    		}

		    	} catch (org.json.JSONException e) {}
	    	}
		}
		//untuk baca kpu_selected
		if(kpuId !=0) {
			readResponse();
		}
			

		if (checkTimeout.isTimeout()){
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

		
		//boolean timeout = false;
		checkTimeout = new CheckTimeout(maxTime);
	    while (!checkTimeout.isTimeout()){
		    if (messageQueue[0].size() > 0) {
		    	String response = messageQueue[0].remove(0).toString();
		    	System.out.println("Acceptor Response: " + response);
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

	public void waitProposal() {
		
		if (messageQueue[0].size() > 0){
			try{
				response = messageQueue[0].remove(0).toString();
				jsonResponse = new JSONObject(response);
				if (jsonResponse.getString("method").equals("prepare_proposal")){
					
					int a = jsonResponse.getJSONArray("proposal_id").getInt(0);
					int b = jsonResponse.getJSONArray("proposal_id").getInt(1);
					int c = previousProposal[0];
					int d = previousProposal[1];
					
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
					

					// send response to proposer
					int a = jsonResponse.getJSONArray("proposal_id").getInt(0);
					int b = jsonResponse.getJSONArray("proposal_id").getInt(1);
					int c = previousProposal[0];
					int d = previousProposal[1];
					
					if (previousProposal[0] == 0 && previousProposal[1] == 0){	
						jsonRequest = new JSONObject();
					    jsonRequest.put("status", "ok");
				        jsonRequest.put("description", "accepted");
				    } else if ((a > c) || (a == c && b > d)) {
				    	jsonRequest = new JSONObject();
					    jsonRequest.put("status", "ok");
				        jsonRequest.put("description", "accepted");
				        jsonRequest.put("previous_accepted", previousProposal);
				    } else if((a==c) &&(d==b)){
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

					if(jsonRequest.getString("status").equals("ok")) {
											// send to server
						try{
							jsonRequest = new JSONObject();
				        	jsonRequest.put("method", "accepted_proposal");
				        	jsonRequest.put("kpu_id", previousProposal[1]);
				        	jsonRequest.put("description", "Kpu is selected");
			        	} catch (org.json.JSONException e) {}
				    	
				    	
				    	out.println(jsonRequest.toString());

						// read from server
						readResponse();
						System.out.println("in wait proposal1 " + jsonResponse.toString());

						// ini untuk nerima ok, tapi bisa aja error? TODO HANDLE THIS
						readResponse();
						System.out.println("in wait proposal2 " + jsonResponse.toString());

						

					   	try {
					   		//karena bisa jadi dia ngirim accepted dua kali
					   		if(jsonResponse.has("method")){
					   			String method = jsonResponse.getString("method");
					    		if (method.equals("kpu_selected")){
					    			kpuId = jsonResponse.getInt("kpu_id");
					    		}
					   		}
					   		else {
					   			readResponse();
					   			System.out.println("in wait proposal3 " + jsonResponse.toString());
					   		}

					    		 
						} catch (JSONException e) {}

						//send dummy data to all
						try{
							jsonRequest = new JSONObject();
					        jsonRequest.put("kpu", kpuId);
					       
					    } catch (org.json.JSONException e) {}

					    // Send json to every one
					    System.out.println("dummy: " + jsonRequest);
						sendData = jsonRequest.toString().getBytes();
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


				} else {

				}
			} catch (org.json.JSONException e) {}
		}
	}

	/*** METHOD FOR KPU ***/

	public void calculateWerewolfVote() {
		// Menghitung hasil voting dari werewolf
		// True if werewolf sudah sepakat
	
		int vote1 = 0, vote2 = 0;

		// Read message from listener
		int countWerewolf = werewolfActive();
		while ((countWerewolf == 2 && (vote1 == 0 || vote2 == 0)) || (countWerewolf==1 && vote1==0)){
			System.out.print("");
			if (messageQueue[0].size() > 0) {
				try {	

					JSONObject vote = new JSONObject((String) messageQueue[0].remove(0));			
					System.out.println(vote.toString());
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
						try {
							sendFailResponse("", InetAddress.getByName(vote.getString("udp_address")), vote.getInt("udp_port"));
						} catch (UnknownHostException e){}
					}
				} catch (JSONException e) {}

			} 
		}
		
		if (countWerewolf == 2) {
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
				
			}
			else {
				try{
					jsonRequest = new JSONObject();
		        	jsonRequest.put("method", "vote_result_werewolf");
		        	jsonRequest.put("vote_status", -1);
		        	
		        	JSONArray array = new JSONArray();
		        	JSONArray array_temp1 = new JSONArray(2);
		        	JSONArray array_temp2 = new JSONArray(2);
		        	array_temp1.put(vote1);
		        	array_temp1.put(1);
		        	array_temp2.put(vote2);
		        	array_temp2.put(1);
		        	array.put(array_temp1);
		        	array.put(array_temp2);

		        	jsonRequest.put("vote_result", array);
	        	} catch (org.json.JSONException e) {}	    	
		    	System.out.println("Sending request: " + jsonRequest.toString());
		    	out.println(jsonRequest.toString());
			}
				
		}
		else { // countWerewolf == 1
			// Send to server
			try{
				jsonRequest = new JSONObject();
	        	jsonRequest.put("method", "vote_result_werewolf");
	        	jsonRequest.put("vote_status", 1);
	        	jsonRequest.put("player_killed", vote1);
	        	JSONArray array = new JSONArray();
	        	array.put(vote1);
	        	array.put(1);
	        	jsonRequest.put("vote_result", array);
        	} catch (org.json.JSONException e) {}	    	
	    	System.out.println("Sending request: " + jsonRequest.toString());
	    	out.println(jsonRequest.toString());
			
		}
		
	}

	public void calculateCivilianVote(int count) {
		// Menghitung voting dari seluruh civilian (voting werewolf mana yang mau dibunuh)

		// Initialize array
		
		int[] voteResult = new int[players.size()+1];
		for (int i=0; i<=players.size(); i++)
			voteResult[i] = 0;

		// Read from listener
		int countVote = 0;
		checkTimeout = new CheckTimeout(maxTime);
		while (countVote < playersActive()) {
			System.out.print("");
			if (messageQueue[0].size() > 0) {
				
				try {
					JSONObject vote = new JSONObject((String) messageQueue[0].remove(0));	
					
					String method = vote.getString("method");
					if (method.equals("vote_civilian")) {
						int targetId = vote.getInt("player_id");
						voteResult[targetId]++;
						countVote++;
						
						// Send response to sender
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
						try {
							sendFailResponse("", InetAddress.getByName(vote.getString("udp_address")), vote.getInt("udp_port"));
						} catch (UnknownHostException e){}
					}
				} catch (JSONException e) {}
			} 
			
		}

		// Hitung siapa yang dapat vote tertinggi dan mayoritas tercapai atau tidak
		int max = voteResult[0];
		int playerKilled = 0;
		for (int i=0; i<voteResult.length; i++) {
			if (voteResult[i] > max) {
				max = voteResult[i];
				playerKilled = i;
			}
			else if (voteResult[i] == max) {
				playerKilled = 0;
			}
		}

		// Tell result to server
		try{
			jsonRequest = new JSONObject();
        	jsonRequest.put("method", "vote_result_civilian");
        	if (playerKilled == 0 && count==1)
        		jsonRequest.put("vote_status", -1);
        	else {
        		jsonRequest.put("vote_status", 1);
        		jsonRequest.put("player_killed", playerKilled);
        	}
        	JSONArray array = new JSONArray();
        	for (int i=0; i<voteResult.length; i++) {
        		if (voteResult[i] > 0) {
	        		array.put(i);
    	    		array.put(voteResult[i]);
        		}
        	}
        	jsonRequest.put("vote_result", array);
    	} catch (org.json.JSONException e) {}	    	
    	System.out.println("Sending request: " + jsonRequest.toString());
    	out.println(jsonRequest.toString());

    	readResponse();
		System.out.println("In calculate civilian: " + jsonResponse.toString());
	}

	//response untuk request UDP
    public void sendFailResponse(String desc, InetAddress address, int port){
        try {
            jsonResponse = new JSONObject();
            jsonResponse.put("status", "fail");
            jsonResponse.put("description", desc);

            //kirim response
            System.out.println("Sending response: " + jsonResponse.toString());
            out.println(jsonResponse.toString());
            byte[] sendData = jsonResponse.toString().getBytes();
			try {
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
				datagramSocket.send(sendPacket);
			} catch (UnknownHostException e){
			} catch (IOException e){}
        } catch (org.json.JSONException e) { }
    }
	

	/*** MAIN ***/
	public static void main(String args[]) throws Exception
	{
		Scanner sc = new Scanner(System.in);
		System.out.print("Your PORT: ");
		int port = Integer.parseInt(sc.nextLine());
		System.out.print("Insert IP Server: ");
		String ip = sc.nextLine();
		Client client = new Client(ip, port);

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
