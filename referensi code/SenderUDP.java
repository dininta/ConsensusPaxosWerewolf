import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SenderUDP {
	/**
	 * Contoh kode program untuk node yang mengirimkan paket. Paket dikirim
	 * menggunakan UnreliableSender untuk mensimulasikan paket yang hilang.
	 */
	public static void main(String args[]) throws Exception
	{
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

		String targetAddress = "192.168.43.236";
		InetAddress IPAddress = InetAddress.getByName(targetAddress);
		int targetPort = 9876;

		DatagramSocket datagramSocket = new DatagramSocket();
		UnreliableSender unreliableSender = new UnreliableSender(datagramSocket);

		while (true)
		{
			System.out.println("message to send:");
			String sentence = inFromUser.readLine();
			if (sentence.equals("quit"))
			{
				break;
			}

			byte[] sendData = sentence.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, targetPort);
			unreliableSender.send(sendPacket);
		}
		datagramSocket.close();
	}
}
