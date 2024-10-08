import java.util.ArrayList;

public class Record {
    private final String id;
    private final ArrayList<Double>info;
    private final ArrayList<String> name;
    private final int line;
    private final int blockID;


    Record(String id,ArrayList<Double>info,int line,int blockID,ArrayList<String> name){
        this.id=id;
        this.info=info;
        this.line=line;
        this.blockID=blockID;
        this.name=name;
    }


    public String getID(){
        return id;
    }

    public ArrayList<Double> getInfo(){
        return info;
    }
    public int getLine(){
        return line;
    }

    public int getBlockID(){
        return blockID;
    }




}