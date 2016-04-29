import org.json.*;
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

public class Client {
	
	protected String dstAddress;
	protected int dstPort;
	protected InetAddress IPAddress;
	protected Socket socket = null;
	protected PrintWriter out;
	protected JSONObject json;
	protected String username;

	public Client(){
		try {
			dstAddress = "localhost";
			IPAddress = InetAddress.getByName(dstAddress);
			dstPort = 9876;
			socket = new Socket(dstAddress, dstPort);
            System.out.println("Server has connected");
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
			json = new JSONObject();
	        json.put("method", "join");
	        json.put("username", username);
        } catch (org.json.JSONException e) {}

        // Send json
	    System.out.println("Request: " + json.toString());
	    out.println(json.toString());
	}

	public void disconnect(){
		// Tell server to disconnect
		try{
			json = new JSONObject();
	        json.put("method", "disconnect");
        } catch (org.json.JSONException e) {}
	    out.println(json.toString());

		try {
	        socket.close();
	        out.close();
        } catch (IOException e) {e.printStackTrace();}
	}

	public static void main(String args[]) throws Exception
	{
		Client client = new Client();
		System.out.print("Command: ");
		Scanner sc = new Scanner(System.in);
		String input = sc.nextLine();
		while(!input.equals("quit")) {
			if(input.equals("join"))
				client.joinGame();
			System.out.print("Command: ");
			input = sc.nextLine();
		}
		client.disconnect();
	}
}
