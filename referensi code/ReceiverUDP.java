import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.*;
import java.lang.*;

public class ReceiverUDP
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
		int listenPort = 9876;
		DatagramSocket serverSocket = new DatagramSocket(listenPort);

		message[0] = "";
		valid[0] = false;
		UDPListenerThread udpListenerThread = new UDPListenerThread(serverSocket, message, valid);
		udpListenerThread.start();
		while(true){
			System.out.print("");
			if (valid[0]){
				printMessage(message[0]);
			}
		}
	}
}
