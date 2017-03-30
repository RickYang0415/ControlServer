package Server;

import Common.BaseMessage;

public interface IReceiveMsg {
	void ReceiveMsg(BaseMessage msg, EndPoint endPoint);
}
