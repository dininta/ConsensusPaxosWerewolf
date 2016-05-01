import org.json.JSONObject;
import org.json.JSONArray;
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

public class ServerThread extends Thread {
    public static final int maxPlayer = 3;

	protected static String dstAddress;
	protected static int dstPort;
	protected static InetAddress IPAddress;
    protected String request; 
	protected String response;

    protected int player_id;
    protected String username;
    protected String address;
    protected int port;
    protected Socket socket;
    protected int is_alive;
    protected boolean isReady;
    protected String role;
    protected String current_time = "day";  // day or night
    protected int counter_day = 1;
    protected int kpuId;
    protected String lastMethod;

    protected PrintWriter out; 

    protected BufferedReader in;
    private boolean running = true;
    private JSONObject jsonRequest; 
    private JSONObject jsonResponse; 

    public ServerThread(Socket socket) {
    	super("ServerThread");
        this.socket = socket;
        is_alive = 0;
        isReady = false;
        lastMethod = "";
        role = "";
        try{
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readRequest(){
        try{

            request = in.readLine();
            jsonRequest = new JSONObject(request);
             System.out.println("Request: " + request);
            
            if(jsonRequest.has("method")) {
                String method = jsonRequest.getString("method");
                sendResponse(method);
            } else if(jsonRequest.has("status")) {
                String status = jsonRequest.getString("status");
                processStatus(status);
            }
           

        } catch (IOException e) {
            sendErrorResponse();
        } catch (org.json.JSONException e) {
            sendErrorResponse();
        }
    }

    //mengirim masukan bergantung dari method
    public void sendResponse(String method){
        //belum join
        if(is_alive==0){
            if(method.equals("join")) {
                joinGame();
            }
            else if(method.equals("leave")) {
                leave();
            }
            else {
                sendFailResponse("please join first");
            }
        }
        else{
            if(method.equals("join")) {
                sendFailResponse("you've already joined");
            }
            else if (method.equals("leave")) {
                leave();
            }
            else if(method.equals("client_address")) {
                listClient();
            }
            else if(method.equals("ready")) {
                //game belum mulai
                if(!Server.isRunning)
                    sendReadyResponse();
                else
                    sendFailResponse("you've ready, game is currently running");
            }
            else if(method.equals("accepted_proposal")) {
                acceptProposal();
            }
            else {
                sendFailResponse("command not found");
            }
        }
    }

    //memproses response status dari client
    public void processStatus(String status) {
        try{
            if(status.equals("ok"))
                System.out.println("status:" + jsonRequest.getString("status"));
            else if(status.equals("fail")) {
                if(lastMethod.equals("start"))
                    startGame();
            }
        } catch (org.json.JSONException e) {}
    }

    public void disconnect(){
        try {
            out.close();
            socket.close();
        } catch (IOException e) {e.printStackTrace();}

    }

    //response for method join
    public void joinGame(){
        try{
            //mengecek apakah player sudah 6 
            jsonResponse = new JSONObject();
            if(!Server.isRunning) {
                String name = jsonRequest.getString("username");
                //if username exists
                if(Server.usernames.contains(name)) {
                    sendFailResponse("user exists");
                }
                else { 
                    Server.clients.add(this);
                    player_id = Server.clients.size();
                    username = name;
                    address = jsonRequest.getString("udp_address");
                    port = jsonRequest.getInt("udp_port");
                    Server.usernames.add(username);
                    is_alive = 1;

                    //response berhasil
                    jsonResponse.put("status", "ok");
                    jsonResponse.put("player_id", player_id);


                    //kirim response
                    System.out.println("Sending response: " + jsonResponse.toString());
                    out.println(jsonResponse.toString());


                }
            }
            //player sudah pas 6, tunggu dulu
            else {
                sendFailResponse("please wait, game is currently running");
            }

        } catch (org.json.JSONException e) {
            sendErrorResponse();
        }
    }

    //response for request ready
    public void sendReadyResponse(){
        //nunggu sampai semua jumlah player sudah mencapai max
        try{
            jsonResponse = new JSONObject();
            jsonResponse.put("status", "ok");
            jsonResponse.put("description", "waiting for other player to start");

            //kirim response
            System.out.println("Sending response: " + jsonResponse.toString());
            out.println(jsonResponse.toString());

            //set isReady dan cek apakah semua player sudah ready
            this.isReady = true;
            if(isAllReady()) {
                generateRole();
                for(ServerThread player: Server.clients) {
                    player.startGame();
                }
                Server.isRunning = true;
            }
                
        } catch (org.json.JSONException e) {
            sendErrorResponse();
        } 
    }

    //response for method list client
    public void listClient(){
        try{
            //mengembalikan list of clients
            jsonResponse = new JSONObject();
            JSONArray list = new JSONArray();

            for(int i=0; i<Server.clients.size(); i++){
                JSONObject obj = new JSONObject();
                obj.put("player_id", Server.clients.get(i).getPlayerId());
                obj.put("is_alive", Server.clients.get(i).getAlive());
                obj.put("address", Server.clients.get(i).getAddress());
                obj.put("port", Server.clients.get(i).getPort());
                obj.put("username", Server.clients.get(i).getUsername());
                list.put(obj);
            }

            //kirim response
            jsonResponse.put("status", "ok");
            jsonResponse.put("clients", list);
            jsonResponse.put("description", "list of clients retrieved");
            System.out.println("Sending response: " + jsonResponse.toString());
            out.println(jsonResponse.toString());
         } catch (org.json.JSONException e) {
            sendErrorResponse();
         }
    }

    public synchronized void startGame(){
       try {
            if(role.equals("werewolf")) {
                //ambil daftar teman
                JSONArray friends = new JSONArray();
                for(ServerThread player: Server.clients) {
                    if(player.getRole().equals("werewolf"))
                        friends.put(player.getUsername());
                }

                //kirim info start
                jsonResponse = new JSONObject();
                jsonResponse.put("method", "start");
                jsonResponse.put("time", "day");
                jsonResponse.put("role", role);
                jsonResponse.put("friend", friends);
                jsonResponse.put("description", "game is started");

            } else {
                jsonResponse = new JSONObject();
                jsonResponse.put("method", "start");
                jsonResponse.put("time", "day");
                jsonResponse.put("role", role);
                jsonResponse.put("description", "game is started");
            }
            

            //kirim response
            System.out.println("Sending response: " + jsonResponse.toString());
            out.println(jsonResponse.toString());

            //baca kembalian dari client
            lastMethod = "start";
            // request = in.readLine();
            // jsonRequest = new JSONObject(request);
            // processStatus("start");

            // out.println(jsonResponse.toString());
        } catch (org.json.JSONException e) {
            sendErrorResponse();
        } 

    }

    public void acceptProposal(){
        try{
            //mengambil kpu yang dipilih
            int id = jsonRequest.getInt("kpu_id");
            Server.kpuCounter.add(id);

            //mengembalikan list of clients
            jsonResponse = new JSONObject();

            jsonResponse.put("status", "ok");
            jsonResponse.put("description", "");
            System.out.println("Sending response: " + jsonResponse.toString());


            //jangan lupa diganti isAlive
            System.out.println("Choosing KPU, I am player number: " + player_id);
            if(Server.kpuCounter.size() == Server.clients.size()-2) {
                System.out.println("I am in, the kpuCounter.size() is = " + Server.kpuCounter.size());
                Server.kpuId = choosenKpu();

                jsonResponse = new JSONObject();

                jsonResponse.put("method", "kpu_selected");
                jsonResponse.put("kpu_id", Server.kpuId);
                System.out.println("Sending response: " + jsonResponse.toString());
                out.println(jsonResponse.toString());
            }
            
         } catch (org.json.JSONException e) {
            sendErrorResponse();
         }                
    }

    public void changePhase() {
        if (current_time.equals("night")) {
            current_time = "day";
            counter_day++;
        }
        else {
            current_time = "night";
        }

        try{
            jsonResponse = new JSONObject();
            jsonResponse.put("method", "change_phase");
            jsonResponse.put("time", current_time);
            jsonResponse.put("days", counter_day);
            jsonResponse.put("description", "");

            //kirim json
            System.out.println("Sending response: " + jsonResponse.toString());
            out.println(jsonResponse.toString());
        } catch (org.json.JSONException e) {
            sendErrorResponse();
        }
    }

    public void vote() {
        try{
            jsonResponse = new JSONObject();
            jsonResponse.put("method", "vote_now");
            jsonResponse.put("phase", current_time);

            //kirim json
            System.out.println("Sending response: " + jsonResponse.toString());
            out.println(jsonResponse.toString());
        } catch (org.json.JSONException e) {
            sendErrorResponse();
        }
    }

    //leave
    public void leave(){
        try {
            jsonResponse = new JSONObject();
            jsonResponse.put("status", "ok");
            //kirim response
            System.out.println("Sending response: " + jsonResponse.toString());
            out.println(jsonResponse.toString());
            is_alive = 0;
        } catch (org.json.JSONException e) {
            sendErrorResponse();
        }

    }

    //response untuk request yang tidak sesuai kebutuhan
    public void sendFailResponse(String desc){
        try {
            jsonResponse = new JSONObject();
            jsonResponse.put("status", "fail");
            jsonResponse.put("description", desc);

            //kirim response
            System.out.println("Sending response: " + jsonResponse.toString());
            out.println(jsonResponse.toString());
        } catch (org.json.JSONException e) {
            sendErrorResponse();
        }
    }

    //response for error request
    public void sendErrorResponse(){
        try {
            jsonResponse = new JSONObject();
            jsonResponse.put("status", "error");
            jsonResponse.put("description", "wrong request");

            //kirim response
            System.out.println("Sending response: " + jsonResponse.toString());
            out.println(jsonResponse.toString());
        } catch (org.json.JSONException e) {}
    }
    
    public void run(){
        while (running)
    	   readRequest();
        out.close();
    }

    //melihat apakah semua player sudah ready
    public boolean isAllReady(){
        if(Server.clients.size() < maxPlayer)
            return false;
        for(ServerThread player : Server.clients) {
            if(player.getIsReady()==false)
                return false;
        }
        return true;
    }


    //random werewolf dan civilian
    public void generateRole(){
        int size = Server.clients.size();
        ArrayList<Integer> randomList = new ArrayList<Integer>(size);
        //suffle random number
        for(int i = 0; i < size; i++)
        {
            randomList.add(i);
        }
        Collections.shuffle(randomList);

        //create role in each player
        for(int i=0; i< size; i++) {
            if(randomList.get(i)==0 || randomList.get(i)==1) {
                Server.clients.get(i).setRole("werewolf");
            }
            else
                Server.clients.get(i).setRole("civilian");
        }
    }

    ///memilih KPU berdasarkan vote 
    public int choosenKpu(){
        int candidate1 = 0;
        int candidate2 = 0;
        int count1 = 0;
        int count2 = 0;
        candidate1 = Server.kpuCounter.get(0);
        boolean found = false;
        int i = 1;
        while(!found && i < Server.kpuCounter.size()){
            if (Server.kpuCounter.get(i) != candidate1){
                candidate2 = Server.kpuCounter.get(i);
                found = true;
            }
            i++;
        }
        for (int j = 0; j < Server.kpuCounter.size()-1; j++){
            if (Server.kpuCounter.get(i) == candidate1){
                count1++;
            } else {
                count2++;
            }
        } 
        if (count1 > count2){
            return candidate1;
        } else if (count1 < count2){
            return candidate2;
        } else{
            Random random = new Random();
            double rand = random.nextDouble();
            if (rand <= 0.5) {
                return candidate1;
            } else {
                return candidate2;
            }
        }
    }

    //getter
    public int getPlayerId(){
        return player_id;
    }

    public Socket getSocket(){
        return socket;
    }

    public String getUsername(){
        return username;
    }

    public String getAddress(){
        return address;
    }

    public int getPort(){
        return port;
    }

    public int getAlive(){
        return is_alive;
    }

    public boolean getIsReady(){
        return isReady;
    }

    public String getRole(){
        return role;
    }

    //setter
    public void setRole(String role){
        this.role = role;
    }
}
            