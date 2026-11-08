package com.dsklabs.epam.tecnicaltest.q01collections;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Capa UNIT de la pregunta 1, para la lista doblemente enlazada. */
class SimpleLinkedListTest {

    @Test
    @DisplayName("Añadir al final y leer por índice conserva el orden")
    void addAndGet() {
        SimpleLinkedList<String> list = new SimpleLinkedList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0)).isEqualTo("a");
        assertThat(list.get(1)).isEqualTo("b");
        assertThat(list.get(2)).isEqualTo("c");
    }

    @Test
    @DisplayName("Insertar en medio re-liga los punteros vecinos sin desplazar nada")
    void addAtIndex() {
        SimpleLinkedList<String> list = new SimpleLinkedList<>();
        list.add("a");
        list.add("c");

        list.add(1, "b");

        assertThat(list.toString()).isEqualTo("[a, b, c]");
        assertThat(list.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("addFirst y addLast mantienen head y tail coherentes")
    void headAndTailAreMaintained() {
        SimpleLinkedList<String> list = new SimpleLinkedList<>();

        list.addFirst("b");
        list.addFirst("a");
        list.addLast("c");

        assertThat(list.toString()).isEqualTo("[a, b, c]");
        assertThat(list.peek()).isEqualTo("a");
    }

    @Test
    @DisplayName("removeFirst y removeLast funcionan en los dos extremos")
    void removeFromBothEnds() {
        SimpleLinkedList<String> list = new SimpleLinkedList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        assertThat(list.removeFirst()).isEqualTo("a");
        assertThat(list.removeLast()).isEqualTo("c");
        assertThat(list.toString()).isEqualTo("[b]");
        assertThat(list.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("push/pop y offer/poll implementan el contrato Deque")
    void dequeOperations() {
        SimpleLinkedList<String> list = new SimpleLinkedList<>();

        // push = addFirst: la pila crece por la cabeza.
        list.push("a");
        list.push("b");
        assertThat(list.pop()).isEqualTo("b");
        assertThat(list.pop()).isEqualTo("a");

        // offer = addLast, poll = removeFirst: la cola FIFO.
        list.offer("x");
        list.offer("y");
        assertThat(list.poll()).isEqualTo("x");
        assertThat(list.poll()).isEqualTo("y");
        assertThat(list.poll()).isNull();
    }

    @Test
    @DisplayName("pop sobre una lista vacía lanza NoSuchElementException")
    void popOnEmptyList() {
        SimpleLinkedList<String> list = new SimpleLinkedList<>();

        assertThatThrownBy(list::pop).isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    @DisplayName("El iterador es fail-fast")
    void iteratorIsFailFast() {
        SimpleLinkedList<Integer> list = new SimpleLinkedList<>();
        list.add(1);
        list.add(2);

        assertThatThrownBy(() -> {
            Iterator<Integer> iterator = list.iterator();
            iterator.next();
            list.add(3);
            iterator.next();
        }).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    @DisplayName("Recorrer la lista al revés desde nodeAt devuelve el mismo elemento")
    void nodeAtIsSymmetric() {
        SimpleLinkedList<Integer> list = new SimpleLinkedList<>();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        // nodeAt recorre desde tail cuando el índice está en la mitad superior.
        assertThat(list.get(0)).isEqualTo(0);
        assertThat(list.get(9)).isEqualTo(9);
        assertThat(list.get(5)).isEqualTo(5);
    }

    @Test
    @DisplayName("clear vacía la lista y suelta head y tail")
    void clear() {
        SimpleLinkedList<String> list = new SimpleLinkedList<>();
        list.add("a");
        list.add("b");

        list.clear();

        assertThat(list).isEmpty();
        assertThat(list.peek()).isNull();
        assertThat(list.poll()).isNull();
    }
}
