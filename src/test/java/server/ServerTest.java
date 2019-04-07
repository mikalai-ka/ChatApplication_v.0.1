package server;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.is;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.Test;



public class ServerTest {

	@Test
	public void unregistredUser() throws IOException {
		String input = "Hello\\u0\\r0\\e";
		String expected = "System: You have to login first\\u0\\r0\\e\r\n";
		Server server = new Server();
		checkResponseFromServer (server, input, expected);
	}
	@Test
	public void loginAsClient() throws IOException {
		String input = "#login-client:Ted\\u0\\r0\\e";
		String expected = "System: Greetings, Ted! Please send message to start chat with the agent.\\u1\\r0\\e\r\n";
		Server server = new Server();
		checkResponseFromServer (server, input, expected);
		checkUserAdded(server, "Ted", false);
	}
	@Test
	public void loginAsAgent() throws IOException {
		String input = "#login-agent:Mat\\u0\\r0\\e";
		String expected = "System: Greetings, agent Mat! Wait for new chat\\u1\\r0\\e\r\n";
		Server server = new Server();
		checkResponseFromServer (server, input, expected);
		checkUserAdded(server, "Mat", true);
	}	
	private void checkResponseFromServer(Server server, String input, String expected) throws IOException {
		Socket socket = mock(Socket.class);
		ByteArrayOutputStream out= new ByteArrayOutputStream();
		ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
		when(socket.getInputStream()).thenReturn(in);
		when(socket.getOutputStream()).thenReturn(out);
		server.start();
		server.messageManager(socket, input);
		assertThat(out.toString(), is(expected));
	}
	private void checkUserAdded(Server server, String name, boolean isAgent) {
		UserData data = server.getUserInfoByUserId(1);
		assertNotNull(data);
		assertThat(data.getName(), is(name));
		assertThat(data.getIsAgent(), is(isAgent));		
	}
	
	//START NEW CHAT WITHOUT AGENT
	@Test
	public void startChatWithoutAgent() throws IOException{
		String input = "#login-client:Ted\\u0\\r0\\e";
		String output = "System: No agents. Wait for agent.\\u1\\r1\\e\r\n";
		Socket clientSocket = mock(Socket.class);
		ByteArrayOutputStream cout= new ByteArrayOutputStream();
		ByteArrayInputStream cin = new ByteArrayInputStream(input.getBytes());
		when(clientSocket.getInputStream()).thenReturn(cin);
		when(clientSocket.getOutputStream()).thenReturn(cout);
		Server server = new Server();
		server.messageManager(clientSocket, input);
		cout= new ByteArrayOutputStream();
		when(clientSocket.getOutputStream()).thenReturn(cout);
		server.messageManager(clientSocket, "Hello\\u1\\r0\\e");

		assertThat(cout.toString(), is(output));
	}
	//START NEW CHAT WITH AGENT
	@Test
	public void startChat() throws IOException{
		String clientInput = "Hello\\u1\\r0\\e";
		String expectedClient = "System: Agent Agent1 has been connected\\u1\\r1\\e\r\n";
		String expectedAgent= "System: New chat has been started.\\u2\\r1\\e\r\nClient1: Hello\\u2\\r1\\e\r\n";
		Socket clientSocket = mock(Socket.class);
		Socket agentSocket = mock(Socket.class);
		Server server = new Server();

		ByteArrayOutputStream cout= new ByteArrayOutputStream();
		ByteArrayOutputStream aout= new ByteArrayOutputStream();	
		when(agentSocket.getOutputStream()).thenReturn(aout);
		when(clientSocket.getOutputStream()).thenReturn(cout);
		server.registerUser(clientSocket, "Client1", false);
		server.registerUser(agentSocket, "Agent1", true);
		server.getUserInfoByUserId(2).setIsAgent(true);
		server.messageManager(clientSocket, clientInput);
		assertThat(cout.toString(), is(expectedClient));
		assertThat(aout.toString(), is(expectedAgent));
	}
	//AGENT GETS CHAT AFTER LOGIN

	@Test
	public void startChatAfterLogin() throws IOException{
		String agentInput = "#login-agent:Agent1\\u0\\r0\\e";
		String expectedClient = "System: Agent Agent1 has been connected\\u1\\r1\\e\r\n";
		String expectedAgent= "System: Greetings, agent Agent1! Wait for new chat\\u2\\r0\\e\r\nSystem: New chat has been started.\\u2\\r1\\e\r\nClient1: Hello\\u2\\r1\\e\r\n";
		Socket clientSocket = mock(Socket.class);
		Socket agentSocket = mock(Socket.class);

		ByteArrayOutputStream cout= new ByteArrayOutputStream();
		ByteArrayOutputStream aout= new ByteArrayOutputStream();		
		when(agentSocket.getOutputStream()).thenReturn(aout);
		when(clientSocket.getOutputStream()).thenReturn(cout);
		Server server = new Server();
		server.registerUser(clientSocket, "Client1", false);
		server.createNewChatRoom(1, "Hello");
		server.messageManager(agentSocket, agentInput);	
		ByteArrayOutputStream cout2= new ByteArrayOutputStream();
		when(clientSocket.getOutputStream()).thenReturn(cout2);
		assertThat(cout.toString(), is(expectedClient));
		assertThat(cout2.toString(), is(""));
		assertThat(aout.toString(), is(expectedAgent));	
	}
	//SEND MESSAGES BETWEEN PARTICIPANTS
	@Test
	public void chatInterraction() throws IOException {
		String agentInput = "Test message from agent";
		String clientInput = "Test message from client";
		Socket clientSocket = mock(Socket.class);
		Socket agentSocket = mock(Socket.class);
		ByteArrayOutputStream cout= new ByteArrayOutputStream();		
		ByteArrayOutputStream aout= new ByteArrayOutputStream();	
		when(agentSocket.getOutputStream()).thenReturn(aout);		
		when(clientSocket.getOutputStream()).thenReturn(cout);
		Server server = new Server();
		long clientId = server.registerUser(clientSocket, "Client1", false);
		long agentId = server.registerUser(agentSocket, "Agent1", true);
		ChatRoom chat = server.createNewChatRoom(clientId, "Hello");
		long chatId = chat.getId();
		chat.setAgentId(agentId);
		server.getUserInfoByUserId(agentId).setInChat(true);
		String agentExpected = messageWithMetainfo("Client1: "+clientInput, agentId, chatId, true); 
		String clientExpected = messageWithMetainfo("Agent1: "+agentInput, clientId, chatId, true);
		clientInput = messageWithMetainfo(clientInput, clientId, chatId, false);
		agentInput = messageWithMetainfo(agentInput, agentId, chatId, false);

		
		server.messageManager(clientSocket, clientInput);
		assertThat(cout.toString(), is(""));
		assertThat(aout.toString(), is(agentExpected));
		aout= new ByteArrayOutputStream();
		when(agentSocket.getOutputStream()).thenReturn(aout);
		server.messageManager(agentSocket, agentInput);
		System.out.println("aout: "+aout);
		System.out.println("cout: "+cout);
		assertThat(aout.toString(), is(""));
		assertThat(cout.toString(), is(clientExpected));
		
	}
	private String messageWithMetainfo(String text, long usrId, long chatId, boolean response) {
		String messageEnd = "";
		if (response) {
			messageEnd = "\r\n";
		}		
		return text + "\\u"+usrId+"\\r"+chatId+"\\e"+messageEnd;
	}

	//AGENT GETS CHAT AFTER CLOSE CHAT
	//AGENT SEND MESSAGE WITHOUT CHATROOM
	//CLOSE CHAT AS AGENT
	//CLOSE CHAT AS CLIENT
	//CLOSE CHAT WITHOUT AGENT
	//LOGOUT AGENT WITH ACTIVE CHAT
	//LOGOUT CLIENT WITH ACTIVE CHAT
	//LOGOUT AGENT
	//LOGOUT CLIENT

}
