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

public class SenderTCP {
	
	protected static String dstAddress;
	protected static int dstPort;
	protected static InetAddress IPAddress;
	protected static String request;
	protected static String response;

	public static void main(String args[]) throws Exception
	{
		ServerSocket serverSocket = null;
		Socket socket = null;
		PrintWriter out;
		dstAddress = "localhost";
		IPAddress = InetAddress.getByName(dstAddress);
		dstPort = 9876;
		request = "Hello World!";
		//JSONObject json;
		try {
			serverSocket = new ServerSocket(dstPort);
			System.out.println("Connecting...");
            socket = serverSocket.accept();
            System.out.println("Server has connected");
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            System.out.println("Request: " + request);
            out.println(request);
            //json = new JSONObject();
            //json.put("method", "join");
            //json.put("username", "cantik");
            out.close();
            socket.close();
            serverSocket.close();
        //}catch(org.json.JSONException e){
        } catch (UnknownHostException e) {
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}
}
