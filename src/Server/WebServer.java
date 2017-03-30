package Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Common.BaseMessage;
import Common.ControlAction;
import Common.ServerCommand;
import Common.Utility;

import java.util.Scanner;

import javax.xml.bind.DatatypeConverter;

public class WebServer implements BaseServer, IAcceptSocket, IReceiveMsg,
		ISendDevice {
	SocketServerThread socketserver = null;
	static public Hashtable<String, Socket> m_Sessions = null;
	Hashtable<String, WebSocketReceiveThread> m_ReceiveThreadTable = null;
	Hashtable<String, WebSocketParseThread> m_ParseThreadTable = null;
	Hashtable<String, WebUpdateInfoThread> m_SendThreadTable = null;
	static ConcurrentLinkedQueue<String> m_Queue = null;

	public WebServer() {
		WebServer.m_Queue = new ConcurrentLinkedQueue<String>();
		m_Sessions = new Hashtable<>();
		m_ReceiveThreadTable = new Hashtable<String, WebSocketReceiveThread>();
		m_ParseThreadTable = new Hashtable<String, WebSocketParseThread>();
		m_SendThreadTable = new Hashtable<String, WebUpdateInfoThread>();
	}

	@Override
	public boolean Start(String ip, int port) {
		socketserver = new SocketServerThread(ip, port);
		socketserver.SetCallBack(this);
		new Thread(socketserver).start();
		return false;
	}

	@Override
	public void Stop() {
		try {
			if (socketserver != null)
				socketserver.Abort();

			if (m_ReceiveThreadTable != null) {
				for (WebSocketReceiveThread thread : m_ReceiveThreadTable
						.values()) {
					thread.Abort();
				}
			}

			if (m_ParseThreadTable != null) {
				for (WebSocketParseThread thread : m_ParseThreadTable.values()) {
					thread.Abort();
				}
			}

			if (m_SendThreadTable != null) {
				for (WebUpdateInfoThread thread : m_SendThreadTable.values()) {
					thread.Abort();
				}
			}

			for (Socket socket : m_Sessions.values()) {
				if (socket.isConnected())
					socket.close();
			}
		} catch (Exception ex) {

		}
	}

	@Override
	public void ReceiveSocket(Socket socket) {
		try {
			String ip = socket.getInetAddress().getHostAddress();
			int port = socket.getPort();
			String key = String.format("%s:%d", ip, port);
			if (!m_Sessions.containsKey(key)) {
				m_Sessions.put(key, socket);
			}

			WebSocketReceiveThread receiveThread = new WebSocketReceiveThread();
			receiveThread.SetSocket(socket);
			m_ReceiveThreadTable.put(String.format("%s:%d", ip, port),
					receiveThread);

			WebSocketParseThread parseThread = new WebSocketParseThread();
			parseThread.SetCallback(this);

			new Thread(receiveThread).start();
			new Thread(parseThread).start();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	@Override
	synchronized public boolean Send(BaseMessage msg, EndPoint endPoint) {
		try {
			Socket remoteSocket = m_Sessions.get(endPoint.ToString());
			OutputStream outStream = remoteSocket.getOutputStream();
			byte[] encodeByte = Encode(msg.GetBytes());
			outStream.write(encodeByte);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	@Override
	synchronized public void ReceiveMsg(BaseMessage msg, EndPoint endPoint) {
		switch (msg.GetAction()) {
		case ControlAction.CLSV_REGISTER:
			CLSV_REGISTER_func(endPoint);
			break;
		case ControlAction.CLSV_UNREGISTER:
			CLSV_UNREGISTER_func(endPoint);
			break;
		// case
		}
	}

	void CLSV_REGISTER_func(EndPoint endPoint) {
		/*
		 * BaseMessage baseMsg = new BaseMessage();
		 * baseMsg.Action(ServerCommand.OK); Send(baseMsg, endPoint);
		 */
		WebUpdateInfoThread updateThread = new WebUpdateInfoThread();
		updateThread.SetCallBack(this);
		updateThread.SetEndPoint(endPoint);
		m_SendThreadTable.put(endPoint.ToString(), updateThread);
		new Thread(updateThread).start();
	}

	void CLSV_UNREGISTER_func(EndPoint endPoint) {
		RemoteSession(endPoint.ToString());
	}

	private byte[] Encode(byte[] src) {
		try {
			byte[] sendBuf = null;
			if (src.length < 126) {
				sendBuf = new byte[src.length + 2];
				sendBuf[0] = (byte) 0x81;
				sendBuf[1] = (byte) src.length;
				System.arraycopy(src, 0, sendBuf, 2, src.length);
			} else if (src.length >= 126 && src.length < 0xFFFF) {
				sendBuf = new byte[src.length + 4];
				sendBuf[0] = (byte) 0x81;
				sendBuf[1] = 126;
				byte[] lengthArgs = ByteBuffer.allocate(2)
						.putShort((short) src.length).array();
				System.arraycopy(lengthArgs, 0, sendBuf, 2, lengthArgs.length);
				System.arraycopy(src, 0, sendBuf, 4, src.length);
			}
			return sendBuf;
		} catch (Exception e) {
			return null;
		}
	}

	public void RemoteSession(String endPoint) {
		try {
			if (m_ReceiveThreadTable.containsKey(endPoint)) {
				WebSocketReceiveThread receiveThread = (WebSocketReceiveThread) m_ReceiveThreadTable
						.get(endPoint);
				m_ReceiveThreadTable.remove(endPoint);
				receiveThread.Abort();
			}
			if (m_ParseThreadTable.containsKey(endPoint)) {
				WebSocketParseThread parseThread = (WebSocketParseThread) m_ParseThreadTable
						.get(endPoint);
				m_ParseThreadTable.remove(endPoint);
				parseThread.Abort();
			}
			if (m_Sessions.containsKey(endPoint)) {
				Socket socket = m_Sessions.get(endPoint);
				if (socket.isConnected()) {
					socket.close();
				}
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void SendDeviceInfo(EndPoint endPoint) {
		for (DeviceInfo item : UDPServer.m_DeviceSessionInfo.values()) {
			try {
				BaseMessage msg = new BaseMessage();
				msg.Action(ControlAction.SVCL_NOTIFY);
				msg.GetArray().add(item.ToString());
				Send(msg, endPoint);
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

class SocketServerThread extends MyRunnable {
	ServerSocket m_ServerSocket = null;
	String m_ip = "";
	int m_port = 0;
	IAcceptSocket m_callback = null;

	public SocketServerThread(String ip, int port) {
		m_ip = ip;
		m_port = port;
	}

	public void SetCallBack(IAcceptSocket callback) {
		m_callback = callback;
	}

	@Override
	public void run() {
		try {
			m_ServerSocket = new ServerSocket();
			m_ServerSocket.bind(new InetSocketAddress(m_ip, m_port));
			m_ServerSocket.setSoTimeout(1000);

			while (!m_stop) {
				try {
					Socket remoteSocket = m_ServerSocket.accept();
					if (m_callback == null) {
						continue;
					}
					m_callback.ReceiveSocket(remoteSocket);
				} catch (Exception e) {
				}
			}
			m_ServerSocket.close();
		} catch (IOException e) {
			System.out.println(e.toString());
		}

	}
}
