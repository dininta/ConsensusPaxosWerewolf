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
import java.net.InetAddress;

public class ServerThread extends Thread {
    public static final int maxPlayer = 2;

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

    protected PrintWriter out; 

    protected BufferedReader in;
    private boolean running = true;
    private JSONObject jsonRequest; 
    private JSONObject jsonResponse; 

    public ServerThread(Socket socket) {
    	super("ServerThread");
        this.socket = socket;
        is_alive = 0;
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
            String method = jsonRequest.getString("method");
            
            sendResponse(method);

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
                sendReadyResponse();
            }
            else {
                sendFailResponse("command not found");
            }
        }
    }

    //response for method join
    public void joinGame(){
        try{
            //mengecek apakah player sudah 6 
            jsonResponse = new JSONObject();
            if(Server.clients.size() < maxPlayer) {
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

    //leave
    public void leave(){
        try {
            jsonResponse = new JSONObject();
            jsonResponse.put("status", "ok");
            //kirim response
            System.out.println("Sending response: " + jsonResponse.toString());
            out.println(jsonResponse.toString());
            running = false;
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
}
            