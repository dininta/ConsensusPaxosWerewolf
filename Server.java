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
import java.util.*;
import java.net.InetAddress;

public class Server {
	
	protected static String dstAddress;
	protected static int dstPort;
	protected static InetAddress IPAddress;
    protected BufferedReader in;
    protected PrintWriter out;

    protected ServerSocket serverSocket = null;
    protected ArrayList<ServerThread> clients;
    protected ArrayList<String> usernames;

    public Server(){
        clients = new ArrayList<ServerThread>();
        usernames = new ArrayList<String>();
        dstAddress = "localhost";
        dstPort = 9876; 
    }

    public void run(){
        int countClient = 1;

        try (ServerSocket serverSocket = new ServerSocket(dstPort)) { 
            System.out.println("Server is running");
            while (true) {
                try{
                    //buat socket
                    Socket socket = serverSocket.accept();
                    //baca json dari socket
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    JSONObject obj = new JSONObject(in.readLine());
                    String method = obj.getString("method");
                    JSONObject json = new JSONObject();

                    //player mau join
                    if(method.equals("join")) {
                        //player masih kurang dari 6
                        if(countClient <= 6) {
                            String username = obj.getString("username");
                            //if username exists
                            if(usernames.contains(username)) {
                                json.put("status", "fail");
                                json.put("description", "user exists");
                            }
                            else {
                                ServerThread client = new ServerThread(serverSocket.accept(), countClient, username);
                                client.start();
                                clients.add(client);
                                usernames.add(client.getUsername());
                            
                                //response berhasil
                                json.put("status", "ok");
                                json.put("player_id", countClient);

                                countClient++;
                            }
                        }
                        //player sudah pas 6, tunggu dulu
                        else {
                            json.put("status", "fail");
                            json.put("description", "please wait, game is currently running");
                        }
                    }
                    //method lain selain join
                    else{
                        json.put("status", "error");
                        json.put("description", "wrong request");
                    }  
                    out.println(json.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (org.json.JSONException e) {}
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port " + dstPort);
            System.exit(-1);
        }

    }

    public void disconnect(){
        try {
            out.close();
            serverSocket.close();
        } catch (IOException e) {e.printStackTrace();}

    }
	public static void main(String args[]) throws Exception
	{
	   Server server = new Server();
       server.run();         
       
	}
}
