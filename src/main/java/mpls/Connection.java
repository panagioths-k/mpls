package mpls;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Connection {

    private int sourceRow;
    private int sourceColumn;
    private boolean source;         //0(false): up  1(true): down
    private int destinationRow;
    private int destinationColumn;
    private boolean destination;    //0(false): up  1(true): down

    public Connection(){

    }

    public Connection(int sourceRow, int sourceColumn, boolean source, int destinationRow, int destinationColumn, boolean destination) {
        this.sourceRow = sourceRow;
        this.sourceColumn = sourceColumn;
        this.source = source;
        this.destinationRow = destinationRow;
        this.destinationColumn = destinationColumn;
        this.destination = destination;
    }
}
