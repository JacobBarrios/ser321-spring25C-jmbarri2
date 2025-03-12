# Assignment 3 Starter Code

## Grid Image Maker Usage

### Terminal

```
gradle runServer -Pport=8888
```

```
gradle runClient -Phost=localhost -Pport=8888
```

## Project Description
### 1. Project Description
My project is a game where you have to guess what wonder of the world it is based on the
given pictures.
### 2. Checklist
- [ ] Leaderboard
- [ ] Quit safely
- [ ] Save name and points in the leaderboard (as long as the server is up)
- [ ] Utilize "next," "skip," and "remaining" commands
- [ ] Randomly select the first hint picture from a predefined set
- [ ] Display the next picture upon "next" command
- [ ] Track total points for each round
- [ ] Reduce earned points when additional hints are used
### 3. Protocol Descriptions
#### Requests
| Type        | Description                                                                          |
|-------------|--------------------------------------------------------------------------------------|
| `start`     | Initiates the game. The server responds with a welcome message and an image.         |
| `name`      | Sends the player's name to the server.                                               |
| `selection` | Indicates whether the player selects "start", "quit or "leaderboard".                |
| `rounds`    | Specifies the number of rounds the player wants to play. Must be a positive integer. |
| `guess`     | Submits a guessed answer for the current image.                                      |
| `next`      | Requests the next image in the current round.                                        |
| `remaining` | Asks how many hints are left.                                                        |
| `skip`      | Skips the current image and moves to the next, reducing potential points.            |
| `quit`      | Ends the game session.                                                               |

#### Responses
| Field         | Description |
|--------------|------------|
| `value`      | A message or prompt for the player. |
| `image`      | A new image for the current round. |
| `total points` | Updated score after each guess or action. |
| `error`      | If something goes wrong (e.g., invalid input, server issues), an error message is returned. |

### 4. Video Demo
### 5. Robustness Features
The game client is designed with the following robustness features:

- **Proper Input Validation:**
    - Ensures that the number of rounds is a positive integer.
    - Prevents invalid selections when choosing game options.

- **Graceful Handling of Server Errors:**
    - If the server sends an error response, the client displays a user-friendly message instead of crashing.

- **Safe Socket Handling:**
    - The client ensures sockets are closed properly when exiting to prevent resource leaks.

- **Predefined Protocol Structure:**
    - Uses JSON-based communication to ensure consistency between the client and server.

- **No Hardcoded Assumptions:**
    - The client does not assume the order of responses but instead checks the `"type"` field to determine how to process the data.

- **Connection Management:**
    - Opens a new connection for each request-response cycle to avoid keeping unused sockets open.
    - A possible improvement would be keeping the connection open for the session to reduce overhead.

### 6. UDP Considerations
If the game used UDP instead of TCP, the following changes would be necessary:

- **No Guaranteed Delivery:**
    - UDP does not guarantee that packets arrive in order or at all. Acknowledgment messages or sequence numbers would be needed.

- **No Built-in Connection Management:**
    - Since UDP is connectionless, the client and server would need to track game state manually.

- **Potential for Duplicated or Lost Packets:**
    - The game logic must handle duplicate responses or missing packets gracefully.

- **Lower Latency but Less Reliability:**
    - UDP could reduce communication delays but may lead to inconsistencies in the game state.

To ensure reliable gameplay, implementing a simple retransmission mechanism or switching to a protocol like QUIC could be beneficial.

## GUI Usage

### Code

1. Create an instance of the GUI

   ```
   ClientGui main = new ClientGui();
   ```

2. Create a new game and give it a grid dimension

   ```
   // the pineapple example is 2, but choose whatever dimension of grid you want
   // you can change the dimension to see how the grid changes size
   main.newGame(2); 
   ```

*Depending on how you want to run the system, 3 and 4 can be run how you want*

3. Insert image

   ```
   // the filename is the path to an image
   // the first coordinate(0) is the row to insert in to
   // the second coordinate(1) is the column to insert in to
   // you can change coordinates to see the image move around the box
   main.insertImage("img/Pineapple-Upside-down-cake_0_1.jpg", 0, 1);
   ```

4. Show GUI

   ```
   // true makes the dialog modal meaning that all interaction allowed is 
   //   in the windows methods.
   // false makes the dialog a pop-up which allows the background program 
   //   that spawned it to continue and process in the background.
   main.show(true);
   ```

For the images: The numbering is alwas starting at 1 which is the "main" view, increasing numbers are always turning to the right. So 2 is a 90 degree right turn of 1, while 4 is a 90 degree left turn of 1. 

### ClientGui.java
#### Summary

> This is the main GUI to display the picture grid. 

#### Methods
  - show(boolean modal) :  Shows the GUI frame with the current state
     * NOTE: modal means that it opens the GUI and suspends background processes. Processing still happens in the GUI If it is desired to continue processing in the background, set modal to false.
   * newGame(int dimension) :  Start a new game with a grid of dimension x dimension size
   * insertImage(String filename, int row, int col) :  Inserts an image into the grid, this is when you know the file name, use the PicturePanel insertImage if you have a ByteStream
   * appendOutput(String message) :  Appends text to the output panel
   * submitClicked() :  Button handler for the submit button in the output panel

### PicturePanel.java

#### Summary

> This is the image grid

#### Methods

- newGame(int dimension) :  Reset the board and set grid size to dimension x dimension
- insertImage(String fname, int row, int col) :  Insert an image at (col, row)
- insertImage(ByteArrayInputStream fname, int row, int col) :  Insert an image at (col, row)

### OutputPanel.java

#### Summary

> This is the input box, submit button, and output text area panel

#### Methods

- getInputText() :  Get the input text box text
- setInputText(String newText) :  Set the input text box text
- addEventHandlers(EventHandlers handlerObj) :  Add event listeners
- appendOutput(String message) :  Add message to output text

