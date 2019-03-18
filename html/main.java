import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;



public class main {


	private static List<GamePlayThread> threadList;  //사용자의 대기실 목록,채팅방,게임화면의 변화를 핸들러에 전달하는 쓰레드
	private static ReentrantLock lock;
	static int port=6000;
	
	private static ServerThread serverThread = null;
	
	private static Room [] room_array; 
	
	 private static HashMap<String, PrintWriter> lobby_hm;
     private static HashMap<String, PrintWriter> waiting_room_hm;
     private static HashMap<String, PrintWriter> game_field_hm;
	
	public static void main(String[] args) throws UnknownHostException {
		// TODO Auto-generated method stub
	
		// 자바 소켓 서버 ip 주소 화면에 띄우기
		 try {Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
	            while (en.hasMoreElements()) {
	                NetworkInterface ni = en.nextElement();
	                if (ni.isLoopback()) continue;
	                if (! ni.isUp()) continue;

	                List<InterfaceAddress> list = ni.getInterfaceAddresses();
	                InetAddress address;
	                for (InterfaceAddress ia : list) {
	                    address = ia.getAddress();
	                    if (address instanceof Inet4Address) {

	                     
	                        String chatting_ip = address.getHostAddress();
	                        System.out.println(ni.getDisplayName() + "의 IP 주소는 " +
	                                address.getHostAddress() + "입니다.\n");
	                    }
	                }
	            }
	        	InetAddress address = InetAddress.getLocalHost();
	    		System.out.println("로컬컴퓨터 이름:"+address.getHostName());
	    		System.out.println("로컬컴퓨터 IP주소:"+address.getHostAddress());


	        } catch (SocketException e) {
	           
	        }
		
		 room_array = new Room[20];  //대기방 정보를 나타내는 20개의 room_array 클래스 초기화. 배열 번호는 각 방 번호를 나타낸다. ex)room_array[0] -> 0번방, room_array[19]->19번방
		 
		 for(int i=0 ; i<20 ; i++) {
			 room_array[i] = new Room();
		 }
		 
		lobby_hm = new HashMap<String, PrintWriter>();
		waiting_room_hm = new HashMap<String, PrintWriter>();
        game_field_hm = new HashMap<String, PrintWriter>();
		
        if (serverThread == null) {
            try {
                serverThread = new ServerThread();
                serverThread.start();
                System.out.println("Server Thread를 시작하였습니다.\n");
                System.out.println("서비스가 시작되었습니다.\n");
            } catch (IOException e) {
            	 System.out.println("Server Thread를 시작하지 못했습니다." + e.toString());
            }
        } else {
        	System.out.println("서비스가 실행되고 있습니다.");
        }
        
          
	}
	
	public static class ServerThread extends Thread{
		private final HashMap<String, PrintWriter> hm;  // String->이름/현재위치(대기실 or 대기방 or 게임필드) 
       
 
		private final ServerSocket socket;
        
        
        public ServerThread() throws IOException {
            super();
            socket = new ServerSocket(port);
            socket.setSoTimeout(10000);
            hm = new HashMap<String, PrintWriter>();
            threadList = new ArrayList<GamePlayThread>();
            lock = new ReentrantLock();
        } // 생성자
        
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {                
                	GamePlayThread thread = new GamePlayThread(socket.accept());
                    thread.start();             
                    lock.lock();
                    threadList.add(thread);
                    lock.unlock();
                } catch (InterruptedIOException e) {
                } catch (SocketException e) {
                    return;
                } catch (IOException e) {
                    shutdown();
                    e.printStackTrace();
                    return;
                }
            }
        }
            
            public void shutdown() {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                clearlist();
                Thread.currentThread().interrupt();
            }

            private void clearlist() {
                if (!threadList.isEmpty()) {
                    lock.lock();
                    for (int i = 0; i < threadList.size(); ++i) {
                    	GamePlayThread t = threadList.get(i);
                        t.quit();
                        t.interrupt();
                    }
                    lock.unlock();
                }
            }
        
	}
	
	static class GamePlayThread extends Thread{
		    private Socket sock;
	        private String where;
	        private int room_number = -1;			//사용자가 접속한 방 번호. 접속하지 않았을 경우 -1로 할당
	         private String id;
	        private BufferedReader br;
	        private PrintWriter opw;

	        public GamePlayThread(Socket sock) {	//GamePlayThread의 생성자. WaitingRoom,ReadyRoom,GameField 입장 시 생성.
	            this.sock = sock;	 
	            try {
	                opw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream(),"utf-8"));
	                br = new BufferedReader(new InputStreamReader(sock.getInputStream(),"utf-8"));	  
	                
	               	              	               	             	                
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	        
	        @Override
	        public void run() {
	            try {
	                String line = null;	                
	                
	                ///////////////////사용자가 서버에 보내는 메세지를 읽어들인다///////////////////////////////
	                while ((line = br.readLine()) != null) {
	                	  String []infor = line.split("\\|");
	                	  
	                	///////////////사용자가 서버에 보낸 메세지를	기능에 따라 if문으로 분류하여 처리한다.///////////////////
	                	if(infor[0].equals("Lobby_Entrance")) {	                	
		                	where = "Lobby";
		                	 id = infor[1];
		                	 synchronized (lobby_hm) {
		                		 lobby_hm.put(id, opw);
		                	 }
		                			String room_infor_line="";
		                			
		                			room_infor_line = room_infor_summary();
		            	        	
		            	          PrintWriter pw = lobby_hm.get(id);
		            	          pw.println(room_infor_line);
		            	          pw.flush();		            	               
		 	                	System.out.println(id+"가 "+where+"에 입장하셨습니다.");
		 	                	room_infor_update();
		 	                	
		                }else if(infor[0].equals("Lobby_Exit")) {		               	
		                	System.out.println(id+"님이 Lobby에서 나갔습니다.");
		                	synchronized(lobby_hm){
		                		lobby_hm.remove(id);
		                	}
		                	quit();		
               	
		                }else if(infor[0].equals("WaitingRoom_Entrance")) {
		                	 where = "WaitingRoom";
		                	 id = infor[1];
		                	 String userType = infor[2];	//방을 만든 유저인지, 만들어진 방에 들어온 유저인지 구분 
		                	 String CharacterType = infor[3];
		                	 
		                	 synchronized (waiting_room_hm) {
		                		 waiting_room_hm.put(id, opw);
		                	 }
		               
		                	
		 	                	
		 	                	if(userType.equals("room_maker")) {
		 	                		for(int i=0 ; i<20 ; i++) {		//20개의 방 중 빈 방 찾아서 사용자가 설정한 방제로 방 만들고, 사용자 집어 넣기
		 	                			if(room_array[i].name.equals("")) {
		 	                				String room_name = infor[4];
		 	                				room_number = i;
		 	                				room_array[i].set_name(room_name);	//방 이름 설정
		 	                				room_array[i].user_entrance(id, Integer.parseInt(CharacterType));
		 	                				SendMsg(waiting_room_hm.get(room_array[i].user1),"Room_infor|"+room_name+"|"+Integer.toString(i));	//방에 들어온 사용자에게 방 이름,방 번호 전송
		 	                				SendMsg(waiting_room_hm.get(room_array[i].user1),"MSG|server|"+id+"님이"+"접속했습니다.");
		 	                				 System.out.println(id+"가 "+where+"에 입장하셨습니다.(방 번호:)"+ Integer.toString(i));
		 	                				break;
		 	                			}
		 	                		}
		 	                		
		 	                	}else if(userType.equals("visitor")){
		 	                		synchronized (room_array) {
		 	                				room_number = Integer.parseInt(infor[4]);		    //사용자가 입장한 방 번호 변수로 저장
		 	                				String room_name = room_array[room_number].name;	//사용자가 입장 할 방 이름
		 	                				
		 	                				room_array[room_number].user_entrance(id,Integer.parseInt(CharacterType));		// 입장 한 방의 저장 정보에 사용자 아이디 입력
		 	                				SendMsg(waiting_room_hm.get(id),"Room_infor|"+room_name+"|"+Integer.toString(room_number));	//방에 들어온 사용자에게 방 이름,방 번호 전송 	            
		 	                				
		 	                			
		 	                				System.out.println(id+"가 "+where+"에 입장하셨습니다.(방 번호:)"+ Integer.toString(room_number));
		 	                				//방에 위치한 사용자들에게 다른 유저가 접속한 사실, 상대방 사용자의 이름, 선택한 캐릭터 종류를 전송한다. 
		 	                				SendMsg(waiting_room_hm.get(room_array[room_number].user1),"Enemy_Entrance|"+room_array[room_number].user2+"|"+Integer.toString(room_array[room_number].user2CharacterType));
		 	                				SendMsg(waiting_room_hm.get(room_array[room_number].user2),"Enemy_Entrance|"+room_array[room_number].user1+"|"+Integer.toString(room_array[room_number].user1CharacterType));
		 	                				SendMsg(waiting_room_hm.get(room_array[room_number].user1),"MSG|server|"+id+"님이"+"접속했습니다.");
		 	                				SendMsg(waiting_room_hm.get(room_array[room_number].user2),"MSG|server|"+id+"님이"+"접속했습니다.");
		 	                		}
		 	                	}			 	                	
		 	                	
	 	                	    room_infor_update(); //로비에 있는 사용자들에게 대기방 업데이트 정보를 전송		 	                	    
		                
	                }else if(infor[0].equals("Character_Change")){
	                	//상릭터 정보
	                	String characterType = infor[1];
	                	//상대방의 캐릭터 정보를 전송한다
	                	if(room_array[room_number].user1.equals(id)) {
		                	//방에 입력된 해당 사용자의 캐릭터 정보를 변경한다.
		                	room_array[room_number].user1CharacterType = Integer.parseInt(characterType);
	                		SendMsg(waiting_room_hm.get(room_array[room_number].user2),"WaitingRoom_Character_Change|"+characterType);
	                		
	                	}else if(room_array[room_number].user2.equals(id)){
		                	//방에 입력된 해당 사용자의 캐릭터 정보를 변경한다.
		                	room_array[room_number].user2CharacterType = Integer.parseInt(characterType);
	                		SendMsg(waiting_room_hm.get(room_array[room_number].user1),"WaitingRoom_Character_Change|"+characterType);
	                		
	                	}
	                	
	                	
	                }else if(infor[0].equals("MSG")) {	
		 	            	
		 	            	String msg = infor[2];	//사용자가 채팅창에 입력한 메세지
		 	            	
		 	            	// 채팅을 보낸 사용자와 같은 방에 위치한 사용자들에게 메세지 전송
		 	            	if(!room_array[room_number].user1.equals("")) {
		 	            		String user_name = room_array[room_number].user1; 	//채팅 내용을 받을 사용자 이름
		 	            		SendMsg(waiting_room_hm.get(user_name),"MSG|"+id+"|"+msg);
		 	            	}
		 	            	if(!room_array[room_number].user2.equals("")) {
		 	            		String user_name = room_array[room_number].user2; 	//채팅 내용을 받을 사용자 이름
		 	            		SendMsg(waiting_room_hm.get(user_name),"MSG|"+id+"|"+msg);
		 	            	}
				 	            	             			                	
		                }else if(infor[0].equals("Ready")) {					                	
		                	String game_progess = infor[1]; //게임 진행 상황을 문자열로 받는다. ( 상대방의 게임 준비가 끝나서 게임을 시작할 수 있는 지. 또는 상대방을 더 기다려야 하는 지)
		                
		                	
		                	if(game_progess.equals("Game_Start")) {
		                		SendMsg(waiting_room_hm.get(room_array[room_number].user1),"Game_Start|");
		                		SendMsg(waiting_room_hm.get(room_array[room_number].user2),"Game_Start|");
		                		
		                	}else if(game_progess.equals("Waiting_Enemy")) {
		                		if(id.equals(room_array[room_number].user1)){
			                		SendMsg(waiting_room_hm.get(room_array[room_number].user2),"Enemy_Ready_Complete|");		
		                		}else if(id.equals(room_array[room_number].user2)) {
			                		SendMsg(waiting_room_hm.get(room_array[room_number].user1),"Enemy_Ready_Complete|");		
		                		}	                		
		                	}
		                	
		                }else if(infor[0].equals("Ready_Cancel")) {
		                	
		                	if(id.equals(room_array[room_number].user1)){
		                		SendMsg(waiting_room_hm.get(room_array[room_number].user2),"Enemy_Ready_Cancel|");		
	                		}else if(id.equals(room_array[room_number].user2)) {
		                		SendMsg(waiting_room_hm.get(room_array[room_number].user1),"Enemy_Ready_Cancel|");		
	                		}	
		                	
		                }else if(infor[0].equals("WaitingRoom_Exit")) {	
		                
		                	room_number=-1;
		                	synchronized(room_array) {
		                	for(int i=0 ; i<20 ; i++) {		//사용자가 나간 방이 빈 방이면, 방을 없앤다. 다른 사용자가 남아 있다면, 해당 사용자만 방에서 나간 것으로 처리한다.	                		
		                		if(room_array[i].user1.equals(id) ) { 	 
		                			room_array[i].user1="";
		                			
		                			if(room_array[i].user2.equals("")) { //방에 남은 사람이 없는 경우 방을 없앤다.
 	                				room_array[i].name="";	                			
		                			}else {								//방에 남은 사람이 있을 경우 상대방이 나간다는 메세지를 보낸다.
		                				SendMsg(waiting_room_hm.get(room_array[i].user2),"Enemy_Exit|"+id);
		                			}
 	                				break;		                		
		                		}
		                		
		                		if(room_array[i].user2.equals(id) ) { 	 
		                			room_array[i].user2="";
		                			
		                			if(room_array[i].user1.equals("")) { //방에 남은 사람이 없는 경우 방을 없앤다.
 	                				room_array[i].name="";
		                			}else {								//방에 남은 사람이 있을 경우 상대방이 나간다는 메세지를 보낸다.
		                				SendMsg(waiting_room_hm.get(room_array[i].user1),"Enemy_Exit|"+id);
		                			}
 	                				break;		                		
		                		}
		                	}
		                	}
		                	
 	                	    room_infor_update(); //대기방이 추가 되었으므로, 로비에 있는 사용자들에게 방 업데이트 정보를 전송		
		                	System.out.println(id+"님이 WaitingRoom에서 나갔습니다.");
		                	synchronized(waiting_room_hm){
		                	waiting_room_hm.remove(id);		
		                	}
		                	quit();
		                	
		                }else if(infor[0].equals("Game_Entrance")) {
		                	
		                	 //해당 소켓 사용자의 어플 상의 위치,아이디,입장한 방 번호 설정
		                	 where = "GameField";
			               	 id = infor[1];
		                	 room_number = Integer.parseInt(infor[2]);
		                	 
		                	 synchronized(waiting_room_hm) {
		                		 waiting_room_hm.remove(id);
		                	 }
		                	 synchronized (game_field_hm) {	                	
		                		 game_field_hm.put(id, opw);
		                	 }
		                
		                	 // 상대플레이어가 게임필드에 입장한 했는 지 game_field_hm 해쉬맵에 상대플레이어의 id로 만들어진 key 존재 여부로 확인한다.
		                	 if(game_field_hm.containsKey(room_array[room_number].user1) && game_field_hm.containsKey(room_array[room_number].user2)){
		                		 //상대 플레이어가 게임필드에 입장해 있으면 게임을 시작 메세지와,상대 유저의 아이디와 캐릭터 타입을 보낸다.
			                		SendMsg( game_field_hm.get(room_array[room_number].user1),"Game_Start|"+room_array[room_number].user2+"|"+Integer.toString(room_array[room_number].user2CharacterType));	
			                		SendMsg( game_field_hm.get(room_array[room_number].user2),"Game_Start|"+room_array[room_number].user1+"|"+Integer.toString(room_array[room_number].user1CharacterType));	
			                		SendMsg( game_field_hm.get(room_array[room_number].user1),"My_Turn_Start|new");	
			                		SendMsg( game_field_hm.get(room_array[room_number].user2),"Enemy_Turn_Start");	
			                		
		                	 }else {
		                		 //상대 플레이어가 게임필드에 입장해 있지 않으면, 접속 확인 메세지만 플레이어에게 보낸다.
		                		 SendMsg(game_field_hm.get(id),"Game_Entrance_Check");		
		                		 
		                	 }
		                	 
		 	               	System.out.println(id+"가 "+where+"에 입장하셨습니다. 방 번호:"+room_number);	
		 	               	
		 	            }else if(infor[0].equals("Yut_Throw_Result")){	//윷을 던진 결과를 상대방에게 전달. 윷을 던진 플레이어의 캐릭터를 상대방의 기기에서 움직일 수 있게 한다.
		 	            	
		 	            	if(room_array[room_number].user1.equals(id)) {
		                		SendMsg( game_field_hm.get(room_array[room_number].user2),line);	
		 	            	}else if(room_array[room_number].user2.equals(id)){
		 	            		SendMsg( game_field_hm.get(room_array[room_number].user1),line);	
		 	            	}
		 	            	
		 	            }else if(infor[0].equals("Item_Use")){		//아이템 사용 정보를 상대방에게 전송한다.		 	 
		 	            		if(room_array[room_number].user1.equals(id)) {		
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user2),line);
		 	            		}else if(room_array[room_number].user2.equals(id)) {
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user1),line);	
		 	            		}
		 	            	
		 	            }else if(infor[0].equals("Turn_End")) {
		 	            	String what = infor[1];	// 사용자가 던진 패가 윷,모가 나와서 턴을 한 번 더 실행할 지, 그대로 턴을 종료할 지 구분하기위한 변수
		 	            	
		 	            	if(what.equals("end")) {
		 	            		if(room_array[room_number].user1.equals(id)) {		//상대방에게 차례를 넘긴다.
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user2),"Enemy_Turn_End");
		 	            			Thread.sleep(1100);
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user2),"My_Turn_Start|new");	
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user1),"Enemy_Turn_Start");
		 	            		}else if(room_array[room_number].user2.equals(id)) {
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user1),"Enemy_Turn_End");	
		 	            			Thread.sleep(1100);
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user1),"My_Turn_Start|new");	
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user2),"Enemy_Turn_Start");	
		 	            		}
		 	            		
		 	            	}else if(what.equals("yut")) {
		 	              		if(room_array[room_number].user1.equals(id)) {		//상대방에게 차례를 넘긴다.
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user2),"Enemy_Turn_End");
		 	            			Thread.sleep(1100);
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user1),"My_Turn_Start|yut");	
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user2),"Enemy_Turn_Start");
		 	            		}else if(room_array[room_number].user2.equals(id)) {
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user1),"Enemy_Turn_End");	
		 	            			Thread.sleep(1100);
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user2),"My_Turn_Start|yut");	
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user1),"Enemy_Turn_Start");	
		 	            		}
		 	            		
		 	            		
		 	            	}else if(what.equals("mo")) {
		 	            		if(room_array[room_number].user1.equals(id)) {		//상대방에게 차례를 넘긴다.
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user2),"Enemy_Turn_End");
		 	            			Thread.sleep(1100);
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user1),"My_Turn_Start|mo");	
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user2),"Enemy_Turn_Start");
		 	            		}else if(room_array[room_number].user2.equals(id)) {
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user1),"Enemy_Turn_End");	
		 	            			Thread.sleep(1100);
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user2),"My_Turn_Start|mo");	
		 	            			SendMsg( game_field_hm.get(room_array[room_number].user1),"Enemy_Turn_Start");	
		 	            		}
		 	            	}
		 	            	
		 	            }else if(infor[0].equals("Game_Out")){
		 	            	if(room_array[room_number].user1.equals(id)) {
		 	            		SendMsg( game_field_hm.get(room_array[room_number].user2),"Enemy_Out");	
		 	            	}else if(room_array[room_number].user2.equals(id)) {
		 	            		SendMsg( game_field_hm.get(room_array[room_number].user1),"Enemy_Out");	
		 	            	}
		 	            }
		 	            else if(infor[0].equals("Game_Win")) {					  //게임에서 승리한 사용자가 보낸 메세지. 상대방에게 본인이 패배했다는 메세지를 보내자.		 	          
		 	            	if(room_array[room_number].user1.equals(id)) {
		 	            		SendMsg( game_field_hm.get(room_array[room_number].user2),"Game_End");	
		 	            	}else if(room_array[room_number].user2.equals(id)) {
		 	            		SendMsg( game_field_hm.get(room_array[room_number].user1),"Game_End");	
		 	            	}
		 	            	
		 	            	//게임이 종료되었으므로 사용자의 id를 키 값으로 사용하는 해쉬맵을 지운다.
		 	            	synchronized(game_field_hm){
		 	            	 game_field_hm.remove(id);
		 	            	 }
		 	            	 
		 	            	// 방을 비우고,로비에 있는 사용자들에게 방 업데이트 정보를 전송	
		 	            	room_array[room_number].user1="";
		 	            	room_array[room_number].user2="";
		 	            	room_array[room_number].name="";
		 	            	room_infor_update(); //로비에 있는 사용자들에게 대기방 업데이트 정보를 전송			 	    
		 	            	
		 	            	quit();	//해당 소켓을 지운다.
		 	            	
		 	            }else if(infor[0].equals("Game_Lose")) {		//게임에서 패배한 사용자가 게임을 종료할때 보낸 메세지. 서버에 존재하는 사용자의 소켓을 지우는 데 사용한다.
		 	            	//사용자의 id를 키 값으로 사용하는 해쉬맵들을 지운다.
		 	            	synchronized(game_field_hm){
			 	            	 game_field_hm.remove(id);
			 	            	 }
		 	            	 quit();	//해당 소켓을 지운다.
		 	            }
	                }
	            } catch(InterruptedIOException e) {
	            } catch (Exception ex) {
	            } /*finally {
	                synchronized (this.hm) {
	                    this.hm.remove(this.id);
	                }
	                broadcast(id + " 님이 접속 종료하였습니다.");
	                Message m = new Message();
	                m.what = MSG_ID;
	                m.obj = (id + " 님이 접속 종료하였습니다.");
	                mHandler.sendMessage(m);
	                try {
	                    lock.lock();
	                    threadList.remove(this);
	                    lock.unlock();
	                    if (sock != null) {
	                        sock.close();
	                        sock = null;
	                    }
	                    if (br != null) {
	                        br.close();
	                        br = null;
	                    }
	                    if (opw != null) {
	                        opw.close();
	                        opw = null;
	                    }
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }*/
	        }
	        public void SendMsg(PrintWriter pw, String msg) {
	        	//메세지 못 보냈을 때 예외처리하기
	        	try {
	        	pw.println(msg);
	        	pw.flush();
	        	}catch(Exception e) {
	        		System.out.println("메세지를 보내는 데 실패했습니다. 내용:"+msg);
	        	}
	        }
	        
	        public String room_infor_summary() {
	        	
	        	String room_infor_line="";   	
	     		for(int i=0 ; i < 20 ; i++) 
        		{		            	        		
        			if(!room_array[i].name.equals("")) {
        				String room_number = Integer.toString(i);
        				room_infor_line = room_infor_line + room_array[i].name+"|"+room_number+"|";	// 방 이름, 방 번호를 room_infor_line에 입력
        				
        				//방에 사람이 2명인 지 1명인 지 구분하는 조건 문. 2명이 면 클라이언트가 해당 방에 못 들어 오게 한다.
        				if(!room_array[i].user1.equals("") && !room_array[i].user2.equals("")) {	 // 방에 위치한 사용자 수를 room_infor_line에 입력
        				room_infor_line = room_infor_line +"2"+"|";	
        				}
        				else
        				{
        				room_infor_line = room_infor_line +"1"+"|";		
        				}
        			}
        		}	        		        
	        	return room_infor_line;
	        }
	        
	        public void room_infor_update() {	//대기방 목록의 정보를 사용자들에게 전달하는 메소드
	        	String room_infor_line="";
	        	room_infor_line = room_infor_summary();
	        	            	        	  
	        	 synchronized (lobby_hm) {
	                     Collection<PrintWriter> collection = lobby_hm.values();
	                     Iterator<PrintWriter> iter = collection.iterator();	        	 
	                    
	                     while (iter.hasNext()) {	                    	 	                    
	                         PrintWriter pw = iter.next();
	                         pw.println(room_infor_line);
	                         pw.flush();
	                     }	                	       
	        	 }
	        }
	        
			public void quit() {
	            if (sock != null) {
	            	 try {
		                    lock.lock();
		                    threadList.remove(this);
		                    lock.unlock();
		                    if (sock != null) {
		                        sock.close();
		                        sock = null;
		                    }
		                    if (br != null) {
		                        br.close();
		                        br = null;
		                    }
		                    if (opw != null) {
		                        opw.close();
		                        opw = null;
		                    }
		                } catch (Exception e) {
		                    e.printStackTrace();
		                }
	                } 
	            }
	        }

	//대기방의 정보를 저장하는 클래스//
	static class Room {
		
		String name=""; // 방이름
		String user1="";  // 대기방에 들어온 유저의 id
		String user2="";
		int user1CharacterType=1;	//대기방에 들어온 유저의 사용 캐릭터 종류. 1-> 일반 캐릭터 2-> 사용자의 얼굴을 사용한 캐릭터
		int user2CharacterType=1;
		
		void set_name(String name) {	//방이름 설정
			this.name = name;
		}
		
		void user_entrance(String user,int characterType) {	//대기방에 들어온 유저 설정
			if(user1.equals("")) {			//비어 있는 방이면 user1에 들어온 유저 이름 입력, user1이 이미 들어온 방이면 user2에 들어온 유저 이름 입력
				user1 = user;
				user1CharacterType = characterType;
			}else if(user2.equals("")){		
				user2 = user;		
				user2CharacterType = characterType;
				
			}
		}

	}


}


