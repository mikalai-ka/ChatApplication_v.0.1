package server;

import java.util.ArrayList;

//NEW PROJECT
class ChatRoom {
	private long chatRoomId;
	private long agentId=0;
	private long clientId=0;
	private ArrayList <String> transcript = new ArrayList <String>();
	
	ChatRoom(long chatRoomId, long clientId, String transcriptLine) {
		this.chatRoomId = chatRoomId;
		this.clientId = clientId;
		transcript.add(transcriptLine);
	}
	
	long getId() {
		return chatRoomId;
	}	
	long getAgentId() {
		return agentId;
	}
	long getClientId() {
		return clientId;
	}
	ArrayList <String> getTranscript(){
		return transcript;
	}	
	void setAgentId(long agentId) {
		this.agentId = agentId;
	}
	void setClientId(long clientId) {
		this.clientId = clientId;
	}
	void addTranscriptLine(String message) {
		transcript.add(message);
	}
}
