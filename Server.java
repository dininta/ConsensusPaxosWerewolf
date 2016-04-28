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

public class Server {
	
	protected static String dstAddress;
	protected static int dstPort;
	protected static InetAddress IPAddress;
	protected static String request;
	protected static String response;


	public static void main(String args[]) throws Exception
	{
		ServerSocket serverSocket = null;
        Socket clientSocket = null;
		BufferedReader in;
		dstAddress = "localhost";
		IPAddress = InetAddress.getByName(dstAddress);
		dstPort = 9876;

		try {
            serverSocket = new ServerSocket(dstPort);
            System.out.println("Connecting...");
            clientSocket = serverSocket.accept();
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            int c;
            response = "";
            while((c=in.read())!=-1){
                response+=(char)c;
            }
            System.out.println("Response: " + response);
            in.close();
            clientSocket.close();
            serverSocket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            response = "UnknownHostException: " + e.toString();
        } catch (IOException e) {
            e.printStackTrace();
            response = "IOException: " + e.toString();
        } finally {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
	}
}
