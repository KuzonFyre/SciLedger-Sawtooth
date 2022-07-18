import java.io.IOException;
import java.util.Random;
import java.util.*;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;

public class randomizeGen {
    static int maxWorkflows =1;
    static int maxWSize = 10;
    static double PERBRANCH = 0.3;
    static ArrayList<Integer> startPoint = new ArrayList<>(maxWorkflows);
    static ArrayList<ArrayList<task>> workflows = new ArrayList<>();

    public static void generate(){
        workflows.add(genRandWorkflow(0,null,null));
        Random rand = new Random();
        for(int i=1; i<maxWorkflows; i++) {
            int randWf = rand.nextInt((i));
            workflows.add(genRandWorkflow(i,rand.nextInt(startPoint.get(randWf))+1,randWf));
        }
    }
    // Function to generate random graph
    public static ArrayList<task> genRandWorkflow(int wf, Integer stpt, Integer stwf) {

        Random rand = new Random();
        int wSize = rand.nextInt(maxWSize/2) + 3;
        int branchCount = (int)(wSize* PERBRANCH) + 1;
        int counter = 1;
        int randIdx;
        ArrayList<task> workflow = new ArrayList<>();

        workflow.add(new gentask("w" + wf, "gen", String.valueOf(stwf),String.valueOf(stpt)));
        workflow.add(new task("w" + wf, "t1", false, new ArrayList<>(Arrays.asList(0))));
        while (counter < wSize) {
            workflow.add(new task("w" + wf, "t" + (counter +1), false, new ArrayList<>(Arrays.asList(counter))));
            counter++;
        }
        int linear = counter;

         for(int i = 0; i< branchCount; i++){
             randIdx = rand.nextInt(linear-2)+1;
                 workflow.add(new task("w" + wf, "t" + (counter +1), false, new ArrayList<>(Arrays.asList(randIdx))));
                 counter++;
                int branchLen = rand.nextInt(4);
                for(int j=0; j<branchLen; j++){
                    workflow.add(new task("w" + wf, "t" + (counter +1), false, new ArrayList<>(Arrays.asList(counter))));
                    counter++;
                }
                task merge = workflow.get(rand.nextInt(linear-1-randIdx)+randIdx+2);
                merge.addIdxParent(counter);
         }
        startPoint.add(counter);
        Collections.sort(workflow, new Comparator<task>() {
    @Override
    public int compare(task first, task second) {
        if(Collections.max(first.getIdxParent()) < Collections.max(second.getIdxParent())){
        return -1;
        }else if(Collections.max(first.getIdxParent()) > Collections.max(second.getIdxParent())){
        return 1;
    }
    return 0;
    }
});
        return workflow;

    }
    


    public static void main(String[] args) {
        generate();
        System.out.println(workflows);
        String allArgs = "";
        for(int i=0; i<workflows.size(); i++){
        	for(int j=0; j<workflows.get(i).size(); j++){
        		allArgs += "wf regular " + workflows.get(i).get(j).toString() + ";";
        	}
        }
        System.out.println(allArgs);
       String command2 = "docker exec -it sawtooth-shell-default bash";
        
        try {
        List<String> list = new ArrayList<String>();
        list.add("bash");
        list.add("-c");
        list.add(allArgs);
        //list.add("wf regular w1 t1 t0; wf regular w1 t2 t1");
        //list.add("-it");
        //list.add("sawtooth-shell-default");
        //list.add("bash");
        //list.add("ls");
        //list.add("ls");
        
        ProcessBuilder pb = new ProcessBuilder(list);
        pb.inheritIO();
        Process process = pb.start();
        process.waitFor();
        
        //OutputStream out = process.getOutputStream();  
        
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
                

        
        
        
        


        //         List<String> list = new ArrayList<String>();
        //list.add("gnome-terminal");
 
        // create the process
        //ProcessBuilder build = new ProcessBuilder(list);
        
        
	//Process proc = Runtime.getRuntime().exec(command2);

	//Process proc1 = Runtime.getRuntime().exec(command2);
        // Read the output
        //OutputStream out = proc.getOutputStream(); 
        //byte[] dataBytes = "command\n".getBytes(); 
	//out.write(dataBytes);  
	//out.flush();
	//BufferedReader reader1 =  
          //    new BufferedReader(new InputStreamReader(proc.getInputStream()));
        //String line = "";
        //while((line = reader1.readLine()) != null) {
          //  System.out.print(line + "\n");
        //}

        //proc.waitFor();  

    }


}
