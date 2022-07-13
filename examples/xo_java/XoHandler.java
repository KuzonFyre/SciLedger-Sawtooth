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

//Ensure design from payload is int
// dont check valid action twice
//Check payload inputs when uppacking transaction?
//Apply genesis - how to check that workflow id has not been used
//Additional checks in apply to ensure that task is allowed to be done (i.e. correct order of dependencies)
//How to handle invalidating dependencies?

public class XoHandler implements TransactionHandler {

  private final Logger logger = Logger.getLogger(XoHandler.class.getName());
  private String taskNameSpace;

  /**
   * constructor.
   */
  public XoHandler() {

    try {
      this.taskNameSpace = Utils.hash512(
              this.transactionFamilyName().getBytes("UTF-8")).substring(0, 6);
    } catch (UnsupportedEncodingException usee) {
      usee.printStackTrace();
      this.taskNameSpace = "";
    }
  }

  @Override
  public String transactionFamilyName() {
    return "wf";
  }

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public Collection<String> getNameSpaces() {
    ArrayList<String> namespaces = new ArrayList<>();
    namespaces.add(this.taskNameSpace);
    return namespaces;
  }

  //Actions: Invalidate, add regular task, create genesis block
  class TransactionData {
    final String taskId;
    String parentWorkflowId;
    final String parentTaskId;
    final String workflowId;
    final String action;
    final String validRoot;
    final String invalidRoot;
    final String design;
    final long timestamp;

    TransactionData(String workflowId, String taskId, String parentWorkflowId, String parentTaskId, String action, String validRoot, String invalidRoot, String design, long timestamp) {
      this.taskId = taskId;
      this.parentWorkflowId = parentWorkflowId;
      this.parentTaskId = parentTaskId;
      this.workflowId = workflowId;
      this.action = action;
      this.validRoot = validRoot;
      this.invalidRoot = invalidRoot;
      this.design = design;
      this.timestamp = timestamp;

    }
  }

  class WorkflowData {
    final String workflowId;
    final String parentWorkflowId;
    final String parentTaskId;
    final String design;

    WorkflowData(String workflowId, String parentWorkflowId, String parentTaskId, String design) {
      this.workflowId = workflowId;
      this.parentWorkflowId = parentWorkflowId;
      this.parentTaskId = parentTaskId;
      this.design = design;
    }
  }

  @Override
  public void apply(TpProcessRequest transactionRequest, Context context)
          throws InvalidTransactionException, InternalError {
    // Call getUnpackedTransaction to unpack the transaction request to get transaction data
    TransactionData transactionData = getUnpackedTransaction(transactionRequest);

    // *** The transaction signer is the scientist who created a workflow or invalidated/added a task
    String scientist;
    //Get the header from the transaction request
    TransactionHeader header = transactionRequest.getHeader();
    //Extract the signer public key. This signer is the scientist.
    scientist = header.getSignerPublicKey();

    //if(transactionData.taskId.equals("")){
      //throw new InvalidTransactionException("Task ID required");
    //}
    if(transactionData.parentTaskId.equals("")){
      System.out.println(transactionData.taskId);
      throw new InvalidTransactionException("Parent task ID required");
    }
    if(transactionData.workflowId.equals("")){
      throw new InvalidTransactionException("Workflow ID required");
    }
    if(transactionData.action.equals("")){
      throw new InvalidTransactionException("Transaction action required");
    }
    //! did not include timestamp
    //! did not include no | allowed

    //! fill in checks for each type of transaction
    if (transactionData.action.equals("genesis")){
      if(transactionData.parentWorkflowId.equals("")){
        throw new InvalidTransactionException("Parent workflow ID required");
      }
    }
    else if(transactionData.action.equals("regular") || transactionData.action.equals("invalidation")){
      if(transactionData.validRoot.equals("")){
        throw new InvalidTransactionException("Valid merkle root required");
      }
      if(transactionData.invalidRoot.equals("")){
        throw new InvalidTransactionException("Invalid merkle root required");
      }
    }
    else{
      throw new InvalidTransactionException(
              String.format("Invalid transaction action type: %s", transactionData.action));
    }

    //Otherwise make an address using the given taskID
    String address = makeTaskAddress(transactionData.workflowId,transactionData.taskId);
    // *** context.get() returns a list.
    // *** If no data has been stored yet at the given address, it will be empty.

    String stateEntry = context.getState(
            Collections.singletonList(address)
    ).get(address).toStringUtf8();
    WorkflowData stateData = getStateData(stateEntry, transactionData.workflowId);
    
    //Call storeWorkflowData
    WorkflowData updatedWorkflowData = initiateAction(transactionData, stateData, scientist);
    System.out.println("yo");
    storeWorkflowData(address, updatedWorkflowData, stateEntry, context);
  }

  /**
   * Helper function to retrieve workflow workflowID, action, and taskID from transaction request.
   */
  private TransactionData getUnpackedTransaction(TpProcessRequest transactionRequest)
          throws InvalidTransactionException {
    String payload =  transactionRequest.getPayload().toStringUtf8();
    ArrayList<String> payloadList = new ArrayList<>(Arrays.asList(payload.split(",")));
    //If payload has more than 6 things, throw exception
    if (payloadList.size() > 9) {
      System.out.println(payloadList.toString());
      throw new InvalidTransactionException("Invalid payload serialization");
    }
    //Add empty string to payload list until it has 6 things
    while (payloadList.size() < 9) {
      payloadList.add("");
    }
    if(payloadList.get(0).equals("genesis")){
      int design;
      //Create a transaction data object with the 7 items from the payload and return it
      return new TransactionData(payloadList.get(1), payloadList.get(2), payloadList.get(3), payloadList.get(4), payloadList.get(0), "", "", payloadList.get(5), System.currentTimeMillis());
    }
    else if (payloadList.get(0).equals("regular") || payloadList.get(0).equals("invalidation")){
      //Create a transaction data object with the 7 items from the payload and return it
      System.out.println(payloadList.toString());
      return new TransactionData(payloadList.get(1), payloadList.get(2), "", payloadList.get(4), payloadList.get(0), " ", " ", "", System.currentTimeMillis());
    }
    else{
      
      throw new InvalidTransactionException("Invalid action");
    }
  }

  /**
   * Helper function to retrieve the workflowID and state from state store.
   */
  private WorkflowData getStateData(String stateEntry, String workflowId)
          throws InternalError, InvalidTransactionException {
    //?? If state entry has length zero, return a new WorkflowData object with all empty parameters. What is state entry?
    if (stateEntry.length() == 0) {
      System.out.println("Hello there");
      return new WorkflowData("", "", "", "");
    } else {
      //Call getWorkflowCsv() with stateEntry and workflowId. Split the workflowCSV into an arraylist workflowList.
      try {
        String workflowCsv = getWorkflowCsv(stateEntry, workflowId);
        ArrayList<String> workflowList = new ArrayList<>(Arrays.asList(workflowCsv.split(",")));
        //While the workflow list has less than 4 things, add empty strings
        while (workflowList.size() < 3) {
          workflowList.add("");
        }
        //Create and return a new WorkflowData object from the game list
        return new WorkflowData(workflowList.get(0), workflowList.get(1), workflowList.get(2),workflowList.get(3));
        //?? If ever an error occurs, throw exception. What could cause this?
      } catch (Error e) {
        throw new InternalError("Failed to deserialize workflow data");
      }
    }
  }

  /**
   * Helper function to generate workflow address.
   */
  private String makeTaskAddress(String workflowId, String taskId) throws InternalError {
    //Hash the task name and return taskNameSpace concatenated with a substring of the hashed name
    //Unless error occurs, then throw exception
    //! do we need to change the hashing or remove it potentially
    try {
      String hashedName = Utils.hash512((workflowId + taskId).getBytes("UTF-8"));
      return taskNameSpace + hashedName.substring(0, 64);
    } catch (UnsupportedEncodingException e) {
      throw new InternalError("Internal Error: " + e.toString());
    }
  }

  /**
   * Helper function to retrieve the correct workflow info from the list of workflow data CSV.
   */
  private String getWorkflowCsv(String stateEntry, String workflowId) {
    //Split stateEntry into workflowCSV arraylist
    ArrayList<String> workflowCsvList = new ArrayList<>(Arrays.asList(stateEntry.split("\\|")));
    //Find and return the workflowCSV in the list with the correct blockchain name
    //?? What is region matches
    //Otherwise return an empty string
    for (String workflowCsv : workflowCsvList) {
      if (workflowCsv.regionMatches(0, workflowId, 0, workflowId.length())) {
        return workflowCsv;
      }
    }
    return "";
  }

  /** Helper function to store state data. */
  private void storeWorkflowData(
          String address, WorkflowData workflowData, String stateEntry, Context context)
  //Try
          throws InternalError, InvalidTransactionException {
    //Format workflow data into a workflowDataCSV strig
    String workflowDataCsv = String.format("%s,%s,%s,%s",
            workflowData.workflowId, workflowData.parentWorkflowId, workflowData.parentTaskId, workflowData.design);
    //If stateEntry length is zero, make the state entry the workflowDataCSV
    if (stateEntry.length() == 0) {
      stateEntry = workflowDataCsv;
      //Otherwise, split the state entry into an arraylist
    } else {
      ArrayList<String> dataList = new ArrayList<>(Arrays.asList(stateEntry.split("\\|")));
      //For every item in the arraylist
      for (int i = 0; i <= dataList.size(); i++) {
        //If the number of the item matches the size of the list (the last item of the list or the item matches
        //the game name
        if (i == dataList.size()
                || dataList.get(i).regionMatches(0, workflowData.workflowId, 0, workflowData.workflowId.length())) {
          //Make that index workflowDataCsv and break
          dataList.set(i, workflowDataCsv);
          break;
        }
      }
      //Reformat the new state entry from dataList
      stateEntry = StringUtils.join(dataList, "|");
    }
    System.out.println("REached line 292");
    //Do some checks and if address size is too small, throw an exception
    ByteString csvByteString = ByteString.copyFromUtf8(stateEntry);
    Map.Entry<String, ByteString> entry = new AbstractMap.SimpleEntry<>(address, csvByteString);
    Collection<Map.Entry<String, ByteString>> addressValues = Collections.singletonList(entry);
    System.out.println("Reached 297");
    Collection<String> addresses = context.setState(addressValues);
    System.out.println(addresses);
    if (addresses.size() < 1) {
      throw new InternalError("State Error");
    }
    
  }

  /**
   * Function that handles logic based on genesis, regular, or invalidation action.
   */
  private WorkflowData initiateAction(TransactionData transactionData, WorkflowData workflowData, String scientist)
          throws InvalidTransactionException, InternalError {
    //Check transactionData action
    System.out.println("yee");
    switch (transactionData.action) {
      //! Implement proper checks prior to function calls (see old xo)
      case "genesis":
        return applyGenesis(transactionData, workflowData, scientist);
      case "regular":
        return applyRegular(transactionData, workflowData, scientist);
      case "invalidation":
        return applyInvalidation(transactionData, workflowData, scientist);

      //Overall if fails, throw exception
      default:
        throw new InvalidTransactionException(String.format(
                "Invalid action: %s", transactionData.action));
    }
  }

  //! What is the state and how are the 3 actions changing it?
  /**
   * Function that handles blockchain logic for 'genesis' action.
   */
  private WorkflowData applyGenesis(TransactionData transactionData, WorkflowData workflowData, String scientist)
          throws InvalidTransactionException {

    //!!! Must add check like above to ensure that a workflow with same id doesn't exist already

//        if (!workflowData.state.equals("")) {
//            throw new InvalidTransactionException("Invalid Action: Blockchain already exists");
//        }


    //Call display function and give scientist name
    display(String.format("Scientist %s created a workflow genesis block", abbreviate(scientist)));
    //Return new WorkflowData object with information about the new workflow
    //String[] designArray = new String[Integer.parseInt(transactionData.design)];
    //Arrays.fill(designArray, "-");
    //String designString = Arrays.toString(designArray);
    //Set the design array to have all values set to "-" which represents that the task has not been done ever
    return new WorkflowData(
            transactionData.workflowId, transactionData.parentWorkflowId, transactionData.parentTaskId, " ");
  }



  /**
   * Function that handles logic for 'regular' action (workflow task).
   */
  private WorkflowData applyRegular(TransactionData transactionData, WorkflowData workflowData, String scientist)
          throws InvalidTransactionException, InternalError {

    //get task state
    char[] designArray = workflowData.design.toCharArray();
    //char taskState = designArray[Integer.parseInt(transactionData.taskId)];
    //make sure task hasn't already been done
    //if (taskState == 'v'){
      //throw new InvalidTransactionException(String.format(
        //      "Invalid action. Workflow task %s already complete", transactionData.taskId));
    //}
    //Set task state to valid
    //designArray[Integer.parseInt(transactionData.taskId)] = 'v';
    String updatedDesign = Arrays.toString(designArray);

    //Create updated workflowData with the new state in the design
    WorkflowData updatedWorkflowData = new WorkflowData(
            workflowData.workflowId, workflowData.parentWorkflowId, workflowData.parentTaskId, updatedDesign);

    //Call display() to show the action taken and return the updated blockchainData object
    display(
            String.format("Scientist %1$s performs workflow task %2$s on workflow %3$s:\n", abbreviate(scientist), transactionData.taskId, workflowData.workflowId));

    return updatedWorkflowData;
  }

  /**
   * Function that handles logic for 'invalidation' action.
   */
  private WorkflowData applyInvalidation(TransactionData transactionData, WorkflowData workflowData, String scientist)
          throws InvalidTransactionException, InternalError {

    char[] designArray = workflowData.design.toCharArray();
    char taskState = designArray[Integer.parseInt(transactionData.taskId)];
    //make sure task has been done and is valid
    if (taskState == 'i'){
      throw new InvalidTransactionException(String.format(
              "Invalid action. Workflow task %s already invalid", transactionData.taskId));
    }
    else if (taskState == '-'){
      throw new InvalidTransactionException(String.format(
              "Invalid action. Workflow task %s has not been completed yet", transactionData.taskId));
    }
    //Set task state to valid
    designArray[Integer.parseInt(transactionData.taskId)] = 'i';
    String updatedDesign = Arrays.toString(designArray);

    //Create updated workflowData with the new state in the design
    WorkflowData updatedWorkflowData = new WorkflowData(
            workflowData.workflowId, workflowData.parentWorkflowId, workflowData.parentTaskId, updatedDesign);

    //Call display() to show the action taken and return the updated blockchainData object
    display(
            String.format("Scientist %1$s performs invalidation of workflow task %2$s on workflow %3$s:\n", abbreviate(scientist), transactionData.taskId, workflowData.workflowId));

    return updatedWorkflowData;
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
