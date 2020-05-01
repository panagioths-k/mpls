package mpls;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidConnection {

    private int source;
    private int destination;
    private String code;

    public ValidConnection() {
    }

    public ValidConnection(int source, int destination, String code) {
        this.source = source;
        this.destination = destination;
        this.code = code;
    }
}
