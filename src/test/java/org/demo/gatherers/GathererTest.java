package org.demo.gatherers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

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
    void testConsecutiveGrouping()
    {
        var input = Stream.of(1, 1, 2, 2, 3, 3, 3, 4, 4);

        var actualOutput = input
                .gather(groupConsecutive())
                .toList();

        var expectedOutput = List.of(
                List.of(1, 1),
                List.of(2, 2),
                List.of(3, 3, 3),
                List.of(4, 4)
        );
        Assertions.assertEquals(expectedOutput, actualOutput);
    }

    private Gatherer<? super Integer, List<Integer>, List<Integer>> groupConsecutive() {
        return Gatherer.ofSequential(
                ArrayList::new,
                (state, element, downstream) -> {
                    if (!state.isEmpty() && !Objects.equals(state.getLast(), element)) {
                        downstream.push(List.copyOf(state));
                        state.clear();
                    }
                    state.add(element);
                    return true;
                },(objects, downstream) -> downstream.push(objects)
        );
    }

    @Test
    void testAdjacentPairs() {
        var actual = Stream.of(1, 2, 3, 4, 5)
                .gather(gatherAdjacent())
                .toList();

        var expected = List.of(
                List.of(1, 2),
                List.of(2, 3),
                List.of(3, 4),
                List.of(4, 5)
        );

        Assertions.assertEquals(expected, actual);
    }

    private Gatherer<? super Integer, List<Integer>, List<Integer>> gatherAdjacent() {
        return Gatherer.ofSequential(
                ArrayList::new,
                (state, element, downstream) -> {
                    state.addLast(element);
                    if (state.size() == 2) {
                     downstream.push(List.copyOf(state));
                     state.removeFirst();
                 }
                    return true;
                }
        );
    }

    @Test
    void testLastN()
    {
        var input = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        var actual = input.gather(last(1))
                .toList();
        var expected = List.of(10);

        Assertions.assertEquals(expected, actual);

    }

    private Gatherer<? super Integer, List<Integer>, Integer> last(int n) {
        return Gatherer.ofSequential(
                ArrayList::new,
                (state, element, _) -> {
                    if (state.size() == n) {
                        state.add(element);
                        state.removeFirst();
                    } else {
                        state.add(element);
                    }
                    return true;
                }, (state, downstream) -> {
                    for (var each: state) {
                        downstream.push(each);
                    }
                }
        );
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
    void testDeduplicate() {
        class LastSeen {Integer lastSeen;}
        Gatherer.Integrator<LastSeen, Integer, Integer> integrator =
                (state, element, result) -> {
            if (!Objects.equals(state.lastSeen, element)) {
                result.push(element);
                state.lastSeen = element;
            }
            return true;
            };
        var dedup = Gatherer.ofSequential(LastSeen::new, integrator);



        var actual = Stream.of(1, 2, 3, 3, 4, 5, 5, 5, 6, 7, 5, 7, 0, 1, 1, 8 )
                .gather(dedup)
                .toList();

        List<Integer> expected = List.of(1, 2, 3, 4, 5, 6, 7, 5, 7, 0, 1, 8);
        Assertions.assertEquals(expected, actual);
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
