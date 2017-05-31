package indexingTopology.aggregator;

import static javafx.scene.input.KeyCode.V;

/**
 * Created by robert on 10/3/17.
 */
public class Sum<I extends Number> implements AggregationFunction<I, Double> {

    @Override
    public Double aggregateFunction(I value, Double originalA) {
        if (value == null || originalA == null)
            return value == null ? originalA : value.doubleValue();
        return (value.doubleValue() + originalA);
    }

    @Override
    public Double init() {
        return null;
    }
}
