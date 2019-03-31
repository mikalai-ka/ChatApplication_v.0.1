package client;

public class ChatLaunch {
	public static void main(String[] args) {
		try {
		new Client();
		}catch(Exception e) {
			System.out.println("Error has been occured");
		}
	}
}
