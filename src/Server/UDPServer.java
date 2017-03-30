package Server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;

import Common.BaseMessage;
import Common.ServerCommand;

public class UDPServer implements BaseServer, IUPDServer {

	DatagramSocket m_LocalSocket = null;
	UDPThread m_ReceiveThread = null;
	ParseThread m_ParseThread = null;
	static ConcurrentLinkedQueue<String> m_Queue = null;
	static Hashtable<String, DeviceInfo> m_DeviceSessionInfo = null;

	public UDPServer() {
		m_Queue = new ConcurrentLinkedQueue<>();
		m_DeviceSessionInfo = new Hashtable<String, DeviceInfo>();
	}

	@Override
	public void Stop() {
		if (m_LocalSocket != null)
			m_LocalSocket.close();
		if (m_ReceiveThread != null)
			m_ReceiveThread.Abort();
		if (m_ParseThread != null)
			m_ParseThread.Abort();
	}

	@Override
	public boolean Start(String ip, int port) {
		try {
			m_LocalSocket = new DatagramSocket(null);
			m_LocalSocket.bind(new InetSocketAddress(ip, port));

			m_ReceiveThread = new UDPThread(m_LocalSocket);
			new Thread(m_ReceiveThread).start();

			// Parse thread
			m_ParseThread = new ParseThread();
			m_ParseThread.SetCallBack(this);
			new Thread(m_ParseThread).start();
		} catch (SocketException e) {
			e.printStackTrace();
			return false;
		}
		return false;
	}

	@Override
	public boolean Send(BaseMessage msg, EndPoint endPoint) {
		try {
			byte[] sendBuff = msg.GetBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendBuff,
					sendBuff.length, InetAddress.getByName(endPoint.IP()),
					endPoint.Port());
			m_LocalSocket.send(sendPacket);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	public void RegisterSuccess(EndPoint endPoint) {
		BaseMessage msg = new BaseMessage();
		msg.SetAction(ServerCommand.OK);
		this.Send(msg, endPoint);
	}
}

class UDPThread extends MyRunnable {
	DatagramSocket m_UdpSocket;

	public UDPThread(DatagramSocket socket) {
		m_UdpSocket = socket;
	}

	@Override
	public void run() {
		while (!m_stop) {
			try {
				byte[] recvBuf = new byte[1024];
				DatagramPacket recvPacket = new DatagramPacket(recvBuf,
						recvBuf.length);
				m_UdpSocket.receive(recvPacket);
				if (recvPacket.getLength() <= 0) {
					Thread.sleep(100);
					continue;
				}
				String clientIP = recvPacket.getAddress().getHostAddress();
				int clientPort = recvPacket.getPort();
				UDPServer.m_Queue.add(String.format("%s:%d|%s", clientIP,
						clientPort, new String(recvPacket.getData()).trim()));
			} catch (Exception e) {
				// m_Log.Writeln(String.format("%s Exception : %s", "run",
				// e.getMessage()));
			}
		}
	}
}

interface IUPDServer {
	void RegisterSuccess(EndPoint endPoint);
}
