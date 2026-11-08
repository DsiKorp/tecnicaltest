package com.dsklabs.epam.tecnicaltest.q01collections;

import java.util.Arrays;

/**
 * Lista de enteros sin boxing: {@code int[]} puro.
 *
 * <p>Esta clase es la respuesta literal a "implementa un ArrayList usando solo primitivas".
 *
 * <p><b>Por qué importa el boxing:</b> un {@code int} ocupa 4 bytes en el array; un
 * {@code Integer} es un <b>objeto</b> (16 bytes de cabecera en HotSpot: mark word + klass
 * pointer, más el valor). Un {@code ArrayList<Integer>} con 1.000.000 de elementos:
 * <ul>
 *   <li>referencias del array: 8 bytes × 1M = 8 MB (con compressed oops)</li>
 *   <li>los {@code Integer}: 16 bytes × 1M = 16 MB</li>
 *   <li>el array de {@code int}: 4 bytes × 1M = 4 MB</li>
 * </ul>
 * Es decir: <b>6 veces más memoria</b> y además un indirección extra en cada lectura
 * (el acceso a un objeto necesita saltar al heap).
 *
 * <p>Ese es el trade-off real de los genéricos en Java: {@code List<Integer>} es cómodo pero
 * paga un coste constante por elemento.
 */
public class IntArrayList {

    private static final int DEFAULT_CAPACITY = 10;

    private int[] values;
    private int size;

    public IntArrayList() {
        this(DEFAULT_CAPACITY);
    }

    public IntArrayList(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Capacidad inicial negativa: " + initialCapacity);
        }
        this.values = new int[initialCapacity];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return values.length;
    }

    public void add(int value) {
        ensureCapacity(size + 1);
        values[size++] = value;
    }

    public int get(int index) {
        checkIndex(index);
        return values[index];
    }

    public int set(int index, int value) {
        checkIndex(index);
        int previous = values[index];
        values[index] = value;
        return previous;
    }

    public int remove(int index) {
        checkIndex(index);
        int removed = values[index];
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(values, index + 1, values, index, moved);
        }
        size--;
        return removed;
    }

    public int sum() {
        int total = 0;
        for (int i = 0; i < size; i++) {
            total += values[i];
        }
        return total;
    }

    public int[] toArray() {
        return Arrays.copyOf(values, size);
    }

    private void ensureCapacity(int required) {
        if (required > values.length) {
            int newCapacity = Math.max(DEFAULT_CAPACITY, values.length * 2);
            while (newCapacity < required) {
                newCapacity *= 2;
            }
            values = Arrays.copyOf(values, newCapacity);
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice " + index + " fuera de rango [0, " + size + ")");
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }
}
