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
    protected String request; 
	protected String response;

    protected int player_id;
    protected String username;
    protected Socket socket;
    protected PrintWriter out; 
    protected boolean isJoin;

    protected BufferedReader in;
    private boolean running = true;
    private JSONObject jsonRequest; 
    private JSONObject jsonResponse; 

    public ServerThread(Socket socket) {
    	super("ServerThread");
        this.socket = socket;
        isJoin = false;
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
            e.printStackTrace();
            request = "IOException: " + e.toString();
        } catch (org.json.JSONException e) {}
    }

    //mengirim masukan bergantung dari method
    public void sendResponse(String method){
        //belum join
        if(!isJoin){
            if(method.equals("join")) {
                joinGame();
            }
            else {
                sendErrorResponse();
            }
        }
        else{
            if(method.equals("join")) {
                sendErrorResponse();
            }
            else if (method.equals("leave")) {
                leave();
            }
        }
    }

    //response for method join
    public void joinGame(){
        try{
            //mengecek apakah player sudah 6 
            jsonResponse = new JSONObject();
            if(Server.clients.size() < 6) {
                String name = jsonRequest.getString("username");
                //if username exists
                if(Server.usernames.contains(name)) {
                    jsonResponse.put("status", "fail");
                    jsonResponse.put("description", "user exists");
                }
                else { 
                    Server.clients.add(this);
                    player_id = Server.clients.size();
                    username = name;
                    Server.usernames.add(username);
                    isJoin = true;

                    //response berhasil
                    jsonResponse.put("status", "ok");
                    jsonResponse.put("player_id", player_id);
                }
            }
            //player sudah pas 6, tunggu dulu
            else {
                jsonResponse.put("status", "fail");
                jsonResponse.put("description", "please wait, game is currently running");
            }

            //kirim response
            System.out.println("Sending response: " + jsonResponse.toString());
            out.println(jsonResponse.toString());
        } catch (org.json.JSONException e) {}
    }

    //leave
    public void leave(){
        running = false;
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
            