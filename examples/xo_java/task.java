import java.util.ArrayList;
import java.util.Arrays;

public class task {
    private String workflowID;
    private String taskID;
    private boolean invalidated;
    private ArrayList<Integer> idxParent;
    public task(String workflowID, String taskID, boolean invalidated, ArrayList<Integer> idxParent){
        this.workflowID = workflowID;
        this.taskID = taskID;
        this.invalidated = invalidated;
        this.idxParent=idxParent;
    }

    public String getTaskID() {
        return taskID;
    }

    public ArrayList<Integer> getIdxParent() {
        return idxParent;
    }

    public void addIdxParent(int parent) {
        this.idxParent.add(parent);
    }

    @Override
    public String toString() {
        return this.workflowID+", " + this.taskID+", " + this.invalidated + ", " + this.idxParent+ "\n";
    }
}
