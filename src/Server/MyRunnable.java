package Server;

public class MyRunnable implements Runnable {
	boolean m_stop = false;
	
	@Override
	public void run() {
	}

	public void Abort() {
		m_stop = true;
	}
}
