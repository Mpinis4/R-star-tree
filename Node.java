import java.util.ArrayList;

public class Node {

    private ArrayList<Record> records;
    private ArrayList<Node> children;

    private Node parent;
    private int dimensions;

    private boolean leaf;

    private double[][] mbr;

    Node(int dimensions,Node parent){
        this.parent=parent;
        this.dimensions=dimensions;
        this.leaf=true;
        mbr=new double[dimensions][2];
        records=new ArrayList<>();
        children=new ArrayList<>();
    }


    public void adjustMbr(Record newEntry){
        for(int i=0; i<dimensions; i++){
            double entryCoor=newEntry.getInfo().get(i);

            if(entryCoor<mbr[i][0]|| mbr[i][0]==0){
                mbr[i][0]=entryCoor;
            }
            if(entryCoor>mbr[i][1] || mbr[i][1]==0){
                mbr[i][1]=entryCoor;
            }
        }
    }

    public void adjustMbr(Node newNode){
        for(int i=0; i<dimensions; i++){
            if(newNode.getMbr()[i][0]<mbr[i][0]|| mbr[i][0]==0){
                mbr[i][0]=newNode.getMbr()[i][0];

            }
            if(newNode.getMbr()[i][1]>mbr[i][1] || mbr[i][1]==0){
                mbr[i][1]=newNode.getMbr()[i][1];
            }
        }
    }


    public void clearMbr(){
        mbr=new double[dimensions][2];
    }


    public double[][] getMbr(){
        return mbr;
    }

    public Node getParent(){
        return  parent;

    }


    public ArrayList<Node> getChildren(){
        return children;
    }

    public ArrayList<Record> getRecords(){
        return records;
    }

    public boolean isLeaf(){
        return leaf;

    }

    public int getDimensions(){return dimensions;}

    public void setParent(Node parent){
        this.parent=parent;
    }

    public void setLeaf(boolean leaf){
        this.leaf=leaf;
    }

    public void addRecord(Record given){records.add(given);}

}