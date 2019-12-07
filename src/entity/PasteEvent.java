package entity;

/**
 *  Event interface implemented class to store information for pasting operation.
 */
public class PasteEvent implements Event {
    private Integer length;
    private Object lastNode;

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Object getLastNode() {
        return lastNode;
    }

    public void setLastNode(Object lastNode) {
        this.lastNode = lastNode;
    }
}
