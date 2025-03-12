package Assign32starter;

import java.awt.Dimension;

import org.json.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

/**
 * The ClientGui class is a GUI frontend that displays an image grid, an input text box,
 * a button, and a text area for status. 
 * 
 * Methods of Interest
 * ----------------------
 * show(boolean modal) - Shows the GUI frame with current state
 *     -> modal means that it opens GUI and suspends background processes. 
 * 		  Processing still happens in the GUI. If it is desired to continue processing in the 
 *        background, set modal to false.
 * newGame(int dimension) - Start a new game with a grid of dimension x dimension size
 * insertImage(String filename, int row, int col) - Inserts an image into the grid
 * appendOutput(String message) - Appends text to the output panel
 * submitClicked() - Button handler for the submit button in the output panel
 * 
 * Notes
 * -----------
 * > Does not show when created. show() must be called to show he GUI.
 * 
 */
public class ClientGui implements Assign32starter.OutputPanel.EventHandlers {
	JDialog frame;
	PicturePanel picPanel;
	OutputPanel outputPanel;
	String currentMess;

	Socket sock;
	OutputStream out;
	ObjectOutputStream os;
	ObjectInputStream in;
	BufferedReader bufferedReader;
	
	JSONObject request;
	JSONObject response;

	String host;
	int port;

	/**
	 * Construct dialog
	 * @throws IOException 
	 */
	public ClientGui(String host, int port) throws IOException, ClassNotFoundException {
		this.host = host; 
		this.port = port; 
	
		frame = new JDialog();
		frame.setLayout(new GridBagLayout());
		frame.setMinimumSize(new Dimension(500, 500));
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		// setup the top picture frame
		picPanel = new PicturePanel();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 0.25;
		frame.add(picPanel, c);

		// setup the input, button, and output area
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.75;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		outputPanel = new OutputPanel();
		outputPanel.addEventHandlers(this);
		frame.add(outputPanel, c);

		picPanel.newGame(1);
		
		open(); // opening server connection here
		in = new ObjectInputStream(sock.getInputStream());
		
		currentMess = "{'type': 'start'}"; // very initial start message for the connection
		try {
			os.writeObject(currentMess);
			System.out.println("[DEBUG] Send message");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String stringResponse = in.readObject().toString();
		System.out.println("[DEBUG] Got a response");
		request = new JSONObject(stringResponse);
		outputPanel.appendOutput(request.getString("value")); // putting the message in the outputpanel

		// reading out the image (abstracted here as just a string)
		System.out.println("[DEBUG] I got an image: " + request.getString("image"));
		// would put image in picture panel
		insertImage(request.getString("image"), 0, 0);
		
		close(); //closing the connection to server

		// Now Client interaction only happens when the submit button is used, see "submitClicked()" method
	}

	/**
	 * Shows the current state in the GUI
	 * @param makeModal - true to make a modal window, false disables modal behavior
	 */
	public void show(boolean makeModal) {
		frame.pack();
		frame.setModal(makeModal);
		frame.setVisible(true);
	}

	/**
	 * Creates a new game and set the size of the grid 
	 * @param dimension - the size of the grid will be dimension x dimension
	 * No changes should be needed here
	 */
	public void newGame(int dimension) {
		picPanel.newGame(1);
		outputPanel.appendOutput("Started new game with a " + dimension + "x" + dimension + " board.");
	}

	/**
	 * Insert an image into the grid at position (col, row)
	 * 
	 * @param filename - filename relative to the root directory
	 * @param row - the row to insert into
	 * @param col - the column to insert into
	 * @return true if successful, false if an invalid coordinate was provided
	 * @throws IOException An error occured with your image file
	 */
	public boolean insertImage(String filename, int row, int col) throws IOException {
		System.out.println("Image insert");
		String error = "";
		try {
			// insert the image
			if (picPanel.insertImage(filename, row, col)) {
				// put status in output
				outputPanel.appendOutput("Inserting " + filename + " in position (" + row + ", " + col + ")"); // you can of course remove this
				return true;
			}
			error = "File(\"" + filename + "\") not found.";
		} catch(PicturePanel.InvalidCoordinateException e) {
			// put error in output
			error = e.toString();
		}
		outputPanel.appendOutput(error);
		return false;
	}

	/**
	 * Submit button handling
	 *
	 * Right now this method opens and closes the connection after every interaction, if you want to keep that or not is up to you. 
	 */
	@Override
	public void submitClicked() {
		try {
			open(); // opening a server connection again
			in = new ObjectInputStream(sock.getInputStream());
			System.out.println("submit clicked");
		
			// Pulls the input box text
			String input = outputPanel.getInputText();
			
			outputPanel.setInputText("");
			System.out.println("[DEBUG] Input: " + input);
			System.out.println("[DEBUG] Quitting?: " + input.equals("quit"));
			
			System.out.println(request.toString());
			
			if(request.getString("type").equals("hello")) {
				System.out.println("Got name: " + input);
				
				response = new JSONObject();
				
				response.put("type", "name");
				response.put("name", input);
				
			}
			else if(request.getString("type").equals("menu")) {
				System.out.println("[DEBUG] Got selection: " + input);
				
				if(!(input.equals("start") || input.equals("leaderboard"))) {
					outputPanel.appendOutput("Invalid Selection, please try again");
					
					close();
					return;
				}
				
				response = new JSONObject();
				
				response.put("type", "selection");
				response.put("selection", input);
			}
			else if(request.getString("type").equals("rounds")) {
				int rounds;
				
				try {
					rounds = Integer.parseInt(input);
					if (rounds <= 0) {
						throw new NumberFormatException(); // Treat non-positive numbers as invalid
					}
					
					System.out.println("[DEBUG] Is an integer");
					
				}
				catch (NumberFormatException e) {
					System.out.println("[DEBUG] Not an integer");
					outputPanel.appendOutput("Rounds must be an integer greater than 0, please try again");
					
					close();
					return; // Input is not an integer
				}
				
				System.out.println("[DEBUG] Got number of rounds: " + input);
				
				response = new JSONObject();
				
				response.put("type", "rounds");
				response.put("rounds", rounds);
				
			}
			else if(input.equals("next")) {
				response.put("type", "next");
				
			}
			else if(input.equals("remaining")) {
				response.put("type", "remaining");
				
			}
			else if(input.equals("skip")) {
				response.put("type", "skip");
				
			}
			else if(request.getString("type").equals("playing")) {
				System.out.println("[DEBUG] Guessing");
				
				response = new JSONObject();
				
				response.put("type", "guess");
				response.put("guess", input);
				
				System.out.println("[DEBUG] Guessing: " + input);
			
			}
			else if(input.equals("quit")) {
				System.out.println("[DEBUG] Quiting");
				
				// Close everything
				close();
				
				frame.dispose();
				System.exit(0);
				return;
				
			}
			// send request to server
			try {
				os.writeObject(response.toString());
				os.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		
			// wait for an answer and handle accordingly
			try {
				System.out.println("[DEBUG] Waiting on response");
				String string = in.readObject().toString();
				System.out.println("[DEBUG] Server request: " + string);
				
				request = new JSONObject(string.trim());
				System.out.println("[DEBUG] Received response");
				
				if(request.has("value")) {
					outputPanel.appendOutput(request.getString("value"));
					
				}
				if(request.has("total points")) {
					outputPanel.setPoints(request.getInt("total points"));
					
				}
				if(request.has("image")) {
					String image = request.getString("image");
					
					insertImage(image, 0, 0);
					
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Key listener for the input text box
	 * 
	 * Change the behavior to whatever you need
	 */
	@Override
	public void inputUpdated(String input) {
		if (input.equals("surprise")) {
			outputPanel.appendOutput("You found me!");
		}
	}

	public void open() throws UnknownHostException, IOException {
		this.sock = new Socket(host, port); // connect to host and socket

		// get output channel
		this.out = sock.getOutputStream();
		// create an object output writer (Java only)
		this.os = new ObjectOutputStream(out);
		this.bufferedReader = new BufferedReader(new InputStreamReader(sock.getInputStream()));

	}
	
	public void close() {
        try {
            if (out != null)  out.close();
            if (bufferedReader != null)   bufferedReader.close(); 
            if (sock != null) sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	public static void main(String[] args) throws IOException {
		// create the frame
		
		try {
			String host = args[0];
			int port = Integer.parseInt(args[1]);


			ClientGui main = new ClientGui(host, port);
			main.show(true);


		} catch (Exception e) {e.printStackTrace();}



	}
}
