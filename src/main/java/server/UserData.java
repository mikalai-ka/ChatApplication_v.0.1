package server;

import java.net.Socket;

class UserData {
	private Socket userSocket;
	private String name;
	private long userId;
	private boolean isAgent = false;
	private boolean inChat = false;
	
	public UserData(Socket userSocket, String name, long userId, boolean isAgent) {
		this.userSocket = userSocket;
		this.name = name;
		this.userId = userId;
		this.isAgent = isAgent;
	}
	
	public UserData(Socket userSocket, String name, long userId) {
		this.userSocket = userSocket;
		this.name = name;
		this.userId = userId;
	}

	public Socket getUserSocket() {
		return userSocket;
	}
	public String getName() {
		return name;
	}
	public long getUserId() {
		return userId;
	}
	public boolean getIsAgent() {
		return isAgent;
	}
	public boolean getInChat() {
		return inChat;
	}
	public void setInChat(boolean inChat) {
		this.inChat=inChat;
	}
	public void setIsAgent(boolean isAgent) {
		this.isAgent=isAgent;
	}
}
