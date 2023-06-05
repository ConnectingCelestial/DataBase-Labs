package simpledb;

/**
 * A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram extends Histogram{

    private int[] heights;

    private int min;

    private int max;

    private double width;

    private int numTuples;

    /**
     * Create a new IntHistogram.
     * <p>
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     * <p>
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * <p>
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't
     * simply store every value that you see in a sorted list.
     *
     * @param buckets The number of buckets to split the input value into.
     * @param min     The minimum integer value that will ever be passed to this class for histogramming
     * @param max     The maximum integer value that will ever be passed to this class for histogramming
     */
    public IntHistogram(int buckets, int min, int max) {
        // some code goes here
        this.heights = new int[buckets];
        this.min = min;
        this.max = max;
        width = (max - min) / (double) buckets;
        numTuples = 0;
    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     *
     * @param v Value to add to the histogram
     */
    @Override
    public void addValue(int v) {
        // some code goes here
        if (v < min || v > max) return;
        heights[locateV(v)]++;
        numTuples++;
    }

    private int locateV(int v) {
        if (v < min || v > max) return -1;
        if (v == max) return heights.length - 1;
        else return (int) ((v - min) / width);
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * <p>
     * For example, if "op" is "GREATER_THAN" and "v" is 5,
     * return your estimate of the fraction of elements that are greater than 5.
     *
     * @param op Operator
     * @param v  Value
     * @return Predicted selectivity of this particular operator and value
     */
    @Override
    public double estimateSelectivity(Predicate.Op op, int v) {
        // some code goes here
        if (v < min)
            if (op.equals(Predicate.Op.GREATER_THAN) || op.equals(Predicate.Op.GREATER_THAN_OR_EQ) || op.equals(Predicate.Op.NOT_EQUALS))
                return 1;
            else return 0;
        if (v > max)
            if (op.equals(Predicate.Op.LESS_THAN) || op.equals(Predicate.Op.LESS_THAN_OR_EQ) || op.equals(Predicate.Op.NOT_EQUALS))
                return 1;
            else return 0;
        int bucket = locateV(v);
        double res = 0;
        switch (op) {
            case EQUALS:
                return (double) heights[bucket] / numTuples;
            case GREATER_THAN:
                res = (1 + bucket - v / width) * heights[bucket];
                for (int i = bucket + 1; i < heights.length; i++) {
                    res += heights[i];
                }
                return res / numTuples;
            case GREATER_THAN_OR_EQ:
                for (int i = bucket; i < heights.length; i++) {
                    res += heights[i];
                }
                return res / numTuples;
            case LESS_THAN:
                res = (v / width - bucket) * heights[bucket];
                for (int i = 0; i < bucket; i++) {
                    res += heights[i];
                }
                return res / numTuples;
            case LESS_THAN_OR_EQ:
                for (int i = 0; i < bucket + 1; i++) {
                    res += heights[i];
                }
                return res / numTuples;
            case NOT_EQUALS:
                return 1 - (double) heights[bucket] / numTuples;
        }
        return 0;
    }

    /**
     * @return the average selectivity of this histogram.
     * <p>
     * This is not an indispensable method to implement the basic
     * join optimization. It may be needed if you want to
     * implement a more efficient optimization
     */
    public double avgSelectivity() {
        // some code goes here
        double res = 0;
        for (int height : heights) {
            res += height;
        }
        return res / numTuples;
    }

    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        // some code goes here
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < heights.length; i++) {
            s.append("bucket").append(i).append(":").append(heights[i]).append(";");
        }
        return s.toString();
    }
}
