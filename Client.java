import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public final class Client {
	private static final int TIMEOUT_MS = 5000; // 5 seconds

	private final int serverPort;
	private final String serverIP;
	private final Socket socket;
	private DataOutputStream toServerStream;
	private BufferedReader fromServerStream;

	public Client(int serverPort, String serverIP) {
		this.serverPort = serverPort;
		this.serverIP = serverIP;
		this.socket = new Socket();
	}

	/**
	 * Tries to connect to the server on the provided IP and port #, then (if successful)
	 * create stream objects for reading and writing in each direction.
	 * 
	 * @return {@code true} if the connect succeeded, {@code false} otherwise.
	 * @throws {@link IOException} if the connect operation fails in a way not caught in
	 *         the {@code catch} statements below.
	 */
	public boolean connectToServer() throws IOException {
		InetSocketAddress sockAddr;
		try {
			sockAddr = new InetSocketAddress(serverIP, serverPort);
			socket.connect(sockAddr, TIMEOUT_MS);
		} catch (UnknownHostException e) {
			System.out.println("Could not find host: " + e);
			return false;
		} catch (SecurityException e) {
			System.out.println("The security manager intervened; your config is very wrong. " + e);
			return false;
		} catch (IllegalArgumentException e) {
			System.out.println("Probably an invalid port number. " + e);
			return false;
		}

		toServerStream = new DataOutputStream(socket.getOutputStream());
		fromServerStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		System.out.println("Connection established!");
		return true;
	}

	/**
	 * Loops, reading a line from standard input, sending it to the server, then waiting for
   * the server to send back a response (which it also prints to the screen.
	 * Loops forever until the user hits Ctrl-C.
	 * 
	 * @throws {@link IOException} if there is a communication error.
	 */
	public void echoLoop() throws IOException {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String blob = stdin.readLine();
			toServerStream.writeBytes(blob + '\n');
			String echo_blob = fromServerStream.readLine();
			System.out.println("ECHO: " + echo_blob);
		}	
	}	
				
	public static void main(String argv[]) {
		Map<String, String> flags = Utils.parseCmdlineFlags(argv);
		if (!flags.containsKey("--serverPort") || !flags.containsKey("--serverIP")) {
			System.out.println("usage: Client --serverPort=12345 --serverIP=1.2.3.4");
			System.exit(-1);
		}

		String serverIP = flags.get("--serverIP");
		int serverPort = -1;
		try {
			serverPort = Integer.parseInt(flags.get("--serverPort"));
		} catch (NumberFormatException e) {
			System.out.println("Invalid port number! Must be an integer.");
			System.exit(-1);
		}

		Client client = new Client(serverPort, serverIP);
		try {
			client.connectToServer();
			client.echoLoop();
		} catch (IOException e) {
			System.out.println("Error communicating with server. aborting. Details: " + e);
		}
	}
}

