package server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;
import java.util.Vector;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;


public class Server {
	


    private final int port=25864;
    private ExecutorService pool = null;
    private AtomicLong userCount = new AtomicLong();
    private AtomicLong chatCount = new AtomicLong();
    private boolean running;
    private Vector <UserData> users = new Vector<>();
    private Vector <ChatRoom> chats = new Vector<>();
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    
    /*
    Server (int port) {
    	this.port = port;

    }
    */
    public void start() throws IOException {

		pool = Executors.newFixedThreadPool(10);
		running=true;		
		ServerSocket ss = new ServerSocket(port);
		System.out.println(dateFormat.format(new Date())+" - Server started on port: "+port);
		listen(ss);
    	
    }
    
    private void listen(ServerSocket serverS) throws IOException{
        while(running){
        		Socket clientSocket = serverS.accept();
        		ServerThread runnable= new ServerThread(clientSocket);
        		pool.execute(runnable);
        	}
        }  	
    
    
    class ServerThread implements Runnable { 	
		String line;
		Socket client;
    
    	ServerThread(Socket client) {
			this.client=client;
    	}	
    		
    	public void run() {
    		try{
    			InputStream input = client.getInputStream();
    			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    			
    			while(true){
    				line = reader.readLine();
    				System.out.println("Got message: "+line);  				
    				messageManager(client, line);
    				//send(client,"message accepted");
    			}
    		}catch(IOException e){
    			e.printStackTrace();
    		}
    	}
    }
    public void messageManager(Socket userSocket, String s) {
		String message, userUniqueIdString, userRoomIdString;

		message = s.substring(0, s.indexOf("\\u"));
		userUniqueIdString=s.substring(s.indexOf("\\u")+2, s.indexOf("\\r"));
		userRoomIdString = s.substring(s.indexOf("\\r")+2, s.indexOf("\\e"));
		
		long senderId = Long.parseLong(userUniqueIdString);
		long userRoomId = Long.parseLong(userRoomIdString);		
		UserData newClient = getUserInfoByUserId(senderId);
		
		//System.out.println("user ID "+senderId);
		if (senderId == 0 && !(message.startsWith("#login"))) {
			send(userSocket, "System: You have to login first", senderId, userRoomId);
			System.out.println(dateFormat.format(new Date())+" - Unregistred user tried to send message.");	
		}
    	
		//COMMAND
		else if (message.startsWith("#")) {
			//System.out.println("Command block");
			command(userSocket, message, senderId, userRoomId);
		}
		
		//NEW CHAT
		else if (userRoomId == 0) {
						
			if (newClient.getIsAgent()) {
				send(userSocket,"System: Message is not sent. There is no active chats for you", senderId, 0);
			}
			else {
				//CREATE NEW ROOM
							
				ChatRoom newChatRoom = createNewChatRoom(senderId, message);
				
				//ADD AGENT TO CHAT
				UserData availableAgent = checkFreeAgent();				
				if (null == availableAgent) {
					System.out.println("No agents");
					send(userSocket,"System: No agents. Wait for agent.", senderId, newChatRoom.getId());
					System.out.println(dateFormat.format(new Date())+" - No free agents. Customer waits for agent");	
					
				}
				else {
					System.out.println("Add agent");
					addAgentToChatRoom(newChatRoom, availableAgent.getUserId());
				}							
			}
		}
		//ACTIVE CHAT
		else {
			System.out.println("Active chat");
			sendMessageToActiveChat((newClient.getName() + ": "+message), senderId, userRoomId);
		}
    	
    }
    
    private void command(Socket userSocket, String message, long senderId, long roomId) {
		if (message.startsWith("#login")){
			
			// !!! CHECK COMMAND BY REGEXP
			
			if (senderId > 0) {
				send(userSocket, "Invalid command. You have already logged in",senderId,-1);
			}
			else {
				//System.out.println("New client");
				String name = message.substring(message.indexOf(":")+1);
				boolean isAgent = true;
				String textMessage = "System: Greetings, agent "+name+"! Wait for new chat";
				
				if (message.startsWith("#login-client")) {
					isAgent = false;
					textMessage = "System: Greetings, "+name+"! Please send message to start chat with the agent.";
				}
				long userId = registerUser(userSocket, name, isAgent);
				send(userSocket,textMessage,userId,0);
				
				//CHECK ACTIVE CHAT WITHOUT AGENT FOR NEW AGENT
				if (isAgent = true) {
					ChatRoom room = chatWithoutAgent();
					
					if (!(null == chatWithoutAgent())) {
						addAgentToChatRoom(room, userId);
					}					
				}
			}
		}
		
		else if (message.startsWith("#close")){
			for (int i =0; i<chats.size(); i++) {
				if (chats.get(i).getId() == roomId) {
					long clientId = chats.get(i).getClientId();
					long agentId = chats.get(i).getAgentId();	
					System.out.println("client id: "+clientId);
					System.out.println("agent Id: "+clientId);
					chats.remove(i);
					
					getUserInfoByUserId(clientId).setInChat(false);
					send(userSocket,"Sytstem: Chat has been closed.", clientId, 0);					
					System.out.println(dateFormat.format(new Date())+" - Chat room "+roomId+ " was closed by " + getUserInfoByUserId(senderId).getName());
										
					if (agentId >0) //If agent was in chat 
					{
						getUserInfoByUserId(agentId).setInChat(false);
						send(getUserInfoByUserId(agentId).getUserSocket(),"Sytstem: Chat has been closed.", agentId, 0);
						
						//Check for chats in queue
						ChatRoom newRoom = chatWithoutAgent();
						
						if (!(null == newRoom)) {
							addAgentToChatRoom(newRoom, agentId);
							//System.out.println("Chat without agent section");
						}						
					}			
				}
			}			
		}
		else if (message.startsWith("#logout")){
			if (getUserInfoByUserId(senderId).getInChat()) {
				send(userSocket, "Sytstem: You have to close active chat before logout", senderId, roomId);
			}
			else {
				UserData removedUser = getUserInfoByUserId(senderId);
				for (int i = 0; i<users.size(); i++) {
					if (users.get(i).equals(removedUser)) {
						users.remove(i);
						send(userSocket,"Sytstem: You were successfully logged out",0,0);
						try {
							userSocket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						System.out.println(dateFormat.format(new Date())+" - User " + removedUser.getName()+" was logged out.");
					}
				}
			}	
		}
    }
    
	void sendMessageToActiveChat(String message, long senderId, long roomId) {
		
		for (ChatRoom room: chats) {
			if (roomId == room.getId()) {
				//IF CHAT WITHOUT AGENT
				if (room.getAgentId()==0) {
					room.addTranscriptLine(message);
					System.out.println("Cannot find agent. Add transcript line");
				}
				//SEND MESSAGE TO ANOTHER PARTICIPANT
				else {
					UserData recieverData = getRecieverUserData(senderId, room);
					send(recieverData.getUserSocket(), message, recieverData.getUserId(), roomId);
				}																		
			}			
		}			
	}
	
    void send(Socket clientSocket, String message, long userId, long roomId){
    	try {
    			OutputStream output = clientSocket.getOutputStream();
    			PrintWriter writer = new PrintWriter(output, true);
    			writer.println(message+"\\u"+userId+"\\r"+roomId+"\\e");
    			System.out.println("Send message to userID#"+userId+": "+message+"\\u"+userId+"\\r"+roomId+"\\e");
    			//clientSocket.close();
    		}catch (IOException ex) {
    				System.out.println("Error : "+ex);
    		}
    }
    
	public ChatRoom createNewChatRoom(long senderId, String message) {
		long newRoomId = chatCount.incrementAndGet();
		UserData data = getUserInfoByUserId(senderId);
		ChatRoom newRoom = new ChatRoom(newRoomId, senderId, (data.getName() + ": "+message));							
		chats.add(newRoom);
		System.out.println(dateFormat.format(new Date())+" - New chat room #"+newRoomId +" has been created");
		data.setInChat(true);
		return newRoom;
	}	
	
	public UserData checkFreeAgent() {
		
		for (UserData agentInfo: users) {
			if ((agentInfo.getIsAgent()) && (!agentInfo.getInChat())) {				
				return agentInfo;
			}
		}
		
		return null;
	}
		
	public void addAgentToChatRoom(ChatRoom newRoom, Long agentId) {
			long roomId = newRoom.getId();
			UserData agent = getUserInfoByUserId(agentId);
			Socket cs = getUserInfoByUserId(newRoom.getClientId()).getUserSocket();
			Socket as = agent.getUserSocket();
			
			newRoom.setAgentId(agentId);		
			agent.setInChat(true);
			
			send(as,"System: New chat has been started.", agentId, roomId);
			send(cs,"System: Agent "+agent.getName()+" has been connected", newRoom.getClientId(), roomId);
			System.out.println(dateFormat.format(new Date())+" - Agent "+agent.getName()+" has been connected to chat #"+roomId);
			
			//SEND CLIENT MESSAGES TO AGENT																						
			for (String transcriptLine: newRoom.getTranscript()) {											
				send(as,transcriptLine, agentId, roomId);
			}					
	}
	
	public ChatRoom chatWithoutAgent() {
		for (ChatRoom room: chats) {
			if (room.getAgentId() == 0) {
				return room;
			}
		}
		return null;	
	}
	
	public UserData getRecieverUserData(long senderId, ChatRoom room) {
		if (senderId == room.getClientId()) {
			return getUserInfoByUserId(room.getAgentId());
			}
		else {
			return getUserInfoByUserId(room.getClientId());
		}
		
	}
    
	public UserData getUserInfoByUserId (long usrId) {
		for (UserData info: users) {
			if (info.getUserId() == usrId)
			return info;
		}
		return null;
	}

	public long registerUser(Socket userSocket, String name, boolean isAgent) {	
		long userId = userCount.incrementAndGet();
		users.add(new UserData(userSocket, name, userId, isAgent));
		String clientType = "Client";
		if(isAgent) {
			clientType = "Agent";
		}
		System.out.println(dateFormat.format(new Date())+" - User "+name+" is connected as "+clientType+". UserId: "+userId );
		return userId;
	}

	public Vector<UserData> getUsers() {
		return users;
	}
	public Vector <ChatRoom> getRooms(){
		return chats;
	}
}
