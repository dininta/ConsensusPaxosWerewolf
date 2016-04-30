import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;

/**
 * Kelas pembungkus DatagramSocket yang mensimulaiskan paket yang hilang
 * dalam pengiriman.
 */
public class ReliableSender
{
	private DatagramSocket datagramSocket;
	private Random random;

	public ReliableSender(DatagramSocket datagramSocket) throws SocketException {
		this.datagramSocket = datagramSocket;
		random = new Random();
	}

	public void send(DatagramPacket packet) throws IOException {
		double rand = random.nextDouble();
		/*if (rand < 0.85) {
			datagramSocket.send(packet);
		}*/
		datagramSocket.send(packet);
	}
}