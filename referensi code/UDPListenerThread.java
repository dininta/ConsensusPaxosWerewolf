import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.*;

public class UDPListenerThread extends Thread {
	protected DatagramSocket serverSocket;
	protected String[] message;
	protected boolean[] valid;

	public UDPListenerThread(DatagramSocket serverSocket, String[] message, boolean[] valid){
		this.serverSocket = serverSocket;
		this.message = message;
		this.valid = valid;
	}

	public void run(){
		byte[] receiveData = new byte[1024];
		while(true)
		{
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				serverSocket.receive(receivePacket);
			} catch (IOException e) {}

			message[0] = new String(receivePacket.getData(), 0, receivePacket.getLength());
			valid[0] = true;
		}
    }

}