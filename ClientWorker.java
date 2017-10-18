import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.lang.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientWorker implements Runnable {

	private Socket client;
	public static String clientName;

	private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
	public final static int SOCKET_PORT = 6664;
	static String ServerFileLocation = "*Path*/ServerMusic";
	static String sync = "";
	static FileInputStream fis = null;
	static BufferedInputStream bis = null;
	static ServerSocket servsock = null;
	static Socket sock = null;
	static DataOutputStream out = null;
	static DataInputStream in = null;
	public static Scanner se = null;
	public static String fileName = "*Path*\\Log.txt";
	public static String line = null;

	public ClientWorker(Socket client) {
		this.client = client;
	}
	// List function to get the list of server files

	public static synchronized List list(String directoryName) throws NoSuchAlgorithmException, IOException {
		File directory = new File(directoryName);
		File[] fList = directory.listFiles();

		List<String> serverList = new ArrayList<String>();

		for (File file : fList) {
			if (file.isFile()) {
				serverList.add(file.getName() + "/@/" + calSHA(file));

				// add hashed value for each file and concatenate it to the list
			}
		}
		return serverList;
	}

	public static synchronized String calSHA(File filename) throws IOException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(Files.readAllBytes(filename.toPath()));
		byte[] finalized = md.digest();
		String toSend = "";
		for (byte b : finalized)
			toSend += String.format("%02x", b);
		return toSend;
		// out.writeUTF(toSend);
		// out.flush();
	}

	private static synchronized void pull(List<String> diffList) throws IOException {
		// TODO Auto-generated method stub
		int i = 0;
		String serverFileName;
		long fileSize;
		try {
			while (i < diffList.size()) {

				// System.out.println(diffList.get(i));
				serverFileName = ServerFileLocation + "/" + (String) diffList.get(i);
				// ServerFileLocation += "/" + serverFileName;

				// send file
				File myFile = new File(serverFileName);
				byte[] mybytearray = new byte[(int) myFile.length()];

				try {
					if (out != null && myFile.exists() && myFile.isFile()) {
						FileInputStream input = new FileInputStream(myFile);
						long len = myFile.length();
						out.writeLong(len);
						System.out.println("\n o(^_-)O  == Syncing files ==  O(-_^)o  \n");
						System.out.println(" \n Sending file: " + diffList.get(i) + "\nFile Size in bytes: " + len);
						// System.out.println(myFile.getAbsolutePath());
						int read = 0;
						while ((read = input.read()) != -1)
							out.writeByte(read);
						out.flush();
						input.close();
						System.out.println(" \n File successfully sent!");
					}
					i = i + 1;
					/*
					 * fis = new FileInputStream(myFile);
					 * 
					 * bis = new BufferedInputStream(fis); bis.read(mybytearray,
					 * 0, mybytearray.length); // os = sock.getOutputStream();
					 * 
					 * System.out.println("Sending " + (String) diffList.get(i)
					 * + "(" + mybytearray.length + " bytes)");
					 * out.write(mybytearray, 0, mybytearray.length); i = i + 1;
					 */

					// out.flush();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} finally {
			leave();
			System.out.println(" \n Connection Closed!");

		}
	}

	public static synchronized void leave() throws IOException {
		// System.out.println("Closing the connections ");
		if (fis != null)
			fis.close();
		if (bis != null)
			bis.close();
		if (out != null)
			out.close();
		if (sock != null)
			sock.close();
		if (servsock != null)
			servsock.close();
		// System.exit(1);
	}

	@Override
	public synchronized void run() {
//		System.out.println("Enter 'Log' View log file\n or");
//		System.out.println("Enter and key to connect to the client \n");
//		se = new Scanner(System.in);
//		String input = se.nextLine();
		try {
//			if (!input.equalsIgnoreCase("Log")) {

				in = new DataInputStream(client.getInputStream());
				out = new DataOutputStream(client.getOutputStream());
				clientName = in.readUTF();

				// writing to a file to maintain log
				LocalDateTime now = LocalDateTime.now();
				System.out.println(dtf.format(now));

				FileWriter fileWriter = new FileWriter(fileName, true);
				BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
				bufferedWriter.newLine();
				bufferedWriter.write(dtf.format(now) + " :: " + clientName + " connected to Server");
				bufferedWriter.newLine();
				bufferedWriter.write(dtf.format(now) + " :: At Server IP :: " + client.getInetAddress().toString());
				bufferedWriter.newLine();

				System.out.println(" \nConnection Accepted  \nConnected to Client at IP: " + client.getInetAddress());
				System.out.println(" \nClient Name: " + clientName);
				if (!clientName.equalsIgnoreCase("No")) {

					List serverList = list(ServerFileLocation);
					System.out.println(" \nList of Files on the Server: ");
					for (int j = 0; j < serverList.size(); j++) {

						System.out.println(((String) serverList.get(j)).split("/@/")[0]);

						out.writeUTF((String) serverList.get(j));
						// out.writeUTF(" ");
					}
					out.writeUTF("EOS");
					String inString;
					List<String> diffList = new ArrayList<String>();
					while (true) {
						inString = in.readUTF();
						if (inString.equalsIgnoreCase("EOS")) {
							break;
						} else {
							// System.out.println(inString);
							diffList.add(inString);
						}
					}

					// System.out.println("The difference list- \nFiles on
					// Server
					// and not on Client: ");
					// System.out.println(diffList);
					pull(diffList);
					diffList.remove(0);
					bufferedWriter.write(dtf.format(now) + " :: " + "Requested difflist files" + diffList.toString());
					bufferedWriter.newLine();
					// this.notify();
					// String serverDir =
					// "C:/Users/Mounish/Documents/books/ComputerNetworks/ServerMusic";
					// List serverList = list(ServerFileLocation);
					bufferedWriter.close();
					fileWriter.close();
					this.notifyAll();

				}

				System.out.println(" \nDone.");

//			} else {
//				FileReader fileReader = new FileReader(fileName);
//				BufferedReader bufferedReader = new BufferedReader(fileReader);
//				while ((line = bufferedReader.readLine()) != null) {
//					System.out.println(line);
//				}
//				bufferedReader.close();
//			}
		}  catch (SocketException e){
			System.out.println("Connection interupted by peer!");
		}
		catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} finally {
			try {

				leave();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}