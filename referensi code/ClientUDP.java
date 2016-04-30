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
	protected static final String[] message = new String[1];
	protected static final boolean[] valid = new boolean[1];
	/**
	 * Contoh kode program untuk node yang menerima paket. Idealnya dalam paxos
	 * balasan juga dikirim melalui UnreliableSender.
	 */



	public synchronized static void printMessage(String message){
		System.out.println("RECEIVED: " + message);
		valid[0] = false;
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

		message[0] = "";
		valid[0] = false;
		UDPListenerThread udpListenerThread = new UDPListenerThread(serverSocket, message, valid);
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
			if (valid[0]){
				printMessage(message[0]);
			}
		}
		datagramSocket.close();
	}
}
