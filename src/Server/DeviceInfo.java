package Server;

import java.util.ArrayList;

public class DeviceInfo {
	String serialNumber = "";
	String modelName = "";
	String address = "";
	String cpu = "";
	String memory = "";

	public DeviceInfo(ArrayList<String> str) {
		for (int i = 0; i < str.size(); i++) {
			switch (i) {
			case 0:
				this.serialNumber = str.get(i);
				break;
			case 1:
				this.modelName = str.get(i);
				break;
			case 2:
				this.cpu = str.get(i);
				break;
			case 3:
				this.memory = str.get(i);
				break;
			case 4:
				this.address = str.get(i);
				break;
			}
		}
	}

	public String ToString() {
		return String.format("%s,%s,%s,%s,%s", serialNumber, modelName,
				address, cpu, memory);
	}
}
