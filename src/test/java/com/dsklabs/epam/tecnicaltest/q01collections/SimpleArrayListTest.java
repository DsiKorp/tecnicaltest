package com.dsklabs.epam.tecnicaltest.q01collections;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Capa UNIT de la pregunta 1. Sin Spring: es lógica pura y tiene que correr en milisegundos.
 */
class SimpleArrayListTest {

    @Test
    @DisplayName("Añadir y leer por índice conserva el orden de inserción")
    void addAndGet() {
        SimpleArrayList<String> list = new SimpleArrayList<>();

        list.add("a");
        list.add("b");
        list.add("c");

        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0)).isEqualTo("a");
        assertThat(list.get(2)).isEqualTo("c");
    }

    @Test
    @DisplayName("El array de backing crece de forma geométrica al superarse la capacidad")
    void growsGeometrically() {
        SimpleArrayList<Integer> list = new SimpleArrayList<>(2);

        assertThat(list.capacity()).isEqualTo(2);
        list.add(1);
        list.add(2);
        assertThat(list.capacity()).isEqualTo(2);

        list.add(3);
        assertThat(list.capacity()).isGreaterThan(2);
        assertThat(list.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Insertar en medio desplaza los elementos posteriores")
    void addAtIndexShiftsElements() {
        SimpleArrayList<String> list = new SimpleArrayList<>();
        list.add("a");
        list.add("c");

        list.add(1, "b");

        assertThat(list.toArray()).containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("Eliminar por índice desplaza los elementos posteriores y libera la referencia")
    void removeAtIndex() {
        SimpleArrayList<String> list = new SimpleArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");

        String removed = list.remove(1);

        assertThat(removed).isEqualTo("b");
        assertThat(list.toArray()).containsExactly("a", "c");
        assertThat(list.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Eliminar por valor devuelve false si no existe")
    void removeByValue() {
        SimpleArrayList<String> list = new SimpleArrayList<>();
        list.add("a");

        assertThat(list.remove("z")).isFalse();
        assertThat(list.remove("a")).isTrue();
        assertThat(list).isEmpty();
    }

    @Test
    @DisplayName("El iterador es fail-fast: detecta modificación durante la iteración")
    void iteratorIsFailFast() {
        SimpleArrayList<Integer> list = new SimpleArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        assertThatThrownBy(() -> {
            Iterator<Integer> iterator = list.iterator();
            iterator.next();
            list.add(4);          // modificación estructural durante la iteración
            iterator.next();
        }).isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    @DisplayName("El iterador recorre todos los elementos en orden")
    void iteratorTraversesInOrder() {
        SimpleArrayList<Integer> list = new SimpleArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add(i);
        }

        StringBuilder sb = new StringBuilder();
        for (int value : list) {
            sb.append(value);
        }

        assertThat(sb.toString()).isEqualTo("01234");
    }

    @Test
    @DisplayName("Los índices fuera de rango lanzan IndexOutOfBoundsException")
    void outOfBounds() {
        SimpleArrayList<String> list = new SimpleArrayList<>();
        list.add("a");

        assertThatThrownBy(() -> list.get(1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> list.get(-1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> list.add(5, "x")).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("toArray devuelve una copia, no el array interno")
    void toArrayIsACopy() {
        SimpleArrayList<String> list = new SimpleArrayList<>();
        list.add("a");

        Object[] array = list.toArray();
        array[0] = "modificado";

        assertThat(list.get(0)).isEqualTo("a");
    }
}
