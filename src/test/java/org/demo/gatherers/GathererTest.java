package org.demo.gatherers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Gatherer;
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

    @Test
    void testWindowSliced() {
        List<List<Integer>> actual = Stream.of(1, 2, 3, 4)
                .gather(Gatherers.windowSliding(2))
                .toList();

        List<List<Integer>> expected = List.of(
                List.of(1, 2),
                List.of(2, 3),
                List.of(3, 4)
        );

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testCustomMapGatherer() {
        Gatherer.Integrator<Void, Integer, Integer> integrator =
                (unused, element, result) -> result.push(element * 10);

        Gatherer<Integer, ?, Integer> mapGatherer = Gatherer.of(integrator);


        List<Integer> actual = Stream.of(1, 2, 3, 4)
                .gather(mapGatherer)
                .toList();
        Assertions.assertEquals(List.of(10, 20, 30, 40), actual);
    }

    @Test
    void testCustomMapMultiGatherer() {

        Gatherer.Integrator<Void, List<Integer>, Integer> integrator =
                (unused, element, downstream) -> {
                    element.forEach(downstream::push);
                    return !downstream.isRejecting();
                };
        Gatherer<List<Integer>, Void, Integer> mapMultiIntegrator = Gatherer.of(integrator);

        var actual = Stream.of(List.of(1, 2), List.of(3, 4), List.of(5))
//                .gather(mapMultiIntegrator)
                .gather(mapMulti(Iterable::forEach))
                .toList();

        var expected = List.of(1, 2, 3, 4, 5);

        Assertions.assertEquals(expected, actual);
    }

    <T, R> Gatherer<T, ?, R> mapMulti(BiConsumer<? super T, Consumer<? super R>> mapper) {
        return Gatherer.of(
                (unused, el, downstream) -> {
                    mapper.accept(el, downstream::push);
                    return !downstream.isRejecting();
                }
        );
    }
}
