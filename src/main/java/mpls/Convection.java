package mpls;

import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Convection {

    private Rectangle rectangle;
    private Convection nextUpConvection;
    private boolean nextUpConvectionIsDown;         //false: up  true: down
    private Convection nextDownConvection;
    private boolean nextDownConvectionIsDown;       //false: up  true: down
    private boolean sourceConvection;               //lefter column
    private boolean finalConvection;                //right column
    private Integer upFlag;
    private Integer downFlag;
    private int i;
    private int j;
    private Line inUp;
    private Line inDown;
    private Line outUp;
    private Line outDown;
    private Text textUp;
    private Text textDown;

    public Convection() {

    }

    public Convection(Rectangle rectangle, Convection nextUpConvection, boolean nextUpConvectionIsDown, Convection nextDownConvection, boolean nextDownConvectionIsDown, boolean sourceConvection, boolean finalConvection, Integer upFlag, Integer downFlag, int i, int j) {
        this.rectangle = rectangle;
        this.nextUpConvection = nextUpConvection;
        this.nextUpConvectionIsDown = nextUpConvectionIsDown;
        this.nextDownConvection = nextDownConvection;
        this.nextDownConvectionIsDown = nextDownConvectionIsDown;
        this.sourceConvection = sourceConvection;
        this.finalConvection = finalConvection;
        this.upFlag = upFlag;
        this.downFlag = downFlag;
        this.i = i;
        this.j = j;
    }
}
