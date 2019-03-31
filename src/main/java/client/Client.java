package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


//NEW PROJECT

class Client{
	
	private static long roomId = 0;
	private static long userId = 0;
    
    Client(){
    	
		Socket sk = null;
		try {
			sk = new Socket("127.0.0.1",25864);
		} catch (UnknownHostException e) {
			System.out.println("Cannot connect to server");
		} catch (IOException e) {
			System.out.println("Cannot connect to server");
		}
		System.out.println("Start with login command: #login-[client/agent]:[name]");
		ExecutorService pool = Executors.newFixedThreadPool(2);	
		pool.execute(new SendThread(sk));
		pool.execute(new RecieverThread(sk));
    }

    private class RecieverThread implements Runnable { 	
    	Socket socket;
        
    	RecieverThread(Socket socket) {
    		this.socket=socket;		
        	}
    		
        public void run() {
        	try{
        		BufferedReader sin=new BufferedReader(new InputStreamReader(socket.getInputStream()));
        			
        		while(true){
        			String s=sin.readLine();
        			//System.out.println("Got message from server"+s);
        			String message, userUniqueIdString, userRoomIdString;
        			message = s.substring(0, s.indexOf("\\u"));
        			System.out.print(message+"\n");
        			
        			userUniqueIdString=s.substring(s.indexOf("\\u")+2, s.indexOf("\\r"));
        			userRoomIdString = s.substring(s.indexOf("\\r")+2, s.indexOf("\\e"));
        			
        			Long userUniqueId = Long.parseLong(userUniqueIdString);
        			Long userRoomId = Long.parseLong(userRoomIdString);
        			
        			if (userId == 0 && userUniqueId > 0) {				
        				userId = userUniqueId;
        			}
        			if (roomId == 0 && userRoomId > 0) {
        				roomId = userRoomId;
        			}
        			if (userRoomId == 0 && roomId > 0) {
        				roomId=0;
        			}       			
        		}        		
        	}catch(IOException e){
        		System.out.println("Stopped get messages from server");
        		}
        	}
        } 
    
    private class SendThread implements Runnable { 	
    	Socket socket;
        
    	SendThread(Socket socket) {
    		this.socket=socket;		
        	}
    		
        public void run() {
        	try{
        		PrintStream sout=new PrintStream(socket.getOutputStream());
        		BufferedReader stdin=new BufferedReader(new InputStreamReader(System.in));
        			
        		while(true){
        			String s=stdin.readLine();
        			sout.println(s+"\\u"+userId+"\\r"+roomId+"\\e");
        			if ( s.equalsIgnoreCase("BYE") ){
        				System.out.println("Connection ended by client");
        				socket.close();
         				sout.close();
         		 		stdin.close();
         			   	break;
                    }
        		}        		
        	}catch(IOException e){
        			e.printStackTrace();
        		}
        }
   }
    
}
