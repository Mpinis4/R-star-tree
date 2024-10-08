import java.util.ArrayList;

public class Records {

    private final String id;
    private final ArrayList<Double> coordinates;
    private final double distance;

    Records(String id,ArrayList<Double>coordinates,double distance){
        this.id=id;
        this.coordinates=coordinates;
        this.distance=distance;
    }


    Records(String id,ArrayList<Double> coordinates){
        this.id=id;
        this.coordinates=coordinates;
        this.distance=-1;

    }

    public Object showRecord(){
        if(distance!=-1){
            System.out.println(id+" "+coordinates+" "+distance);

        }else{
            System.out.println(id+" "+coordinates);
        }
        return null;
    }

    public double getDistance(){
        return distance;
    }

    public String getId(){
        return id;
    }

    public ArrayList<Double> getCoordinates(){
        return coordinates;
    }
}
