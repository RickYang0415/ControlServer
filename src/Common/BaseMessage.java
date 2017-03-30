package Common;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class BaseMessage {
	ArrayList<String> m_arr = null;
	private int m_action = 0;

	public BaseMessage() {
		m_arr = new ArrayList<String>();
	}

	public ArrayList<String> GetArray() {
		return this.m_arr;
	}

	public void Action(int value) {
		m_action = value;
	}
	
	public void SetAction(int action) {
		m_action = action;
    }
	
	public String ToString() {
        StringBuilder SB = new StringBuilder();
        SB.append(String.format("%d|", m_action));
        for (String arg : m_arr) {
            SB.append(String.format("%s;", arg));
        }
        return SB.toString().substring(0, SB.length());
    }

	public int GetAction() {
		return this.m_action;
	}
	
	public byte[] GetBytes() {
        return this.ToString().getBytes(Charset.forName("UTF-8"));
    }
}
