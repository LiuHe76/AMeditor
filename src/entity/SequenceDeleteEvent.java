package entity;

import java.util.Deque;

/**
 *   Event interface implemented class to store information for pasting-counter operation.
 */
public class SequenceDeleteEvent implements Event {
    private Deque<Object> nodes;
    private Object start;

    public Deque<Object> getNodes() {
        return nodes;
    }

    public void setNodes(Deque<Object> nodes) {
        this.nodes = nodes;
    }

    public Object getStart() {
        return start;
    }

    public void setStart(Object start) {
        this.start = start;
    }
}
