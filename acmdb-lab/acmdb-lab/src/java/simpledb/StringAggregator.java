package simpledb;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbField;

    private Type gbFieldType;

    private int afield;

    private Op what;

    private HashMap<Field, Integer> resSet = new HashMap<>();

    /**
     * Aggregate constructor
     *
     * @param gbfield     the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield      the 0-based index of the aggregate field in the tuple
     * @param what        aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */

    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.gbField = gbfield;
        this.gbFieldType = gbfieldtype;
        this.afield = afield;
        this.what = what;
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     *
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        Field group = null;
        if (gbField > -1)
            group = tup.getField(gbField);
        if (resSet.containsKey(group))
            resSet.put(group, resSet.get(group) + 1);
        else resSet.put(group, 1);
    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal,
     * aggregateVal) if using group, or a single (aggregateVal) if no
     * grouping. The aggregateVal is determined by the type of
     * aggregate specified in the constructor.
     */
    public DbIterator iterator() {
        // some code goes here
        ArrayList<Tuple> arr = new ArrayList<>();
        if (gbField > -1)
            for (Field f : resSet.keySet()) {
                Tuple t = new Tuple(new TupleDesc(new Type[]{gbFieldType, Type.INT_TYPE}));
                t.setField(0, f);
                t.setField(1, new IntField(resSet.get(f)));
                arr.add(t);
            }
        else
            for (Field f : resSet.keySet()) {
                Tuple t = new Tuple(new TupleDesc(new Type[]{gbFieldType, Type.INT_TYPE}));
                t.setField(0, new IntField(resSet.get(f)));
                arr.add(t);
            }
        return new TupleArrayIterator(arr);
    }

}
