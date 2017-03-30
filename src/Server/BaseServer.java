package Server;

import Common.BaseMessage;

public interface BaseServer {
	boolean Start(String ip, int port);
	void Stop();
	boolean Send(BaseMessage msg, EndPoint endPoint);
}
