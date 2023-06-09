package simpledb;

import java.io.Serializable;

/**
 * A RecordId is a reference to a specific tuple on a specific page of a
 * specific table.
 */
public class RecordId implements Serializable {

    private static final long serialVersionUID = 1L;

    private PageId pid;

    private int tupleno;

    /**
     * Creates a new RecordId referring to the specified PageId and tuple
     * number.
     * 
     * @param pid
     *            the pageid of the page on which the tuple resides
     * @param tupleno
     *            the tuple number within the page.
     */

    public RecordId(PageId pid, int tupleno) {
        // some code goes here
        this.pid=pid;
        this.tupleno=tupleno;
    }

    /**
     * @return the tuple number this RecordId references.
     */
    public int tupleno() {
        // some code goes here
        return tupleno;
    }

    /**
     * @return the page id this RecordId references.
     */
    public PageId getPageId() {
        // some code goes here
        return pid;
    }

    /**
     * Two RecordId objects are considered equal if they represent the same
     * tuple.
     * 
     * @return True if this and o represent the same tuple
     */
    @Override
    public boolean equals(Object o) {
        // some code goes here
        if(this==o)
            return true;
        if(o==null)
            return false;
        if(getClass()!=o.getClass())
            return false;
        RecordId rid=(RecordId) o;
        if(pid.equals(rid.pid)&&tupleno==rid.tupleno())
            return true;
        return false;
    }

    /**
     * You should implement the hashCode() so that two equal RecordId instances
     * (with respect to equals()) have the same hashCode().
     * 
     * @return An int that is the same for equal RecordId objects.
     */
    @Override
    public int hashCode() {
        // some code goes here
        int hash=0,n=1,num1=pid.hashCode(),num2=tupleno;
        while(num1/n>0){
            hash=31*hash+num1/n;
            n=n*10;
            num1/=10;
        }
        n=1;
        while(num2/n>0){
            hash=31*hash+num2/n;
            n=n*10;
            num2/=10;
        }
        return hash;
    }

}
