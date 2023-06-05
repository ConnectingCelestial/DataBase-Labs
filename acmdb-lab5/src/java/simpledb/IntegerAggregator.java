package simpledb;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbField;

    private Type gbFieldType;

    private int afield;

    private Op what;

    private HashMap<Field, Integer> resSet = new HashMap<>();

    private HashMap<Field, Integer> numSet = new HashMap<>();

    /**
     * Aggregate constructor
     *
     * @param gbfield     the 0-based index of the group-by field in the tuple, or
     *                    NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null
     *                    if there is no grouping
     * @param afield      the 0-based index of the aggregate field in the tuple
     * @param what        the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        this.gbField = gbfield;
        this.gbFieldType = gbfieldtype;
        this.afield = afield;
        this.what = what;
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     *
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        Field group = null;
        if (gbField > -1)
            group = tup.getField(gbField);
        Integer val = ((IntField) tup.getField(afield)).getValue();
        switch (what) {
            case COUNT, SUM, AVG:
                if (resSet.containsKey(group)) {
                    numSet.put(group, numSet.get(group) + 1);
                    resSet.put(group, resSet.get(group) + val);
                } else {
                    numSet.put(group, 1);
                    resSet.put(group, val);
                }
                break;
            case MAX:
                if (!resSet.containsKey(group) || resSet.get(group) < val)
                    resSet.put(group, val);
                break;
            case MIN:
                if (!resSet.containsKey(group) || resSet.get(group) > val)
                    resSet.put(group, val);
                break;
        }
    }

    /**
     * Create a DbIterator over group aggregate results.
     *
     * @return a DbIterator whose tuples are the pair (groupVal, aggregateVal)
     * if using group, or a single (aggregateVal) if no grouping. The
     * aggregateVal is determined by the type of aggregate specified in
     * the constructor.
     */
    public DbIterator iterator() {
        // some code goes here
        ArrayList<Tuple> arr = new ArrayList<>();
        if (gbField > -1)
            switch (what) {
                case SUM, MAX, MIN:
                    for (Field f : resSet.keySet()) {
                        Tuple t = new Tuple(new TupleDesc(new Type[]{gbFieldType, Type.INT_TYPE}));
                        t.setField(0, f);
                        t.setField(1, new IntField(resSet.get(f)));
                        arr.add(t);
                    }
                    break;
                case COUNT:
                    for (Field f : numSet.keySet()) {
                        Tuple t = new Tuple(new TupleDesc(new Type[]{gbFieldType, Type.INT_TYPE}));
                        t.setField(0, f);
                        t.setField(1, new IntField(numSet.get(f)));
                        arr.add(t);
                    }
                    break;
                case AVG:
                    for (Field f : resSet.keySet()) {
                        Tuple t = new Tuple(new TupleDesc(new Type[]{gbFieldType, Type.INT_TYPE}));
                        t.setField(0, f);
                        t.setField(1, new IntField(resSet.get(f) / numSet.get(f)));
                        arr.add(t);
                    }
                    break;
            }
        else {
            switch (what) {
                case SUM, MAX, MIN:
                    for (Field f : resSet.keySet()) {
                        Tuple t = new Tuple(new TupleDesc(new Type[]{Type.INT_TYPE}));
                        t.setField(0, new IntField(resSet.get(f)));
                        arr.add(t);
                    }
                    break;
                case COUNT:
                    for (Field f : numSet.keySet()) {
                        Tuple t = new Tuple(new TupleDesc(new Type[]{Type.INT_TYPE}));
                        t.setField(0, new IntField(numSet.get(f)));
                        arr.add(t);
                    }
                    break;
                case AVG:
                    for (Field f : resSet.keySet()) {
                        Tuple t = new Tuple(new TupleDesc(new Type[]{Type.INT_TYPE}));
                        t.setField(0, new IntField(resSet.get(f) / numSet.get(f)));
                        arr.add(t);
                    }
                    break;
            }
        }
        return new TupleArrayIterator(arr);
    }

}
