package Server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Hashtable;

import Common.ControlAction;
import Common.Utility;

class WebSocketReceiveThread extends MyRunnable {
	Socket m_socket = null;
	InputStream m_InStream = null;
	EndPoint m_RemoteEndPoint = null;
	boolean m_IsFirst = true;

	@Override
	public void run() {
		while (!m_stop) {
			try {
				if (m_InStream.available() <= 0) {
					Thread.sleep(100);
					continue;
				}
				byte[] recvBuf = new byte[m_InStream.available()];
				m_InStream.read(recvBuf);
				RefString refItem = new RefString();
				int decodeLength = Decode(recvBuf, refItem);
				if (decodeLength <= 0) {
					continue;
				}
				if (refItem.value.equals("Disconnect")) {
					WebServer.m_Queue.add(String.format("%s|%d", m_RemoteEndPoint.ToString(), ControlAction.CLSV_UNREGISTER));
					continue;
				}
				String recvData = String.format("%s|%s",
						m_RemoteEndPoint.ToString(), refItem.value);
				WebServer.m_Queue.add(recvData);
				Thread.sleep(100);
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}

	private int Decode(byte[] recvBuf, RefString recvString) {
		try {
			if (m_IsFirst) {
				String newLine = "\r\n";
				Hashtable<String, String> header = new Hashtable<>();
				String recvData = new String(recvBuf);
				String[] parameters = Utility.Split("[\\r\\n]", recvData);
				for (String parameter : parameters) {
					int index = parameter.indexOf(":");
					if (index > -1) {
						String key = parameter.substring(0, index);
						String value = parameter.substring(index + 2,
								parameter.length());
						header.put(key, value);
					}
				}
				String newKey = GetAcceptKey(header.get("Sec-WebSocket-Key"));
				String newHead = String.format(
						"HTTP/1.1 101 Switching Protocols%s", newLine);
				newHead += String.format("Upgrade: WebSocket%s", newLine);
				newHead += String.format("Connection: Upgrade%s", newLine);
				newHead += String.format("Sec-WebSocket-Accept: %s%s%s",
						newKey, newLine, newLine);
				m_socket.getOutputStream().write(newHead.getBytes("UTF8"));
				m_IsFirst = false;
			} else {
				int length = recvBuf[1] & 0x7f;
				byte[] masks = new byte[4];
				byte[] dataBuf = null;
				switch (length) {
				case 126: {
					System.arraycopy(recvBuf, 4, masks, 0, 4);
					short u16Length = (short) ((recvBuf[2] & 0xFF) << 8 | (recvBuf[3] & 0xFF));
					dataBuf = new byte[u16Length];
					System.arraycopy(recvBuf, 8, dataBuf, 0, u16Length);
				}
					break;
				case 127: {
				}
					break;
				default: {
					System.arraycopy(recvBuf, 2, masks, 0, 4);
					dataBuf = new byte[length];
					System.arraycopy(recvBuf, 6, dataBuf, 0, length);
				}
					break;
				}
				for (int i = 0; i < dataBuf.length; i++) {
					dataBuf[i] = (byte) (dataBuf[i] ^ masks[i % 4]);
				}
				if (IsDisconnect(dataBuf)) {
					recvString.value = "Disconnect";
					return recvString.value.length();
				}
				recvString.value = new String(dataBuf, "UTF8").trim();
				return recvString.value.length();
			}
		} catch (Exception e) {
		}
		return recvString.value.length();
	}

	private boolean IsDisconnect(byte[] src) {
		byte[] target = new byte[] { (byte) 3, (byte) 233 };
		return Arrays.equals(src, target);
	}

	private String GetAcceptKey(String keySource) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			byte[] rawBuf = md
					.digest((keySource + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
							.getBytes("UTF8"));
			Base64.Encoder encoder = Base64.getEncoder();
			return encoder.encodeToString(rawBuf);

		} catch (Exception e) {
			System.out.println(e.toString());
			return "";
		}
	}

	void SetSocket(Socket socket) {
		try {
			m_socket = socket;
			m_InStream = m_socket.getInputStream();
			String remoteIP = socket.getInetAddress().getHostAddress();
			int remotePort = socket.getPort();
			m_RemoteEndPoint = new EndPoint(remoteIP, remotePort);
		} catch (IOException e) {
			System.out.println(e.toString());
		}
	}

	class RefString {
		public String value = "";
	}
}
