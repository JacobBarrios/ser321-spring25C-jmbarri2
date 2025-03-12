package Assign32starter;
import java.net.*;
import java.util.Base64;
import java.util.Set;
import java.util.Stack;
import java.util.*;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import java.awt.image.BufferedImage;
import java.io.*;
import org.json.*;


/**
 * A class to demonstrate a simple client-server connection using sockets.
 * Ser321 Foundations of Distributed Software Systems
 */
public class SockServer {
	static Stack<String> imageSource = new Stack<String>();
	static int port;
	static int rounds;
	static int hints;
	static String file;
	static int totalPoints;
	static String correctAnswer;
	static JSONArray leaderboard;
	
	public static void main (String args[]) {
		Socket sock;
		try {
			port = Integer.parseInt(args[0]);
			
			//opening the socket here, just hard coded since this is just a bas example
			ServerSocket serv = new ServerSocket(port);
			System.out.println("Server ready for connection");

			// placeholder for the person who wants to play a game
			String name = "";
			int points = 0;

			// read in one object, the message. we know a string was written only by knowing what the client sent.
			// must cast the object from Object to desired type to be useful
			while(true) {
				sock = serv.accept(); // blocking wait

				// could totally use other input output streams here
				ObjectInputStream in = new ObjectInputStream(sock.getInputStream());
				ObjectOutputStream out = new ObjectOutputStream(sock.getOutputStream());
				
				String s = (String) in.readObject();
				JSONObject request = new JSONObject(s); // the requests that is received
				System.out.println("[DEBUG] Request received");
				
				JSONObject response = new JSONObject();
				
				if (request.getString("type").equals("start")){
					hints = 1;
					System.out.println("[DEBUG] got a start");
				
					response.put("type","hello");
					response.put("value","Hello, please tell me your name.");
					response = sendImg("img/hi.png", response); // calling a method that will manipulate the image and will make it send ready
					
				}
				else if(request.getString("type").equals("name")) {
					name = request.getString("name");
					
					response.put("type", "menu");
					response.put("value", "Hello, " + name + "!\n" +
							"What would you like to do: start, leaderboard, or quit");
					
					System.out.println("[DEBUG] Sent menu");
					
				}
				else if(request.getString("type").equals("selection")) {
					String selection = request.getString("selection");
					
					boolean selecting = true;
					while(selecting) {
						switch(selection) {
							case "start":
								response.put("type", "rounds");
								response.put("value", "Please enter the number of rounds the game should last");
								
								selecting = false;
								break;
								
							case "leaderboard":
								response.put("type", "rounds");
								response.put("value", "");
								
								selecting = false;
								break;
								
							default:
								selecting = true;
								
							
						}
						
					}
					
					
				}
				else if(request.getString("type").equals("rounds")) {
					rounds = request.getInt("rounds");
					System.out.println("[DEBUG] Number of rounds: " + rounds);
					
					response.put("type","playing");
					response.put("value", "Starting game for " + rounds + " rounds");
					
					file = getRandomFile();
					correctAnswer = file.substring(0, file.lastIndexOf('.')); // Remove file extension
					correctAnswer = correctAnswer.replaceAll("\\s+", "").toLowerCase(); // Remove spaces and convert to lowercase
					correctAnswer = correctAnswer.replaceAll("\\d+$", ""); // Remove trailing digits (number suffix)
					
					System.out.println("[DEBUG] Correct answer: " + correctAnswer);
					System.out.println("[DEBUG] Chosen file: " + file);
					String filePath = "img/" + file;
					
					response = sendImg(filePath, response);
					
				}
				else if(request.getString("type").equals("guess")) {
					String guess = request.getString("guess");
					
					// Strip any spaces from the guess and the actual file name
					guess = guess.replaceAll("\\s+", "").toLowerCase(); // Remove spaces and convert to lowercase
					
					// Compare the guess to the actual file name
					if (guess.equals(correctAnswer)) {
						--rounds;
						totalPoints += hints * 5 + 5;
						
						response.put("type", "playing");
						response.put("value", "Correct!");
						response.put("total points", totalPoints);
						
						file = getRandomFile();
						correctAnswer = file.substring(0, file.lastIndexOf('.')); // Remove file extension
						correctAnswer = correctAnswer.replaceAll("\\s+", "").toLowerCase(); // Remove spaces and convert to lowercase
						correctAnswer = correctAnswer.replaceAll("\\d+$", ""); // Remove trailing digits (number suffix)
						
						System.out.println("[DEBUG] Chosen file: " + file);
						String filePath = "img/" + file;
						
						response = sendImg(filePath, response);
					}
					else {
						response.put("type", "playing");
						response.put("value", "Incorrect, Please try again!");
					}
				}
				else if(request.getString("type").equals("next")) {
					hints++;
					// Construct the next image filename based on the hint number
					String nextFileName = file + hints + ".png";
					
					// Create the full file path for the next image
					String nextFilePath = "img/" + nextFileName;
					
					// Check if the next image exists
					File nextImage = new File(nextFilePath);
					if (nextImage.exists()) {
						// Set the next image as the file to send
						file = nextFileName; // Update the file variable with the next image name
						response.put("type", "playing");
						response.put("value", "Hint number " + hints);
						response = sendImg(nextFilePath, response); // Send the next image
						
					}
					else {
						hints = 1;
						rounds -= 1;
						
						response.put("type", "playing");
						
						file = getRandomFile();
						correctAnswer = file.substring(0, file.lastIndexOf('.')); // Remove file extension
						correctAnswer = correctAnswer.replaceAll("\\s+", "").toLowerCase(); // Remove spaces and convert to lowercase
						correctAnswer = correctAnswer.replaceAll("\\d+$", ""); // Remove trailing digits (number suffix)
						
						System.out.println("[DEBUG] Chosen file: " + file);
						String filePath = "img/" + file;
						
						response = sendImg(filePath, response);
					}
				}
				else if(request.getString("type").equals("skip")) {
					hints = 1;
					--rounds;
					
					response.put("type","playing");
					response.put("value", "Starting game for " + rounds + " rounds");
					
					file = getRandomFile();
					correctAnswer = file.substring(0, file.lastIndexOf('.')); // Remove file extension
					correctAnswer = correctAnswer.replaceAll("\\s+", "").toLowerCase(); // Remove spaces and convert to lowercase
					correctAnswer = correctAnswer.replaceAll("\\d+$", ""); // Remove trailing digits (number suffix)
					
					System.out.println("[DEBUG] Chosen file: " + file);
					String filePath = "img/" + file;
					
					response = sendImg(filePath, response);
					
				}
				else if(request.getString("type").equals("remaining")) {
					if(hints != 4) {
						response.put("type", "playing");
						response.put("value", 4 - hints + " hint(s) remaining");
					}
					else {
						response.put("type", "playing");
						response.put("value", "No hints remaining!");
					}
				}
				else {
					System.out.println("not sure what you meant");
					response.put("type","error");
					response.put("message","unknown response");
				}
				
				// Send response
				out.writeObject(response.toString());
				out.flush();
				System.out.println("[DEBUG] Sent message");
				
				if(rounds == 0) {
					JSONObject player = new JSONObject();
					
					player.put("name", name);
					player.put("total points", totalPoints);
					leaderboard.put(player);
					
					response.put("type", "menu");
					response.put("value", "Game completed with " + totalPoints+ " total points!\n" +
							"What would you like to do: start, leaderboard, or quit");
					
					System.out.println("[DEBUG] Sent menu");
				}
				
				System.out.println("Total points: " + totalPoints);
			}
			
		} catch(Exception e) {e.printStackTrace();}
	}
	
	//TODO write description for method
	public static String getRandomFile() {
		String folderPath = "img/"; // Change this to your folder path
		File folder = new File(folderPath);
		
		// Get list of image files
		File[] files = folder.listFiles((dir, fileName) -> (fileName.endsWith("1.png")));
		
		if (files == null || files.length == 0) {
			System.out.println("No images found in the folder.");
			return null;
		}
		
		// Select a random file
		Random random = new Random();
		File randomImage = files[random.nextInt(files.length)];
		
		System.out.println("[DEBUG] Selected Image: " + randomImage.getName());
		
		return randomImage.getName();
		
	}

	public static JSONObject sendImg(String filename, JSONObject obj) throws Exception {
		File file = new File(filename);
		System.out.println("[DEBUG] send image");
		if (file.exists()) {
			// import image
			// I did not use the Advanced Custom protocol
			// I read in the image and translated it into basically into a string and send it back to the client where I then decoded again
			obj.put("image", filename);
		}
		return obj;
	}
}
