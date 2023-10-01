
package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.openrefine.model.ColumnId;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;

/**
 * A function which computes change data to be persisted to disk, to be later joined back to the project to produce the
 * new grid. This data might be serialized because it is volatile or expensive to compute.
 * <p>
 * The calls to the external resource can be batched by overriding {@link #getBatchSize()} to specify the size of
 * batches and {@link #callRowBatch(List, ColumnModel)} for the batch processing itself. In that case, the
 * {@link #call(long, Row, ColumnModel)} method's implementation can be omitted (by throwing NotImplementedException for
 * instance).
 * <p>
 * It is also possible to limit the number of concurrent calls to the producer (for instance for rate-limited resources)
 * by overriding {@link #getMaxConcurrency()}.
 *
 * @param <T>
 */
public interface RowChangeDataProducer<T> extends Serializable {

    /**
     * Compute the change data on a given row.
     */
    public T call(long rowId, Row row, ColumnModel columnModel);

    /**
     * Compute the change data on a batch of consecutive rows. This defaults to individual calls if the method is not
     * overridden.
     *
     * @param rows
     *            the list of rows to fetch change data on
     * @param columnModel
     * @return a list of the same size
     */
    public default List<T> callRowBatch(List<IndexedRow> rows, ColumnModel columnModel) {
        return rows.stream()
                .map(ir -> call(ir.getIndex(), ir.getRow(), columnModel))
                .collect(Collectors.toList());
    }

    /**
     * The size of batches this producer would like to be called on. Smaller batches can be submitted (for instance at
     * the end of a partition). Defaults to 1.
     */
    public default int getBatchSize() {
        return 1;
    }

    /**
     * The maximum number of concurrent calls to this change data producer. If 0, there is no limit to the concurrency.
     */
    public default int getMaxConcurrency() {
        return 0;
    }

    /**
     * The columns this producer depends on. If null is returned, we assume that it can rely on any columns. If a set of
     * columns is returned, the producer is only allowed to read columns that are listed in this set: the other columns
     * it is fed with might not reflect the state of the grid on which the producer was meant to be applied.
     */
    public default List<ColumnId> getColumnDependencies() {
        return null;
    }
}