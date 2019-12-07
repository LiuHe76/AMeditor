package entity;

/**
 *  Event interface implemented class to store information for deletion operation.
 */
public class DeleteEvent implements Event {
    Object element;
    Object pre;

    public Object getElement() {
        return element;
    }

    public void setElement(Object element) {
        this.element = element;
    }

    public Object getPre() {
        return pre;
    }

    public void setPre(Object pre) {
        this.pre = pre;
    }
}
