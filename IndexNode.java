import java.util.ArrayList;
import java.util.List;

public class IndexNode {
    private final int id;
    private final List<Double> mbr;
    private final List<Integer> childs;
    private final String blockID;
    private final int dimensions;

    IndexNode(int dimensions,int id, List<Double> mbr, List<Integer> childs, String blockID)
    {
        this.dimensions=dimensions;
        this.id=id;
        this.mbr=new ArrayList<>(mbr);
        this.childs= new ArrayList<>(childs);
        this.blockID=blockID;

    }

    public int getId()
    {
        return id;
    }

    public List<Double> getMbr()
    {
        return mbr;

    }

    public List<Integer> getChilds()
    {
        return childs;
    }

    public String getBlockID()
    {
        return blockID;
    }


}
