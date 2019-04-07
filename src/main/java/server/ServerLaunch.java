package server;

import java.io.IOException;

public class ServerLaunch {

	public static void main(String[] args) throws IOException {
		
		new ServerLaunch();
		
	}
	public ServerLaunch() throws IOException {
		new Server().start();
		
	}

}
