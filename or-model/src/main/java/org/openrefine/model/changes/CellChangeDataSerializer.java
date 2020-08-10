
package org.openrefine.model.changes;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.openrefine.model.Cell;
import org.openrefine.util.ParsingUtilities;

public class CellChangeDataSerializer implements ChangeDataSerializer<Cell> {

    private static final long serialVersionUID = 606360403156779037L;

    @Override
    public String serialize(Cell changeDataItem) {
        try {
            return ParsingUtilities.mapper.writeValueAsString(changeDataItem);
        } catch (JsonProcessingException e) {
            // does not happen, Cells are always serializable
            return null;
        }
    }

    @Override
    public Cell deserialize(String serialized) throws IOException {
        return ParsingUtilities.mapper.readValue(serialized, Cell.class);
    }

}
