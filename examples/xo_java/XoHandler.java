/* Copyright 2017 Intel Corporation
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
------------------------------------------------------------------------------*/

package sawtooth.examples.xo;


import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.commons.lang3.StringUtils;

import sawtooth.sdk.processor.Context;
import sawtooth.sdk.processor.TransactionHandler;
import sawtooth.sdk.processor.Utils;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.protobuf.TransactionHeader;

import java.io.UnsupportedEncodingException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

public class XoHandler implements TransactionHandler {

  private final Logger logger = Logger.getLogger(XoHandler.class.getName());
  private String xoNameSpace;

  /**
   * constructor.
   */
  public XoHandler() {
  
    try {
      this.xoNameSpace = Utils.hash512(
        this.transactionFamilyName().getBytes("UTF-8")).substring(0, 6);
    } catch (UnsupportedEncodingException usee) {
      usee.printStackTrace();
      this.xoNameSpace = "";
    }
  }

  @Override
  public String transactionFamilyName() {
    return "xo";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public Collection<String> getNameSpaces() {
    ArrayList<String> namespaces = new ArrayList<>();
    namespaces.add(this.xoNameSpace);
    return namespaces;
  }

  class TransactionData {
    final String gameName;
    final String action;
    final String space;

    TransactionData(String gameName, String action, String space) {
      this.gameName = gameName;
      this.action = action;
      this.space = space;
    }
  }

  class GameData {
    final String gameName;
    final String board;
    final String state;
    final String playerOne;
    final String playerTwo;

    GameData(String gameName, String board, String state, String playerOne, String playerTwo) {
      this.gameName = gameName;
      this.board = board;
      this.state = state;
      this.playerOne = playerOne;
      this.playerTwo = playerTwo;
    }
  }

  @Override
  public void apply(TpProcessRequest transactionRequest, Context context)
      throws InvalidTransactionException, InternalError {
    // Call getUnpackedTransaction to unpack the transaction request to get transaction data
    TransactionData transactionData = getUnpackedTransaction(transactionRequest);

    // *** The transaction signer is the player
    String player;
    //Get the header from the transaction request
    TransactionHeader header = transactionRequest.getHeader();
    //Extract the signer public key. This signer is the player.
    player = header.getSignerPublicKey();
    int space = 0;
    //Check the space depending on the action this is the space they want to play or the size of the board
    //If space is not a number throw an exception depending on the action taken (create or take)
    try {
      space = Integer.parseInt(transactionData.space);
    } catch (NumberFormatException e) {
      if (transactionData.action.equals("take")) {
        throw new InvalidTransactionException("Space could not be converted to an integer.");
      }
      if (transactionData.action.equals("create")) {
        throw new InvalidTransactionException("Dimensions could not be converted to an integer. "
         + transactionData.space);
      }
    }
    //If game name is empty, throw exception
    if (transactionData.gameName.equals("")) {
      throw new InvalidTransactionException("Name is required");
    }
    //If game name has |, throw exception
    if (transactionData.gameName.contains("|")) {
      throw new InvalidTransactionException("Game name cannot contain '|'");
    }
    //If action is empty, throw exception
    if (transactionData.action.equals("")) {
      throw new InvalidTransactionException("Action is required");
    }
    //If action is create and space (size) is less than 1 or greater than 12, throw exception (invalid dimensions)
    if (transactionData.action.equals("create")) {
      if (space < 1 || space > 12) {
        throw new InvalidTransactionException(
          String.format(
          "Invalid dimension:  Should be between 1 and 12: %s", transactionData.space
          ));
      }
    }
    //If action is neither take nor create, throw exception (invalid action)
    if (!transactionData.action.equals("take") && !transactionData.action.equals("create")) {
      throw new InvalidTransactionException(
          String.format("Invalid action: %s", transactionData.action));
    }
    //Otherwise make a game address using the given game name
    //?? Start
    String address = makeGameAddress(transactionData.gameName);
    // *** context.get() returns a list.
    // *** If no data has been stored yet at the given address, it will be empty.
    String stateEntry = context.getState(
        Collections.singletonList(address)
    ).get(address).toStringUtf8();
    GameData stateData = getStateData(stateEntry, transactionData.gameName);
    //?? End

    //Call playXO to update the game data
    //Call storeGameData
    GameData updatedGameData = playXo(transactionData, stateData, player);
    storeGameData(address, updatedGameData, stateEntry, context);
  }

  /**
   * Helper function to retrieve game gameName, action, and space from transaction request.
   */
  private TransactionData getUnpackedTransaction(TpProcessRequest transactionRequest)
      throws InvalidTransactionException {
    //?? Gets string transaction request payload and makes a list of it. What is the payload?
    String payload =  transactionRequest.getPayload().toStringUtf8();
    ArrayList<String> payloadList = new ArrayList<>(Arrays.asList(payload.split(",")));
    //If payload has more than 3 things, throw exception
    if (payloadList.size() > 3) {
      throw new InvalidTransactionException("Invalid payload serialization");
    }
    //Add empty string to payload list until it has 3 things
    while (payloadList.size() < 3) {
      payloadList.add("");
    }
    //Create a transaction data object with the 3 items from the payload and return it
    return new TransactionData(payloadList.get(0), payloadList.get(1), payloadList.get(2));
  }

  /**
   * Helper function to retrieve the board, state, playerOne, and playerTwo from state store.
   */
  private GameData getStateData(String stateEntry, String gameName)
      throws InternalError, InvalidTransactionException {
    //?? If state entry has length zero, return a new GameData object with all empty parameters. What is state entry?
    if (stateEntry.length() == 0) {
      return new GameData("", "", "", "", "");
    } else {
      //Call getGameCsv() with stateEntry and gameName. Split the gameCSV into an arraylist gameList.
      try {
        String gameCsv = getGameCsv(stateEntry, gameName);
        ArrayList<String> gameList = new ArrayList<>(Arrays.asList(gameCsv.split(",")));
        //While the game list has less than 5 things, add empty strings
        while (gameList.size() < 5) {
          gameList.add("");
        }
        //Create and return a new GameData object from the game list
        return new GameData(gameList.get(0), gameList.get(1),
            gameList.get(2), gameList.get(3), gameList.get(4));
        //?? If ever an error occurs, throw exception. What could cause this?
      } catch (Error e) {
        throw new InternalError("Failed to deserialize game data");
      }
    }
  }

  /**
   * Helper function to generate game address.
   */
  private String makeGameAddress(String gameName) throws InternalError {
    //Hash the game name and return xoNameSpace concatenated with a substring of the hashed name
    //Unless error occurs, then throw exception
    try {
      String hashedName = Utils.hash512(gameName.getBytes("UTF-8"));
      return xoNameSpace + hashedName.substring(0, 64);
    } catch (UnsupportedEncodingException e) {
      throw new InternalError("Internal Error: " + e.toString());
    }
  }

  /**
   * Helper function to retrieve the correct game info from the list of game data CSV.
   */
  private String getGameCsv(String stateEntry, String gameName) {
    //Split stateEntry into gameCSV arralist
    ArrayList<String> gameCsvList = new ArrayList<>(Arrays.asList(stateEntry.split("\\|")));
    //Find and return the gameCSV in the list with the correct game name
    //?? What is region matches
    //Otherwise return an empty string
    for (String gameCsv : gameCsvList) {
      if (gameCsv.regionMatches(0, gameName, 0, gameName.length())) {
        return gameCsv;
      }
    }
    return "";
  }

  /** Helper function to store state data. */
  private void storeGameData(
      String address, GameData gameData, String stateEntry, Context context)
  //Try
      throws InternalError, InvalidTransactionException {
    //Format game data into a gameDataCSV strig
    String gameDataCsv = String.format("%s,%s,%s,%s,%s",
        gameData.gameName, gameData.board, gameData.state, gameData.playerOne, gameData.playerTwo);
    //If stateEntry length is zero, make the state entry the gameDataCSV
    if (stateEntry.length() == 0) {
      stateEntry = gameDataCsv;
      //Otherwise, split the state entry into an arraylist
    } else {
      ArrayList<String> dataList = new ArrayList<>(Arrays.asList(stateEntry.split("\\|")));
      //For every item in the arraylist
      for (int i = 0; i <= dataList.size(); i++) {
        //If the number of the item matches the size of the list (the last item of the list or the item matches
        //the game name
        if (i == dataList.size()
            || dataList.get(i).regionMatches(0, gameData.gameName, 0, gameData.gameName.length())) {
          //Make that index gameDataCsv and break
          dataList.set(i, gameDataCsv);
          break;
        }
      }
      //Reformat the new state entry from dataList
      stateEntry = StringUtils.join(dataList, "|");
    }

    //Do dome checks and if address size is too small, throw an exception
    ByteString csvByteString = ByteString.copyFromUtf8(stateEntry);
    Map.Entry<String, ByteString> entry = new AbstractMap.SimpleEntry<>(address, csvByteString);
    Collection<Map.Entry<String, ByteString>> addressValues = Collections.singletonList(entry);
    Collection<String> addresses = context.setState(addressValues);
    if (addresses.size() < 1) {
      throw new InternalError("State Error");
    }
  }

  /**
   * Function that handles game logic.
   */
  private GameData playXo(TransactionData transactionData, GameData gameData, String player)
      throws InvalidTransactionException, InternalError {
    //Check transactionData action
    switch (transactionData.action) {
      //If create, call apply create
      case "create":
        return applyCreate(transactionData, gameData, player);
        //If take get the space
      case "take":
        int space;
        // Convert space to integer and if this fails throw an exception.
        try {
          space = Integer.parseInt(transactionData.space);
        } catch (NumberFormatException e) {
          throw new InvalidTransactionException("Space could not be converted to an integer.");
        }
        //If space outside the board spaces, throw exception
        if (space < 1 || space > gameData.board.length()) {
          throw new InvalidTransactionException(
              String.format("Invalid space: %s", transactionData.space));
        }
        //Otherwise, call applyTake()
        return applyTake(transactionData, gameData, player);
        //Overall if fails, throw exception
      default:
        throw new InvalidTransactionException(String.format(
            "Invalid action: %s", transactionData.action));
    }
  }

  /**
   * Function that handles game logic for 'create' action.
   */
  private GameData applyCreate(TransactionData transactionData, GameData gameData, String player)
      throws InvalidTransactionException {
    //If gameData board is not empty, throw exception becuase game already exists
    if (!gameData.board.equals("")) {
      throw new InvalidTransactionException("Invalid Action: Game already exists");
    }
    //Call display function and give player name
    display(String.format("Player %s created a game", abbreviate(player)));
    //Return new GameData object with information about the new game
    return new GameData(
    transactionData.gameName, 
    new String(new char[
    (int) Math.pow(Integer.parseInt(transactionData.space),2)
    ]).replace('\0', '-'), 
    "P1-NEXT", "", "");
  }

  /**
   * Function that handles game logic for 'take' action.
   */
  private GameData applyTake(TransactionData transactionData, GameData gameData, String player)
      throws InvalidTransactionException, InternalError {
    //If the gameData state shows a win or tie has already happened, throw exception because the game already ended
    if (Arrays.asList("P1-WIN", "P2-WIN", "TIE").contains(gameData.state)) {
      throw new InvalidTransactionException("Invalid action: Game has ended");
    }
    //If the game board doesn't exist, throw exception
    if (gameData.board.equals("")) {
      throw new InvalidTransactionException("Invalid action: 'take' requires an existing game");
    }
    //If the state is not a player taking a turn, the game is invalid
    if (!Arrays.asList("P1-NEXT", "P2-NEXT").contains(gameData.state)) {
      throw new InternalError(String.format(
          "Internal Error: Game has reached an invalid state: %s", gameData.state));
    }

    // *** Assign players if new game
    String updatedPlayerOne = gameData.playerOne;
    String updatedPlayerTwo = gameData.playerTwo;
    if (gameData.playerOne.equals("")) {
      updatedPlayerOne = player;
    } else if (gameData.playerTwo.equals("")) {
      updatedPlayerTwo = player;
    }

    // *** Verify player identity and take space
    //Get space and board. If the space is already used, throw exception
    int space = Integer.parseInt(transactionData.space);
    char[] boardList = gameData.board.toCharArray();
    String updatedState;
    if (boardList[space - 1] != '-') {
      throw new InvalidTransactionException("Space already taken");
    }

    //Otherwise, verify that the correct player is playing (throw exception if not),
    //mark the space on the baord, switch the state to the next player's turn
    if (gameData.state.equals("P1-NEXT") && player.equals(updatedPlayerOne)) {
      boardList[space - 1] = 'H';
      updatedState = "P2-NEXT";
    } else if (gameData.state.equals("P2-NEXT") && player.equals(updatedPlayerTwo)) {
      boardList[space - 1] = 'Q';
      updatedState = "P1-NEXT";
    } else {
      throw new InvalidTransactionException(String.format(
          "Not this player's turn: %s", abbreviate(player)));
    }
    //Update the board, state, and create new GameData object from these
    String updatedBoard = String.valueOf(boardList);
    updatedState = determineState(boardList, updatedState);
    GameData updatedGameData = new GameData(
        gameData.gameName, updatedBoard, updatedState, updatedPlayerOne, updatedPlayerTwo);

    //Call display() to show the action taken and return the updated GameData object
    display(
        String.format("Player %s takes space %d \n", abbreviate(player), space)
            + gameDataToString(updatedGameData));
    return updatedGameData;
  }

  /**
   * Helper function that updates game state based on the current board position.
   */
  private String determineState(char[] boardList, String state) {
    //Call isWin() to check if a player has won or tied.
    //If so, change the state.
    //Return state (may not have changed)
    if (isWin(boardList, 'X')) {
      state = "P1-WIN";
    } else if (isWin(boardList, 'O')) {
      state = "P2-WIN";
    } else if (!(String.valueOf(boardList).contains("-"))) {
      state = "TIE";
    }
    return state;
  }

  /**
   * Helper function that analyzes board position to determine if it is in a winning state.
   */
  private boolean isWin(char[] board, char letter) {

    //Set win to false
    boolean win = false;
    //*** Horizontal Wins
    //Get board dimension
    int dim = (int) Math.sqrt(board.length);
    //Loop through the rows of the board, set win to true
    for (int i = 0; i < dim; i++) {
      win = true;
      //Loop through columns of the board
      for (int j = 0; j < dim; j++) {
        //If the space at the intersection of the row and column is not taken
        //set win to false and break
        if (board[i * dim + j] != letter) {
          win = false;
          break;
        }
      }
    }
    //*** vertical wins
    //Loop through columns of the board, set win to true
    for (int i = 0; i < dim; i++) {
      win = true;
      //Loop through rows on the board
      for (int j = 0; j < dim; j++) {
        //If the space at the intersection of the row and column is not taken
        //set win to false and break
        if (board[(i * dim) + j] != letter) {
          win = false;
          break;
        }
      }
    }
    //*** diagonal 1
    {
      //Set win to true and loop through the diagonal spaces
      win = true;
      for (int j = 0; j < dim; j++) {
        //If the space is not taken, set win to false and break
        if (board[j * (dim + 1)] != letter) {
          win = false;
          break;
        }
      }
    }
    //*** diagonal 2
    {
      //Repeat of the above, doing back diagonal
      win = true;
      for (int j = 0; j < dim; j++) {
        if (board[(j + 1) * (dim - 1)] != letter) {
          win = false;
          break;
        }
      }
    }
    //Give the state of win after checking horizontal, vertical, forward diagonal, and backward diagonal
    return win;
  }

  /**
   * Helper function to create an ASCII representation of the board.
   */
  private String gameDataToString(GameData gameData) {
    String out = "";
    //Build output with gameName, players, and state
    out += String.format("GAME: %s\n", gameData.gameName);
    out += String.format("PLAYER 1: %s\n", abbreviate(gameData.playerOne));
    out += String.format("PLAYER 2: %s\n", abbreviate(gameData.playerTwo));
    out += String.format("STATE: %s\n", gameData.state);
    out += "\n";

    //Take the board string and replace '-' (empty space) with ' ' then convert to array
    char[] board = gameData.board.replace('-',' ').toCharArray();

    //Get board dimension
    int dim = (int) Math.sqrt(board.length);

    //Loop through all spaces on the board
    //Depending on the space, format the character and add the lines needed to visualize the board
    for (int i = 0; i < dim - 1; i++) {
      for (int j = 0; j < dim - 1; j++) {
        out += String.format(" %c |", board[i * dim + j]);
      }
      out += String.format(" %c\n ", board[i * dim + (dim - 1)]);
      
      for (int j = 0; j < dim - 1; j++) {
        out += String.format("---|");
      }
      out += String.format("---\n");
    }
    
    for (int j = 0; j < dim - 1; j++) {
      out += String.format(" %c |", board[(dim - 1) * dim + j]);
    }
    out += String.format(" %c\n ", board[(dim - 1) * dim + (dim - 1)]);
    //Return the string that represents the board image
    return out;
  }

  /**
   * Helper function to print game data to the logger.
   */
  private void display(String msg) {
    String displayMsg = "";
    //Overall, just gets the length of the longest line in the message
    //Set length to 0
    int length = 0;
    //Make an array of the msg by splitting at new lines
    String[] msgLines = msg.split("\n");
    //If there are new lines, for each line if the length of the
    // line is greater than length, set length to be the line length
    if (msg.contains("\n")) {
      for (String line : msgLines) {
        if (line.length() > length) {
          length = line.length();
        }
      }
      //If no newlines, set length to be the message length
    } else {
      length = msg.length();
    }

  //Format the display message with dashes and call logger.info with the display message
    displayMsg = displayMsg.concat("\n+" + printDashes(length + 2) + "+\n");
    for (String line : msgLines) {
      displayMsg = displayMsg.concat("+" + StringUtils.center(line, length + 2) + "+\n");
    }
    displayMsg = displayMsg.concat("+" + printDashes(length + 2) + "+");
    logger.info(displayMsg);
  }

  /**
   * Helper function to create a string with a specified number of dashes (for logging purposes).
   */
  private String printDashes(int length) {
    //Make string of dashes with "length" number of dashes
    String dashes = "";
    for (int i = 0; i < length; i++) {
      dashes = dashes.concat("-");
    }
    return dashes;
  }

  /**
   * Helper function to shorten a string to a max of 6 characters for logging purposes.
   */
  private Object abbreviate(String player) {
    return player.substring(0, Math.min(player.length(), 6));
  }
}
