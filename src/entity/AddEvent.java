package entity;

/**
 *  Event interface implemented class to store information for adding operation.
 */
public class AddEvent implements Event {
    private Object element;

    public Object getElement() {
        return element;
    }

    public void setElement(Object element) {
        this.element = element;
    }
}
