import java.io.*; 
import java.util.TimeZone;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.io.File;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.*;
import java.security.KeyStore;

public final class Server implements Runnable {
	private boolean isHTTPS;
	private final int serverPort;
	private ServerSocket socket;
	private DataOutputStream toClientStream;
	private BufferedReader fromClientStream;
	private Socket clientSocket;

	public Server(int port, boolean isHTTPS) {
		this.isHTTPS = isHTTPS;
		this.serverPort = port;
	}

	public void run() {
		if(isHTTPS) {
			System.out.println("Starting up HTTPS Server.");
			startHTTPS(serverPort);
		}
		else {
			System.out.println("Starting up HTTP Server.");
			startHTTP(serverPort);
		}
	}

	/**
	 * Creates a socket + binds to the desired server-side port #.
	 *
	 * @throws {@link IOException} if the port is already in use.
	 
	public void bindSSL() throws IOException {
		SSLServerSocketFactory fact = SSLServerSocketFactory.getDefault();
		sslsocket = fact.createServerSocket(sslServerPort);
		System.out.println("SSL Server bound and listening to port " + sslServerPort);
	}
	
*/
	public void bind() throws IOException {
		socket = new ServerSocket(serverPort);
		System.out.println("Server bound and listening to port " + serverPort);
	}

	public boolean acceptSSLFromClient() throws IOException {
		//ServerSocket sslsock;
		try {
		KeyStore store = KeyStore.getInstance("JKS");
		store.load(new FileInputStream("server.jks"), "password".toCharArray());
		
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(store,"password".toCharArray());
		
		//TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		//tmf.init(store);
		
		SSLContext sslContext = SSLContext.getInstance("TLS");
		//TrustManager[] trustman = tmf.getTrustManagers();
		sslContext.init(kmf.getKeyManagers(),null, null);
		
		SSLServerSocketFactory sslsockfactory = sslContext.getServerSocketFactory();
		socket = (SSLServerSocket) sslsockfactory.createServerSocket(serverPort);
		
		((SSLServerSocket) socket).setNeedClientAuth(false);

		}catch (Exception e) {
			System.out.println("found exception " + e);
			return false;
		}
		return true;
	}
	
	public void acceptSSL() {
		try {
			clientSocket = socket.accept();
			toClientStream = new DataOutputStream(clientSocket.getOutputStream());
		
		fromClientStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

		}
		catch(Exception e) {
			System.out.println("found exception accepting from ssl client socket " + e);
		}
	}

	/**
	 * Waits for a client to connect, and then sets up stream objects for communication
 	 * in both directions.
	 *
	 * @return {@code true} if the connection is successfully established.
	 * @throws {@link IOException} if the server fails to accept the connection.
	 */
	public boolean acceptFromClient() throws IOException {
		Socket clientSocket;
		try {
			clientSocket = socket.accept();
		} catch (SecurityException e) {
			System.out.println("The security manager intervened; your config is very wrong. " + e);
			return false;
		} catch (IllegalArgumentException e) {
			System.out.println("Probably an invalid port number. " + e);
			return false;
		}

		toClientStream = new DataOutputStream(clientSocket.getOutputStream());
		fromClientStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		return true;
	}

	public String[] requestFileInfo(String pathToFile) {
		System.out.println(pathToFile);
		String[] returnArray = new String[5];
		File redirectFile = new File("www/redirect.defs");
		try {
			BufferedReader buff = new BufferedReader(new FileReader(redirectFile));
			String line = null;
		/*loop through all of the redirect paths listed in www/redirects.defs
		if we find a match we server the corresponding site
		if not we search the local files
		*/
			while((line = buff.readLine()) != null) {
				String delims = "[ ]";
				String [] lineWords =  line.split(delims);
				if(pathToFile.equals(lineWords[0])) {
					returnArray[0] = "301 Moved Permanently";
					returnArray[1] = "";
					returnArray[2] = "";
					returnArray[3] = "";
					returnArray[4] = lineWords[1];
					return returnArray;
				}
			}
		}
		catch(Exception e) {System.out.println("caught in the redirect " + e);}
		File requestedFile = new File("www/" + pathToFile);
		if(!requestedFile.exists()|| pathToFile.equals("/redirect.defs")) {
			//return 404
			returnArray[0] = "404 Not Found";
			returnArray[1] = "";
			returnArray[2] = "";
			returnArray[3] = "";
			return returnArray;
		}
		returnArray[0] = "200 OK";
		long unixTimeModified = requestedFile.lastModified();
		Date lastModifiedDate = new Date(unixTimeModified * 1000L);
		SimpleDateFormat dateformat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		dateformat.setTimeZone(TimeZone.getTimeZone("GMT"));
		returnArray[1] = dateformat.format(lastModifiedDate);
		returnArray[2] = "" + requestedFile.length();
		//parse for file extension switch to determine MIME type
		String extension = pathToFile.substring(pathToFile.lastIndexOf('.')+1);
		System.out.println(extension);
		switch(extension) {
			case "txt": returnArray[3] = "text/plain";
				break;
			case "html": returnArray[3] = "text/html";
				break;
			case "pdf": returnArray[3] = "application/pdf";
				break;
			case "png": returnArray[3] = "image/png";
				break;
			case "jpg": returnArray[3] = "image/jpg";
				break;
			default: returnArray[3] = "";
				break;
		}
		return returnArray;
	}
		
	public void writeFileBytes(String pathToFile) {
		System.out.println("we are calling the writefilebytes func");
		FileInputStream input = null;
		try {
			toClientStream.writeBytes("\r\n");
			input = new FileInputStream(pathToFile);
			int c;
			while((c = input.read()) != -1) {
				//System.out.println("this is a byte" + c);
				toClientStream.write(c);
			}
			toClientStream.writeBytes("\r\n");
		}
		catch(Exception e) {
			System.out.println("exception in writeFileBytes " + e); 
		}
	}
		

	public void parseRequest() throws IOException {
		
			String requestLine = fromClientStream.readLine();
			String delims = "[ ]";
			System.out.println(requestLine);
			String [] requestTokens = requestLine.split(delims);
			if (requestTokens[0].equals("HEAD") || requestTokens[0].equals("GET")) {
				String[] outputFileInfo = new String[4];
				outputFileInfo = requestFileInfo(requestTokens[1]);
				String [] requestOutput = new String[8];
				requestOutput[0] = requestTokens[2] + " " + outputFileInfo[0];
				requestOutput[1] = "Connection: close";
				
				DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
				Date dateobj = new Date();
				requestOutput[2] = df.format(dateobj);
				requestOutput[3] = "Server: Eric/Shuaib's Server!!!";
				requestOutput[4] = "Last-Modified: " + outputFileInfo[1];
				requestOutput[5] = "Content-Length: " + outputFileInfo[2];
				requestOutput[6] = "Content-Type: " + outputFileInfo[3];
				requestOutput[7] = "Location: " + outputFileInfo[4];

				for (String element: requestOutput) {
					System.out.println(element);
					toClientStream.writeBytes(element + "\r\n");
				}
				if(requestTokens[0].equals("GET") && outputFileInfo[0].equals("200 OK")) {
					System.out.println("we received a GET");
				writeFileBytes("www/" + requestTokens[1]);
				}
			}
			else {
				DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
				Date dateobj = new Date();
				String[] requestOutput = {"HTTP/1.1 403 Forbidden",df.format(dateobj),"Server: Eric/Shuaib's Server!!!","Last-Modified: ","Content-Length: ","Content-Type: "};
				for (String element: requestOutput) {
					toClientStream.writeBytes(element + "\r\n");
					System.out.println(element);
				}
			}
		}

	/**
	 * Loops forever, reading a line from the client, printing it to the screen,
	 * then echoing it back.
	 *
	 * @throws (@link IOException} if a communication error occurs.
	public void echoLoop() throws IOException {
		while (true) {
			String blob = fromClientStream.readLine();
			System.out.println(blob);
			toClientStream.writeBytes(blob + '\n');
		}	
	}	
*/

	private void startHTTP(int port) {
		try {
				while(true) {
					this.bind();
					if(this.acceptFromClient()) {
						this.parseRequest();
					} else {
						System.out.println("Error connecting on HTTP server.");
					}
					this.fromClientStream.close();
					this.socket.close();
				}
		} catch (IOException e) {
			System.out.println("Error communicating with HTTP client. Aborting. Details: " + e);
		}
	}

	private void startHTTPS(int port) {
		try {
			this.acceptSSLFromClient();
			while(true) {
				this.acceptSSL();
				this.parseRequest();
				this.clientSocket.close();
			}
		} catch (IOException e) {
			System.out.println("Error communicating with HTTPS client. aborting. Details: " + e);
		}
	}
				
	public static void main(String argv[]) {
		Map<String, String> flags = Utils.parseCmdlineFlags(argv);
		if (!flags.containsKey("--serverPort")) {
			System.out.println("usage: Server --serverPort=12345 --sslServerPort=12346");
			System.exit(-1);
		}
		if (!flags.containsKey("--sslServerPort")) {
			System.out.println("usage Server --serverPort=12345 --sslServerPort=12346");
			System.exit(-1);
		}

		int serverPort = -1;
		int sslServerPort = -1;
		try {
			serverPort = Integer.parseInt(flags.get("--serverPort"));
			sslServerPort = Integer.parseInt(flags.get("--sslServerPort"));
		} catch (NumberFormatException e) {
			System.out.println("Invalid port number! Must be an integer.");
			System.exit(-1);
		}
		//start HTTP and HTTPS servers on separate threads listening on their respective ports
		Server server = new Server(serverPort,false);
		Server sslServer = new Server(sslServerPort,true);
		new Thread(server).start();
		new Thread(sslServer).start();

		// Server server = new Server(serverPort,sslServerPort);
		// try {
		// 	while(true) {
		// 		//server.bind();
		// 		if(server.acceptSSLFromClient()) {
		// 			server.parseRequest();
		// 		}
		// 		else {
		// 			System.out.println("nope");
		// 		}
		// 		if (server.acceptFromClient()) {
		// 			server.parseRequest();
		// 		} else {
		// 			System.out.println("Error accepting client connection.");
		// 		}
		// 		//server.fromClientStream.close();
		// 		//server.sslsocket.close();
		// 	}
		// } catch (IOException e) {
		// 	System.out.println("Error communicating with client. aborting. Details: " + e);
		// }
	}
}

