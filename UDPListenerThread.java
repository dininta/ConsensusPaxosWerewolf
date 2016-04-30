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

			messageQueue[0].add(new String(receivePacket.getData(), 0, receivePacket.getLength()));
		}
    }

}