import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.*;
import java.lang.*;

public class ClientUDP
{
	protected static final ArrayList[] messageQueue = new ArrayList[1];

	/**
	 * Contoh kode program untuk node yang menerima paket. Idealnya dalam paxos
	 * balasan juga dikirim melalui UnreliableSender.
	 */


	public synchronized static void printMessage(String message){
		System.out.println("RECEIVED: " + message);
	}
	public static void main(String args[]) throws Exception
	{
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Target PORT: ");
		String port = inFromUser.readLine();

		String targetAddress = "localhost";
		InetAddress IPAddress = InetAddress.getByName(targetAddress);
		int targetPort = Integer.parseInt(port);

		DatagramSocket datagramSocket = new DatagramSocket();
		UnreliableSender unreliableSender = new UnreliableSender(datagramSocket);

		System.out.println("Listen PORT: ");
		port = inFromUser.readLine();
		int listenPort = Integer.parseInt(port);
		DatagramSocket serverSocket = new DatagramSocket(listenPort);

		messageQueue[0] = new ArrayList<String>();
		UDPListenerThread udpListenerThread = new UDPListenerThread(serverSocket, messageQueue);
		udpListenerThread.start();
		while(true){
			System.out.println("message to send:");
			String sentence = inFromUser.readLine();
			if (sentence.equals("quit"))
			{
				break;
			}
			byte[] sendData = sentence.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, targetPort);
			unreliableSender.send(sendPacket);
			System.out.print("");
			while (messageQueue[0].size() > 0){
				printMessage((String) messageQueue[0].remove(0));
			}
		}
		datagramSocket.close();
	}
}
