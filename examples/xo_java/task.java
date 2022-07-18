import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class task implements Comparable<task>{
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
    	if (idxParent==null){
    		ArrayList<Integer> list = new ArrayList<Integer>();
    		list.add(-2);
    		return list;
    	}else{
    	
        return idxParent;
        }
    }
    
    public int getAddedParentVal(){
    	int add =0;
        for(int i=0; i<this.idxParent.size(); i++){
        	add += idxParent.get(i);
        }
        return add;
    }

    public void addIdxParent(int parent) {
        this.idxParent.add(parent);
    }

    @Override
    public int compareTo(task task){
    	System.out.println("Compare: " + task.toString());
    	System.out.println(this.toString());
        if(Collections.max(task.getIdxParent()) < Collections.max(this.getIdxParent())){
        System.out.println("1");
        return 1;
        }else if(Collections.max(task.getIdxParent()) > Collections.max(this.getIdxParent())){
        System.out.println("-1");
        return -1;
    }else{
    System.out.println("0");
    return 0;
    }
    }


    @Override
    public String toString() {
        return this.workflowID+", " + this.taskID+", " + this.invalidated + ", " + this.idxParent+ "\n";
    }
}
