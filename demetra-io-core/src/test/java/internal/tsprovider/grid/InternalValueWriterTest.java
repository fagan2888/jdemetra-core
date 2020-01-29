/*
 * Copyright 2017 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package internal.tsprovider.grid;

import demetra.tsprovider.grid.GridDataType;
import demetra.tsprovider.grid.GridLayout;
import demetra.tsprovider.grid.GridOutput;
import demetra.tsprovider.util.ObsFormat;
import static internal.tsprovider.grid.InternalValueWriter.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Locale;
import org.junit.Test;
import test.tsprovider.grid.ArrayGridInput;
import test.tsprovider.grid.ArrayGridOutput;
import static test.tsprovider.grid.Data.*;
import static org.assertj.core.api.Assertions.*;

/**
 *
 * @author Philippe Charles
 */
public class InternalValueWriterTest {

    @Test
    public void testOnNull() throws IOException {
        InternalValueWriter<Object> x = onNull();

        Object[][] data = {
            {null, JAN_2010, FEB_2010, MAR_2010},
            {"S1", 3.14, 4.56, 7.89}
        };

        ArrayGridInput in = ArrayGridInput.of(data);

        ArrayGridOutput out = new ArrayGridOutput(GridLayout.VERTICAL, EnumSet.allOf(GridDataType.class));
        try (GridOutput.Stream stream = out.open("test", in.getRowCount(), in.getColumnCount())) {
            for (int i = 0; i < in.getRowCount(); i++) {
                for (int j = 0; j < in.getColumnCount(i); j++) {
                    x.write(stream, in.getValue(i, j));
                }
                stream.writeEndOfRow();
            }
        }

        assertThat(out.getData().get("test"))
                .containsExactly(
                        new Object[][]{
                            {null, null, null, null},
                            {null, null, null, null}
                        });
    }

    @Test
    public void testOnDateTime() throws IOException {
        InternalValueWriter<LocalDateTime> x = onDateTime();

        ArrayGridOutput out = new ArrayGridOutput(GridLayout.VERTICAL, EnumSet.allOf(GridDataType.class));
        try (GridOutput.Stream stream = out.open("test", 1, 2)) {
            x.write(stream, JAN_2010);
            x.write(stream, null);
        }

        assertThat(out.getData().get("test")).containsExactly(new Object[]{JAN_2010, null});
    }

    @Test
    public void testOnDouble() throws IOException {
        InternalValueWriter<Double> x = onDouble();

        ArrayGridOutput out = new ArrayGridOutput(GridLayout.VERTICAL, EnumSet.allOf(GridDataType.class));
        try (GridOutput.Stream stream = out.open("test", 1, 2)) {
            x.write(stream, 3.14);
            x.write(stream, null);
        }

        assertThat(out.getData().get("test")).containsExactly(new Object[]{3.14, null});
    }

    @Test
    public void testOnString() throws IOException {
        InternalValueWriter<String> x = onString();

        ArrayGridOutput out = new ArrayGridOutput(GridLayout.VERTICAL, EnumSet.allOf(GridDataType.class));
        try (GridOutput.Stream stream = out.open("test", 1, 2)) {
            x.write(stream, "S1");
            x.write(stream, null);
        }

        assertThat(out.getData().get("test")).containsExactly(new Object[]{"S1", null});
    }

    @Test
    public void testOnStringFormatter() throws IOException {
        ObsFormat f = ObsFormat.of(Locale.ROOT, "yyyy-MM-dd", "");

        ArrayGridOutput out = new ArrayGridOutput(GridLayout.VERTICAL, EnumSet.allOf(GridDataType.class));
        try (GridOutput.Stream stream = out.open("test", 1, 4)) {
            onStringFormatter(f.dateTimeFormatter()::formatAsString).write(stream, JAN_2010);
            onStringFormatter(f.dateTimeFormatter()::formatAsString).write(stream, null);
            onStringFormatter(f.numberFormatter()::formatAsString).write(stream, null);
            onStringFormatter(f.numberFormatter()::formatAsString).write(stream, 3.14);
        }

        assertThat(out.getData().get("test")).containsExactly(new Object[]{JAN_2010.format(DateTimeFormatter.ISO_DATE), null, null, "3.14"});
    }
}
