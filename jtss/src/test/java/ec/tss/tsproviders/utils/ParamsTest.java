/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.tss.tsproviders.utils;

import com.google.common.collect.ImmutableMap;
import ec.tss.tsproviders.DataSet;
import ec.tss.tsproviders.DataSource;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.Test;

/**
 *
 * @author Philippe Charles
 */
public class ParamsTest {

    private <T> void assertBehavior(IParam<DataSource, T> p, T defaultValue, T newValue, String key, String newValueAsString) {
        assertBehavior(p, defaultValue, newValue, ImmutableMap.of(key, newValueAsString));
    }

    @SuppressWarnings("null")
    private <T> void assertBehavior(IParam<DataSource, T> param, T defaultValue, T newValue, Map<String, String> keyValues) {
        DataSource.Builder builder = DataSource.builder("", "");

        // NPE
        assertThatThrownBy(() -> param.get(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> param.set(null, defaultValue)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> param.set(null, newValue)).isInstanceOf(NullPointerException.class);

        // default value
        assertThat(param.defaultValue()).isEqualTo(defaultValue);

        // keys absent => default value
        DataSource emptyConfig = builder.clear().build();
        assertThat(param.get(emptyConfig)).isEqualTo(defaultValue);

        // keys present => new value
        builder.clear();
        keyValues.forEach(builder::put);
        DataSource normalConfig = builder.build();
        if (newValue instanceof double[]) {
            assertThat((double[]) param.get(normalConfig)).containsExactly((double[]) newValue);
        } else {
            assertThat(param.get(normalConfig)).isEqualTo(newValue);
        }

        // new value => keys present
        builder.clear();
        param.set(builder, newValue);
        DataSource newConfig = builder.build();
        keyValues.forEach((k, v) -> assertThat(newConfig.get(k)).isEqualTo(v));

        // default value => keys absent
        builder.clear();
        param.set(builder, defaultValue);
        assertThat(builder.build().getParams()).isEmpty();
    }

    @Test
    public void testOnString() {
        String d = "defaultValue";
        String n = "newValue";
        assertBehavior(Params.<DataSource>onString(d, "k"), d, n, "k", n);
    }

    @Test
    public void testOnFile() {
        File d = new File("d");
        File n = new File("x");
        assertBehavior(Params.<DataSource>onFile(d, "k"), d, n, "k", n.getPath());
    }

    @Test
    public void testOnEnum() {
        DataSet.Kind d = DataSet.Kind.SERIES;
        DataSet.Kind n = DataSet.Kind.COLLECTION;
        assertBehavior(Params.<DataSource, DataSet.Kind>onEnum(d, "k"), d, n, "k", n.name());
    }

    @Test
    public void testOnInteger() {
        Integer d = 123;
        Integer n = 456;
        assertBehavior(Params.<DataSource>onInteger(d, "k"), d, n, "k", n.toString());
    }

    @Test
    public void testOnLong() {
        Long d = 123l;
        Long n = 456l;
        assertBehavior(Params.<DataSource>onLong(d, "k"), d, n, "k", n.toString());
    }

    @Test
    public void testOnBoolean() {
        Boolean d = true;
        Boolean n = false;
        assertBehavior(Params.<DataSource>onBoolean(d, "k"), d, n, "k", n.toString());
    }

    @Test
    public void testOnCharset() {
        Charset d = StandardCharsets.US_ASCII;
        Charset n = StandardCharsets.UTF_8;
        assertBehavior(Params.<DataSource>onCharset(d, "k"), d, n, "k", n.name());
    }

    @Test
    public void testOnDataFormat() {
        DataFormat d = DataFormat.create(null, "yyyy-MM", null);
        DataFormat n1 = DataFormat.create(null, "dd-MM-yyyy", null);
        assertBehavior(Params.<DataSource>onDataFormat(d, "k1", "k2"), d, n1, ImmutableMap.of("k1", "", "k2", "dd-MM-yyyy"));
        assertBehavior(Params.<DataSource>onDataFormat(d, "k1", "k2"), d, n1, ImmutableMap.of("k1", "", "k2", "dd-MM-yyyy", "numberPattern", ""));
        assertBehavior(Params.<DataSource>onDataFormat(d, "k1", "k2", "k3"), d, n1, ImmutableMap.of("k1", "", "k2", "dd-MM-yyyy"));
        assertBehavior(Params.<DataSource>onDataFormat(d, "k1", "k2", "k3"), d, n1, ImmutableMap.of("k1", "", "k2", "dd-MM-yyyy", "k3", ""));
        DataFormat n2 = DataFormat.create(null, "dd-MM-yyyy", "#");
        assertBehavior(Params.<DataSource>onDataFormat(d, "k1", "k2", "k3"), d, n2, ImmutableMap.of("k1", "", "k2", "dd-MM-yyyy", "k3", "#"));
    }

    @Test
    public void testOnDoubleArray() {
        double[] d = {1, 2, 3};
        double[] n = {4, 5, 6};
        assertBehavior(Params.<DataSource>onDoubleArray("k", d), d, n, "k", Arrays.toString(n));
    }
}
