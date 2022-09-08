package acsse.csc2b.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Scanner;
import java.util.StringTokenizer;


public class Server extends Thread{
	private int portNumber =  46829;
	private ServerSocket connection = null;
	private Socket socket= null;
	private DataInputStream din = null;
	private DataOutputStream dos = null;
	private Scanner scanner = null;
	
	public Server()
	{
		
	}
	
	public Server(int portNumber)
	{
		this.portNumber = portNumber;
	}
	
	@Override
	public void run()
	{
		try {
			connection = new ServerSocket(portNumber);

			while(true)
			{
				socket = null;
				System.out.println("ready for incomming connections on port: "+ portNumber);
				socket = connection.accept();
				din = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
				dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
				scanner = new Scanner(new BufferedInputStream(socket.getInputStream()));
				
				// get 1 of three operations to be conducted 
				String operation = din.readUTF();
				
				// switch between the 3 operation
				switch(operation)
				{
				case"POSTIMG": // adding image to the server from a client
				{
					addImage(operation);
					
					// when adding image is successful we return an ok to client
					PrintWriter w = new PrintWriter(new BufferedOutputStream(socket.getOutputStream()),true);
					w.println("Ok");
					w.flush();
					break;
				}
				case"GETIMG": // sending image to client
				{
					
					String ID = din.readUTF();
					
					//open image list to check if requested image is available
					BufferedReader read = new BufferedReader(new FileReader("data/server/Image-List.txt")); 
					
					String readInput = null;
					String filename = null;
					
					while((readInput = read.readLine())!=null)
					{
						// if we get file on the list
						if(readInput.startsWith(String.valueOf(ID)))
						{
							StringTokenizer token = new StringTokenizer(readInput);
							@SuppressWarnings("unused")
							String number = token.nextToken();
							filename = token.nextToken();
							dos.writeUTF(filename); // send filename to client
							break;
						}
					}
					
					
					if(filename != null)
					{
						//create a file and send if if the filename is not null
						File file = new File("data/server",filename);
						
						FileInputStream fis = new FileInputStream(file);
							
						dos.writeLong(file.length());
						while(fis.available()>0)
						{
							dos.write(fis.readAllBytes());
						}
						
						fis.close();
						read.close();
						System.out.println("file: "+filename +" sent"); // confirm file is sent on server side
					}
					else
					{
						// if file ID is not available
						dos.writeUTF("File Not Found");  // notify client that image is not available
						dos.flush();
						System.err.println("File Request not found");
					}
					break;
				}
				case"IMGLST": // send the image list to client
				{
					//get list file from files
					File file = new File("data/server","Image-List.txt");
					
					dos.writeUTF("Image-List.txt");
					dos.flush();
					
					FileInputStream fis = new FileInputStream(file);
						
					dos.writeLong(file.length());
					dos.flush();
					while(fis.available()>0)
					{
						dos.write(fis.readAllBytes());
					}
					
					dos.flush();
					fis.close();
					System.out.println("List sent");
					

					break;
				}
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try {
				if(socket!=null)
				{
					socket.close();
				}
				if(din!=null)
				{
					din.close();
				}
				if(dos!=null)
				{
					dos.close();
				}
				if(scanner!=null)
				{
					scanner.close();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @param operation to be conducted which in this case its adding image to database
	 */
	public void addImage(String operation)
	{
		try {
			// create a  file using filename supplied by client
			String filename = din.readUTF();
			System.out.println("Image Added to server:"+filename);
			FileOutputStream fos = new FileOutputStream(new File("data/server",filename));
			
			// read in the image from the connection
			long fileSize = din.readLong();
			byte[] buffer = new byte[1024];
			int n=0;
			int totalbytes = 0;
			while(totalbytes!=fileSize)
			{
				n= din.read(buffer,0, buffer.length);
				fos.write(buffer,0,n);
				fos.flush();
				totalbytes+=n;
			}	
			
			// read number of lines in out list to get the ID of the newly added image
			BufferedReader read = new BufferedReader(new FileReader("data/server/Image-List.txt"));
			
			int lines = 0;
			@SuppressWarnings("unused")
			String file = "";
			
			while((file = read.readLine())!=null)
			{
				lines++;
			}
			read.close();
			
			lines++; // ID of new image 
			
			// append image list with the ID and image filename 
			Files.write(Paths.get("data/server/Image-List.txt"),("\n"+lines+ " "+filename).getBytes(), StandardOpenOption.APPEND);
		}
		catch(IOException e)
		{
			System.err.println("could not add image to server");
			try {
				dos.writeUTF("NOTOK");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
}
