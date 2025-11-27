package org.demo.gatherers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * Unit test for simple App.
 */
public class GathererTest {

    @Test
    public void testWindowFixed() {

        var actual = Stream.of(1, 2, 3, 4)
                .gather(Gatherers.windowFixed(2))
                        .toList();

        Assertions.assertEquals(List.of(
                List.of(1, 2),
                List.of(3, 4)

        ), actual);

    }
}
