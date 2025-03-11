import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.io.*;

/**
 * A class to demonstrate a simple client-server connection using sockets.
 *
 */
public class SockServer {
  static Socket sock;
  static DataOutputStream os;
  static ObjectInputStream in;

  static int port = 8888;
  
  static JSONArray inventory;

  public static void main (String args[]) {
    inventory = new JSONArray();
    
    if (args.length != 1) {
      System.out.println("Expected arguments: <port(int)>");
      System.exit(1);
    }

    try {
      port = Integer.parseInt(args[0]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port|sleepDelay] must be an integer");
      System.exit(2);
    }

    try {
      //open socket
      ServerSocket serv = new ServerSocket(port);
      System.out.println("Server ready for connections");

      /**
       * Simple loop accepting one client and calling handling one request.
       *
       */


      while (true){
        System.out.println("Server waiting for a connection");
        sock = serv.accept(); // blocking wait
        System.out.println("Client connected");

        // setup the object reading channel
        in = new ObjectInputStream(sock.getInputStream());

        // get output channel
        OutputStream out = sock.getOutputStream();

        // create an object output writer (Java only)
        os = new DataOutputStream(out);

        boolean connected = true;
        while (connected) {
          String s = "";
          try {
            s = (String) in.readObject(); // attempt to read string in from client
          } catch (Exception e) { // catch rough disconnect
            System.out.println("Client disconnect");
            connected = false;
            continue;
          }

          JSONObject res = isValid(s);

          if (res.has("ok")) {
            writeOut(res);
            continue;
          }

          JSONObject req = new JSONObject(s);

          res = testField(req, "type");
          if (!res.getBoolean("ok")) { // no "type" header provided
            res = noType(req);
            writeOut(res);
            continue;
          }
          // check which request it is (could also be a switch statement)
          if (req.getString("type").equals("echo")) {
            res = echo(req);
          } else if (req.getString("type").equals("add")) {
            res = add(req);
          } else if (req.getString("type").equals("addmany")) {
            res = addmany(req);
          }
          else if(req.getString("type").equals("charcount")) {
            res = charCount(req);
            
          }
          else if(req.getString("type").equals("inventory")) {
            res = inventory(req);
          
          }
          else {
            res = wrongType(req);
          }
          writeOut(res);
        }
        // if we are here - client has disconnected so close connection to socket
        overandout();
      }
    } catch(Exception e) {
      e.printStackTrace();
      overandout(); // close connection to socket upon error
    }
  }


  /**
   * Checks if a specific field exists
   *
   */
  static JSONObject testField(JSONObject req, String key){
    JSONObject res = new JSONObject();

    // field does not exist
    if (!req.has(key)){
      res.put("ok", false);
      res.put("message", "Field " + key + " does not exist in request");
      return res;
    }
    return res.put("ok", true);
  }

  // handles the simple echo request
  static JSONObject echo(JSONObject req){
    System.out.println("Echo request: " + req.toString());
    JSONObject res = testField(req, "data");
    if (res.getBoolean("ok")) {
      if (!req.get("data").getClass().getName().equals("java.lang.String")){
        res.put("ok", false);
        res.put("message", "Field data needs to be of type: String");
        return res;
      }

      res.put("type", "echo");
      res.put("echo", "Here is your echo: " + req.getString("data"));
    }
    return res;
  }

  // handles the simple add request with two numbers
  static JSONObject add(JSONObject req){
    System.out.println("Add request: " + req.toString());
    JSONObject res1 = testField(req, "num1");
    if (!res1.getBoolean("ok")) {
      return res1;
    }

    JSONObject res2 = testField(req, "num2");
    if (!res2.getBoolean("ok")) {
      return res2;
    }

    JSONObject res = new JSONObject();
    res.put("ok", true);
    res.put("type", "add");
    try {
      res.put("result", req.getInt("num1") + req.getInt("num2"));
    } catch (org.json.JSONException e){
      res.put("ok", false);
      res.put("message", "Field num1/num2 needs to be of type: int");
    }
    return res;
  }

  // implement me in assignment 3
  static JSONObject inventory(JSONObject req) {
    System.out.println("inventory request: " + req.toString());
    JSONObject res = testField(req, "task");
    res.put("type", "inventory");
    
    if(!res.getBoolean("ok")) {
      return res;
    }
    
    String task = req.getString("task");
    
    if(task.equals("view")){
      String inventoryList = buildList();
      res.put("inventory", inventoryList);
      
      return res;
      
    }
    else if(task.equals("add")){
      if(!(req.get("productName") instanceof String)) {
        res.put("ok", false);
        res.put("message", "Field product name needs to be of type: String");
        
        return res;
        
      }
      
      if(!(req.get("quantity") instanceof Integer)) {
        res.put("ok", false);
        res.put("message", "Field quantity needs to be of type: int");
        
        return res;
        
      }
      
      if(inventory.length() == 0) {
        JSONObject product = new JSONObject();
        product.put("Name", req.getString("productName"));
        product.put("Quantity", req.getInt("quantity"));
        
        inventory.put(product);
        
      }
      else {
        boolean found = false;
        String newProductName = req.getString("productName");
        int newQuantity = req.getInt("quantity");
        
        for(int i = 0; i < inventory.length(); i++) {
          JSONObject product = inventory.getJSONObject(i);
          
          if(product.getString("Name").equals(newProductName)) {
            int totalQuantity = product.getInt("Quantity") + newQuantity;
            
            inventory.getJSONObject(i).put("Quantity", totalQuantity);
            
            found = true;
            
            break;
            
          }
        }
        if (!found) {
          JSONObject newProduct = new JSONObject();
          newProduct.put("Name", newProductName);
          newProduct.put("Quantity", newQuantity);
          inventory.put(newProduct);
          
        }
      }
    }
    else if(task.equals("buy")) {
      if(!(req.get("productName") instanceof String)) {
        res.put("ok", false);
        res.put("message", "Field product name needs to be of type: String");
        
        return res;
        
      }
      
      if(!(req.get("quantity") instanceof Integer)) {
        res.put("ok", false);
        res.put("message", "Field quantity needs to be of type: int");
        
        return res;
        
      }
      
      boolean found = false;
      String buyProduct = req.getString("productName");
      int takeAway = req.getInt("quantity");
      
      for(int i = 0; i < inventory.length(); i++) {
        JSONObject product = inventory.getJSONObject(i);
        
        if(product.getString("Name").equals(buyProduct)) {
          found = true;
          
          if(product.getInt("Quantity") >= takeAway) {
            int totalQuantity = product.getInt("Quantity") - takeAway;
            inventory.getJSONObject(i).put("Quantity", totalQuantity);
            
            break;

          }
          else {
            res.put("ok", false);
            res.put("message", "Product " + buyProduct + " not available in quantity " + takeAway);
            
            return res;
            
          }
          
        }
      }
      if(!found) {
        res.put("ok", false);
        res.put("message", "Product " + buyProduct + " not in inventory");
        
        return res;
        
      }
    }
    
    String inventoryList = buildList();
    res.put("inventory", inventoryList);
    
    return res;
    
  }
  
  // Helper method to build list of inventory for inventory method
  static String buildList() {
    StringBuilder productListBuilder = new StringBuilder();
    if(inventory.length() == 0 || inventory == null) {
      
      return "";
      
    }
    for(int i = 0; i < inventory.length(); i++) {
      JSONObject product = inventory.getJSONObject(i);
      
      productListBuilder.append(product.getString("Name"))
              .append(", ")
              .append(product.getInt("Quantity"));
      
      if(i < inventory.length() - 1) {
        productListBuilder.append(", ");
      }
    }
    
    return productListBuilder.toString();
    
  }

  // implement me in assignment 3
  static JSONObject charCount(JSONObject req) {
    System.out.println("charcount request: " + req.toString());
    JSONObject res = testField(req, "count");
    res.put("type", "charcount");
    
    if (res.getBoolean("ok")) {
      if (!req.get("count").getClass().getName().equals("java.lang.String")) {
        res.put("ok", false);
        res.put("message", "Field count needs to be of type: String");
        
        return res;
      
      }
      
      String charCount = req.getString("count");
      
      if (req.getBoolean("findchar")) {
        char findChar = req.getString("find").charAt(0);  // The character to search for
        int count = 0;
        
        for (int i = 0; i < charCount.length(); i++) {
          if (charCount.charAt(i) == findChar) {
            count++;
          }
        }
        res.put("result", count);
      }
      else { res.put("result", charCount.length()); }
    }
    
    return res;
    
  }

  // handles the simple addmany request
  static JSONObject addmany(JSONObject req){
    System.out.println("Add many request: " + req.toString());
    JSONObject res = testField(req, "nums");
    if (!res.getBoolean("ok")) {
      return res;
    }

    int result = 0;
    JSONArray array = req.getJSONArray("nums");
    for (int i = 0; i < array.length(); i ++){
      try{
        result += array.getInt(i);
      } catch (org.json.JSONException e){
        res.put("ok", false);
        res.put("message", "Values in array need to be ints");
        return res;
      }
    }

    res.put("ok", true);
    res.put("type", "addmany");
    res.put("result", result);
    return res;
  }

  // creates the error message for wrong type
  static JSONObject wrongType(JSONObject req){
    System.out.println("Wrong type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "Type " + req.getString("type") + " is not supported.");
    return res;
  }

  // creates the error message for no given type
  static JSONObject noType(JSONObject req){
    System.out.println("No type request: " + req.toString());
    JSONObject res = new JSONObject();
    res.put("ok", false);
    res.put("message", "No request type was given.");
    return res;
  }

  // From: https://www.baeldung.com/java-validate-json-string
  public static JSONObject isValid(String json) {
    try {
      new JSONObject(json);
    } catch (JSONException e) {
      try {
        new JSONArray(json);
      } catch (JSONException ne) {
        JSONObject res = new JSONObject();
        res.put("ok", false);
        res.put("message", "req not JSON");
        return res;
      }
    }
    return new JSONObject();
  }

  // sends the response and closes the connection between client and server.
  static void overandout() {
    try {
      os.close();
      in.close();
      sock.close();
    } catch(Exception e) {e.printStackTrace();}

  }

  // sends the response and closes the connection between client and server.
  static void writeOut(JSONObject res) {
    try {
      os.writeUTF(res.toString());
      // make sure it wrote and doesn't get cached in a buffer
      os.flush();

    } catch(Exception e) {e.printStackTrace();}

  }
}