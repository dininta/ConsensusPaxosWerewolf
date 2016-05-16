import org.json.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.*;
import java.util.*;

public class UDPListenerThread extends Thread {
	protected DatagramSocket serverSocket;
	protected ArrayList[] messageQueue;

	public UDPListenerThread(DatagramSocket serverSocket, ArrayList[] messageQueue){
		this.serverSocket = serverSocket;
		this.messageQueue = messageQueue;
	}

	public void run(){
		byte[] receiveData = new byte[1024];
		while(true)
		{
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				serverSocket.receive(receivePacket);
			} catch (IOException e) {}

			// Get sender address and port
			try {
				String message = new String(receivePacket.getData(), 0, receivePacket.getLength());
				JSONObject obj = new JSONObject(message);
				obj.put("udp_address", receivePacket.getAddress());
				obj.put("udp_port", receivePacket.getPort());
				// System.out.println("Received from other player: " + obj.toString());
				messageQueue[0].add(obj.toString());
			} catch (JSONException e) {}
		}
    }

}