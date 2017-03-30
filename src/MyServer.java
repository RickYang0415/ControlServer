import Server.UDPServer;
import Server.WebServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

public class MyServer {
	static int webSocketPort = 88;
	static int port = 7777;

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		try {
			InetAddress hostIP = InetAddress.getLocalHost();
			System.out.println(String.format("Server start at %s:%d",
					hostIP.getHostAddress(), port));
			System.out.println(String.format("WebServer start at %s:%d",
					hostIP.getHostAddress(), webSocketPort));

			UDPServer udpServer = new UDPServer();
			udpServer.Start(hostIP.getHostAddress(), port);

			WebServer hostServer = new WebServer();
			hostServer.Start(hostIP.getHostAddress(), webSocketPort);

			new Scanner(System.in).nextLine();
			udpServer.Stop();
			hostServer.Stop();
		} catch (UnknownHostException e) { // TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Server Stop...");
	}
}
