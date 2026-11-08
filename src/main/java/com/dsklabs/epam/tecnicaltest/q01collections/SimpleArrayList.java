package com.dsklabs.epam.tecnicaltest.q01collections;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implementación de una lista basada en array, escrita desde cero (sin usar
 * {@code java.util.ArrayList} ni ninguna otra colección de la JDK).
 *
 * <p><b>Por qué la interviewer pide esto:</b> para ver si el candidato entiende de verdad el
 * coste de cada operación. Esta es la estructura que hay dentro de {@code ArrayList}:
 *
 * <ul>
 *   <li><b>Acceso por índice: O(1)</b> — es una aritmética sobre el índice del array.</li>
 *   <li><b>Insertar en medio: O(n)</b> — hay que desplazar los elementos con
 *       {@code System.arraycopy}.</li>
 *   <li><b>Añadir al final: amortizado O(1)</b> — cuando el array se llena se duplica su
 *       capacidad (aquí ×1,5, como el JDK) y se copia el contenido: por eso es <i>amortizado</i>
 *       y no O(1) en el peor caso.</li>
 * </ul>
 *
 * <p>Incluye también el iterador <i>fail-fast</i>: si la lista se modifica mientras se itera, se
 * lanza {@link ConcurrentModificationException}. Es el mismo mecanismo del JDK (campo
 * {@code modCount}) y es una decisión de diseño que vale la pena explicar en la entrevista:
 * detecta errores de uso en vez de devolver datos inconsistentes.
 *
 * @param <E> tipo de los elementos
 */
public class SimpleArrayList<E> implements Iterable<E> {

    private static final int DEFAULT_CAPACITY = 10;
    private static final float GROWTH_FACTOR = 1.5f;

    /** Array de backing. Slots por encima de {@code size} valen {@code null}. */
    private Object[] elements;

    /** Número de elementos realmente almacenados. */
    private int size;

    /**
     * Contador de modificaciones estructurales. El iterador lo guarda al crearse y lo compara
     * en cada {@code next()}: si cambia, alguien modificó la lista durante la iteración.
     */
    private int modCount;

    public SimpleArrayList() {
        this(DEFAULT_CAPACITY);
    }

    public SimpleArrayList(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Capacidad inicial negativa: " + initialCapacity);
        }
        this.elements = new Object[initialCapacity];
    }

    /** Constructor de copia. */
    public SimpleArrayList(SimpleArrayList<E> other) {
        this.elements = Arrays.copyOf(other.elements, other.size);
        this.size = other.size;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return elements.length;
    }

    /** Añade al final. O(1) amortizado. */
    public boolean add(E element) {
        ensureCapacity(size + 1);
        elements[size++] = element;
        modCount++;
        return true;
    }

    /** Inserta en una posición. O(n) por el desplazamiento. */
    public void add(int index, E element) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Índice " + index + " fuera de rango [0, " + size + "]");
        }
        ensureCapacity(size + 1);
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = element;
        size++;
        modCount++;
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        checkIndex(index);
        return (E) elements[index];
    }

    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        checkIndex(index);
        E previous = (E) elements[index];
        elements[index] = element;
        return previous;
    }

    /** Elimina por posición. O(n) por el desplazamiento. */
    @SuppressWarnings("unchecked")
    public E remove(int index) {
        checkIndex(index);
        E removed = (E) elements[index];
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(elements, index + 1, elements, index, moved);
        }
        elements[--size] = null;
        modCount++;
        return removed;
    }

    /** Elimina por valor. O(n). Devuelve true si se eliminó. */
    public boolean remove(Object o) {
        int index = indexOf(o);
        if (index < 0) {
            return false;
        }
        remove(index);
        return true;
    }

    public int indexOf(Object o) {
        for (int i = 0; i < size; i++) {
            if (o == null ? elements[i] == null : o.equals(elements[i])) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public void clear() {
        Arrays.fill(elements, 0, size, null);
        size = 0;
        modCount++;
    }

    public Object[] toArray() {
        return Arrays.copyOf(elements, size);
    }

    private void ensureCapacity(int required) {
        if (required <= elements.length) {
            return;
        }
        int newCapacity = Math.max(DEFAULT_CAPACITY, (int) (elements.length * GROWTH_FACTOR) + 1);
        while (newCapacity < required) {
            newCapacity = (int) (newCapacity * GROWTH_FACTOR) + 1;
        }
        elements = Arrays.copyOf(elements, newCapacity);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice " + index + " fuera de rango [0, " + size + ")");
        }
    }

    /**
     * Iterador fail-fast: detecta modificaciones de la lista durante la iteración.
     *
     * <p>Es exactamente el mismo compromiso que hace el JDK: es más barato que hacer el
     * iterador "correcto" y convierte un bug silencioso en un error explícito.
     */
    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {

            private int cursor;
            private int expectedModCount = modCount;
            private boolean canRemove;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException(
                            "La lista fue modificada durante la iteración (modCount esperado "
                                    + expectedModCount + ", actual " + modCount + ")");
                }
                if (cursor >= size) {
                    throw new NoSuchElementException("No quedan elementos");
                }
                canRemove = true;
                return (E) elements[cursor++];
            }

            @Override
            public void remove() {
                if (!canRemove) {
                    throw new IllegalStateException("remove() sin una llamada previa a next()");
                }
                SimpleArrayList.this.remove(--cursor);
                canRemove = false;
                expectedModCount = modCount;
            }
        };
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }
}
