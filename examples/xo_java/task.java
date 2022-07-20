import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class task{
    private String workflowID;
    private String taskID;
    private boolean invalidated;
    private ArrayList<Integer> idxParent;
    private String hexStr;
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

    public String getHexStr(){
    	return this.hexStr;
    	}
public void addMerkleHash(String hexStr){
	this.hexStr = hexStr;
}
	public String hashString(){
	return this.workflowID + this.taskID +"";
	}
	public String getMerkleHash(){
	return this.hexStr;
}
    @Override
    public String toString() {
    String str = this.workflowID + " " + this.taskID + " -pt ";
    	for(int i=0; i<idxParent.size(); i++){
    	str+= idxParent.get(i).toString() + " ";
    	}
    return str;
    }
    
}
