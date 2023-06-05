package simpledb;

import java.util.*;

/**
 * The Join operator implements the relational join operation.
 */
public class HashEquiJoin extends Operator {

    private static final long serialVersionUID = 1L;

    private JoinPredicate p;

    private DbIterator child1;

    private DbIterator child2;

    private Tuple t1;

    private Tuple t2;

    private HashMap<Field, ArrayList<Tuple>> buckets;

    /**
     * Constructor. Accepts to children to join and the predicate to join them
     * on
     *
     * @param p      The predicate to use to join the children
     * @param child1 Iterator for the left(outer) relation to join
     * @param child2 Iterator for the right(inner) relation to join
     */
    public HashEquiJoin(JoinPredicate p, DbIterator child1, DbIterator child2) {
        // some code goes here
        this.p = p;
        this.child1 = child1;
        this.child2 = child2;
        buckets = new HashMap<>();
    }

    public JoinPredicate getJoinPredicate() {
        // some code goes here
        return p;
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        return TupleDesc.merge(child1.getTupleDesc(), child2.getTupleDesc());
    }

    public String getJoinField1Name() {
        // some code goes here
        return child1.getTupleDesc().getFieldName(p.getField1());
    }

    public String getJoinField2Name() {
        // some code goes here
        return child2.getTupleDesc().getFieldName(p.getField2());
    }

    public void open() throws DbException, NoSuchElementException,
            TransactionAbortedException {
        // some code goes here
        super.open();
        child1.open();
        child2.open();
        while (child1.hasNext()) {
            Tuple t = child1.next();
            Field key = t.getField(p.getField1());
            if (!buckets.containsKey(key)) {
                ArrayList<Tuple> bucket = new ArrayList<>();
                buckets.put(key, bucket);
            }
            buckets.get(key).add(t);
        }
    }

    public void close() {
        // some code goes here
        super.close();
        child1.close();
        child2.close();
        buckets.clear();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        close();
        open();
    }

    transient Iterator<Tuple> listIt = null;

    /**
     * Returns the next tuple generated by the join, or null if there are no
     * more tuples. Logically, this is the next tuple in r1 cross r2 that
     * satisfies the join predicate. There are many possible implementations;
     * the simplest is a nested loops join.
     * <p>
     * Note that the tuples returned from this particular implementation of Join
     * are simply the concatenation of joining tuples from the left and right
     * relation. Therefore, there will be two copies of the join attribute in
     * the results. (Removing such duplicate columns can be done with an
     * additional projection operator if needed.)
     * <p>
     * For example, if one tuple is {1,2,3} and the other tuple is {1,5,6},
     * joined on equality of the first column, then this returns {1,2,3,1,5,6}.
     *
     * @return The next matching tuple.
     * @see JoinPredicate#filter
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        if (listIt == null || !listIt.hasNext())
            while (child2.hasNext()) {
                t2 = child2.next();
                Field key = t2.getField(p.getField2());
                if (buckets.containsKey(key)) {
                    listIt = buckets.get(key).iterator();
                    break;
                }
            }
        if (listIt == null || !listIt.hasNext()) return null;
        t1 = listIt.next();
        if (p.filter(t1, t2)) {
            Tuple t = new Tuple(TupleDesc.merge(t1.getTupleDesc(), t2.getTupleDesc()));
            int k = 0;
            for (int i = 0; i < t1.getTupleDesc().numFields(); i++, k++)
                t.setField(k, t1.getField(i));
            for (int i = 0; i < t2.getTupleDesc().numFields(); i++, k++)
                t.setField(k, t2.getField(i));
            return t;
        }
        return null;
    }

    @Override
    public DbIterator[] getChildren() {
        // some code goes here
        DbIterator[] children = new DbIterator[2];
        children[0] = child1;
        children[1] = child2;
        return null;
    }

    @Override
    public void setChildren(DbIterator[] children) {
        // some code goes here
        child1 = children[0];
        child2 = children[1];
    }

}
