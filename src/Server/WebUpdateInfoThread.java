package Server;

public class WebUpdateInfoThread extends MyRunnable {
	ISendDevice m_callback = null;
	EndPoint m_endPoint = null;

	@Override
	public void run() {
		while (!this.m_stop) {
			try {
				if (m_callback != null) {
					m_callback.SendDeviceInfo(m_endPoint);
				}
				Thread.sleep(1000);
			} catch (Exception ex) {

			}
		}
	}

	void SetCallBack(ISendDevice callback) {
		m_callback = callback;
	}

	void SetEndPoint(EndPoint ep) {
		m_endPoint = ep;
	}
}
