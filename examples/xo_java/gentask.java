import java.util.ArrayList;

public class gentask extends task{
    ArrayList<task> info;
    private String workflowID;
    private String taskID;
    private String pWID;
    private String pTID;

    public gentask(String workflowID, String taskID, String pWID, String pTID) {
        super(workflowID, taskID, false, null);
        this.workflowID = workflowID;
        this.taskID=taskID;
        this.pWID = pWID;
        this.pTID = pTID;
    }

    public void setInfo(ArrayList<task> info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return "Workflow ID: " + this.workflowID+"\nTask ID: " + this.taskID+ "\nParent Workflow: " + this.pWID + "\nParent Task ID:  " + this.pTID + "\nWorkflow Info:\n";
    }
}
