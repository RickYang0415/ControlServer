package Server;

import java.util.ArrayList;

import Common.BaseMessage;
import Common.Utility;
import Common.Command;

public class ParseThread extends MyRunnable {
	IUPDServer m_callback = null;

	public void SetCallBack(IUPDServer callback) {
		m_callback = callback;
	}

	@Override
	public void run() {
		while (!m_stop) {
			try {
				if (UDPServer.m_Queue.size() > 0) {
					String item = UDPServer.m_Queue.poll();
					String[] args = Utility.Split("[;|,]", item);
					String[] epArgs = Utility.Split(":", args[0]);
					EndPoint ep = new EndPoint(epArgs[0],
							Integer.parseInt(epArgs[1]));
					BaseMessage msg = new BaseMessage();
					msg.Action(Integer.parseInt(args[1]));
					for (int i = 2; i < args.length; i++) {
						msg.GetArray().add(args[i]);
					}
					ReceiveMsg(msg, ep);
				}
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	synchronized void ReceiveMsg(BaseMessage msg, EndPoint endPoint) {
		DeviceInfo info = new DeviceInfo(msg.GetArray());
		switch (msg.GetAction()) {
		case Command.DEVICE_REGISTER:
			UDPServer.m_DeviceSessionInfo.put(endPoint.ToString(), info);
			m_callback.RegisterSuccess(endPoint);
			System.out.println(String.format("Device %s online.",
					info.serialNumber));
			break;
		case Command.DEVICE_DISREGISTER:
			UDPServer.m_DeviceSessionInfo.remove(endPoint.ToString());
			System.out.println(String.format("Device %s off line.",
					info.serialNumber));
			break;
		case Command.DEVICE_UPDATE:
			UDPServer.m_DeviceSessionInfo.put(endPoint.ToString(), info);
			System.out.println(info.ToString());
			break;
		default:
			break;
		}
	}
}
