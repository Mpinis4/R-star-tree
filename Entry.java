import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class Entry {

    private final String id;
    private final ArrayList<Double> coordinates;

    private ArrayList<String> name;

    public Entry(String id,ArrayList<Double> coordinates,ArrayList<String>name) {
        this.id = id;
        this.coordinates=coordinates;
        this.name=name;
    }

    Entry(String id, ArrayList<Double> coordinates){
        this.id=id;
        this.coordinates=coordinates;
    }

    public String getId(){
        return id;
    }


    public String getName(){
        if(name==null){
            return String.valueOf(name);
        }
        else{
            return name.toString().replace("[","").replace(",","").replace("]"," ");
        }
    }

    public String getCoordinates(){
        return Arrays.toString(coordinates.toArray()).replace("[","").replace(",","").replace("]"," ");

    }
}