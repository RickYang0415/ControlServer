package Server;

import Common.BaseMessage;
import Common.Utility;

class WebSocketParseThread extends MyRunnable {
	IReceiveMsg m_callback = null;

	@Override
	public void run() {
		while (!m_stop) {
			try {
				if (WebServer.m_Queue.size() > 0) {
					String item = WebServer.m_Queue.poll();
					if (m_callback != null) {
						String[] args = Utility.Split("[;|]", item);
                        String[] epArgs = Utility.Split(":", args[0]);
                        EndPoint ep = new EndPoint(epArgs[0], Integer.parseInt(epArgs[1]));
                        BaseMessage newMsg = new BaseMessage();
                        newMsg.SetAction(Integer.parseInt(args[1]));
                        for (int i = 2; i < args.length; i++) {
                            newMsg.GetArray().add(args[i]);
                        }
                        m_callback.ReceiveMsg(newMsg, ep);
					}
				}
				Thread.sleep(100);
			} catch (Exception e) {
				System.out.println(e.toString());
			}
		}
	}
	
	void SetCallback(IReceiveMsg callback)
	{
		this.m_callback = callback;
	}
}