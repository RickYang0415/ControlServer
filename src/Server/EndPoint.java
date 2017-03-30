package Server;

public class EndPoint {
	private String m_ip = "";
	private int m_port = 0;
	public EndPoint(String ip, int port) {
		m_ip = ip;
		m_port = port;
	}
	
	public String IP()
	{
		return this.m_ip;
	}
	
	public int Port()
	{
		return this.m_port;
	}
	
	public String ToString()
	{
		return String.format("%s:%d", this.m_ip, this.m_port);
	}
}
