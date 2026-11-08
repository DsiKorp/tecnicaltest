package com.dsklabs.epam.tecnicaltest.q01collections;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implementación de una lista doblemente enlazada, escrita desde cero.
 *
 * <p>Contrasta con {@link SimpleArrayList}:
 *
 * <ul>
 *   <li><b>Acceso por índice: O(n)</b> — hay que recorrer la cadena. Es el precio de no
 *       tener el array.</li>
 *   <li><b>Insertar/eliminar en medio: O(1)</b> — basta con re-ligar los punteros vecinos, sin
 *       desplazar nada.</li>
 *   <li><b>Añadir al final: O(1)</b> — se mantiene un puntero {@code tail}.</li>
 *   <li><b>Coste de memoria: más alto</b> — cada nodo guarda el valor <b>y dos punteros</b>.
 *       En un {@code int[]} un int ocupa 4 bytes; en la lista enlazada, cada entero ocupa
 *       4 bytes + 8 bytes del puntero siguiente + 8 del anterior, más el header del objeto.
 *       Por eso {@code LinkedList<Integer>} ocupa bastante más que {@code ArrayList<Integer>}.</li>
 * </ul>
 *
 * <p>Además implementa las operaciones de {@code Deque} ({@code push}, {@code pop}, {@code offer},
 * {@code poll}): es la diferencia que el artículo menciona — {@code LinkedList} es también una
 * cola FIFO, mientras que {@code ArrayList} no.
 *
 * @param <E> tipo de los elementos
 */
public class SimpleLinkedList<E> implements Iterable<E> {

    /** Nodo de la cadena. */
    private static final class Node<E> {
        private E value;
        private Node<E> next;
        private Node<E> prev;

        Node(E value) {
            this.value = value;
        }
    }

    /** Primer nodo real. */
    private Node<E> head;

    /** Último nodo real (permite O(1) en append y en push/pop). */
    private Node<E> tail;

    private int size;

    /** Contador fail-fast, igual que en {@link SimpleArrayList}. */
    private int modCount;

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    // ---------------------------------------------------------------- singly linked core

    public boolean add(E element) {
        addLast(element);
        return true;
    }

    public void add(int index, E element) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Índice " + index + " fuera de rango [0, " + size + "]");
        }
        if (index == size) {
            addLast(element);
            return;
        }
        Node<E> target = nodeAt(index);
        Node<E> inserted = new Node<>(element);
        inserted.prev = target.prev;
        inserted.next = target;
        target.prev.next = inserted;
        target.prev = inserted;
        size++;
        modCount++;
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) nodeAt(index).value;
    }

    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        Node<E> node = nodeAt(index);
        E previous = (E) node.value;
        node.value = element;
        return previous;
    }

    @SuppressWarnings("unchecked")
    public E remove(int index) {
        return (E) unlink(nodeAt(index));
    }

    public boolean remove(Object o) {
        for (Node<E> node = head; node != null; node = node.next) {
            if (o == null ? node.value == null : o.equals(node.value)) {
                unlink(node);
                return true;
            }
        }
        return false;
    }

    public int indexOf(Object o) {
        int index = 0;
        for (Node<E> node = head; node != null; node = node.next, index++) {
            if (o == null ? node.value == null : o.equals(node.value)) {
                return index;
            }
        }
        return -1;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
        modCount++;
    }

    // ---------------------------------------------------------------- Deque operations

    /** Inserta al principio. O(1). */
    public void addFirst(E element) {
        Node<E> node = new Node<>(element);
        node.next = head;
        if (head != null) {
            head.prev = node;
        }
        head = node;
        if (tail == null) {
            tail = node;
        }
        size++;
        modCount++;
    }

    /** Inserta al final. O(1) gracias al puntero {@code tail}. */
    public void addLast(E element) {
        Node<E> node = new Node<>(element);
        if (tail == null) {
            head = node;
            tail = node;
        } else {
            node.prev = tail;
            tail.next = node;
            tail = node;
        }
        size++;
        modCount++;
    }

    /** Saca el primero. O(1). Lanza {@link NoSuchElementException} si está vacía. */
    @SuppressWarnings("unchecked")
    public E removeFirst() {
        if (head == null) {
            throw new NoSuchElementException("La lista está vacía");
        }
        return (E) unlink(head);
    }

    /** Saca el último. O(1). */
    @SuppressWarnings("unchecked")
    public E removeLast() {
        if (tail == null) {
            throw new NoSuchElementException("La lista está vacía");
        }
        return (E) unlink(tail);
    }

    /** {@code Deque.push}: añade al principio (pila). */
    public void push(E element) {
        addFirst(element);
    }

    /** {@code Deque.pop}: saca el primero (pila). */
    public E pop() {
        return removeFirst();
    }

    /** {@code Deque.offer}: añade al final (cola). */
    public void offer(E element) {
        addLast(element);
    }

    /** {@code Deque.poll}: saca el primero (cola), o null si está vacía. */
    public E poll() {
        return head == null ? null : removeFirst();
    }

    /** {@code Deque.peek}: mira el primero sin sacarlo. */
    public E peek() {
        return head == null ? null : head.value;
    }

    // ---------------------------------------------------------------- internals

    /** Devuelve el nodo en la posición indicada. O(n) — el coste que no tiene el array. */
    private Node<E> nodeAt(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Índice " + index + " fuera de rango [0, " + size + ")");
        }
        Node<E> node = head;
        // Trick: si el índice está en la mitad superior, se recorre desde atrás.
        // Es la misma optimización que hace el JDK y mejora el caso "get(i) cerca del final".
        if (index > size / 2) {
            node = tail;
            for (int i = size - 1; i > index; i--) {
                node = node.prev;
            }
        } else {
            for (int i = 0; i < index; i++) {
                node = node.next;
            }
        }
        return node;
    }

    /** Desenlaza un nodo y devuelve su valor. */
    private E unlink(Node<E> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
        }
        E value = node.value;
        node.value = null;
        node.next = null;
        node.prev = null;
        size--;
        modCount++;
        return value;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {

            private Node<E> current = head;
            private int expectedModCount = modCount;
            private Node<E> lastReturned;
            private boolean canRemove;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                if (modCount != expectedModCount) {
                    throw new ConcurrentModificationException(
                            "La lista fue modificada durante la iteración (modCount esperado "
                                    + expectedModCount + ", actual " + modCount + ")");
                }
                if (current == null) {
                    throw new NoSuchElementException("No quedan elementos");
                }
                lastReturned = current;
                current = current.next;
                canRemove = true;
                return (E) lastReturned.value;
            }

            @Override
            public void remove() {
                if (!canRemove || lastReturned == null) {
                    throw new IllegalStateException("remove() sin una llamada previa a next()");
                }
                Node<E> next = lastReturned.next;
                unlink(lastReturned);
                lastReturned = next;
                canRemove = false;
                expectedModCount = modCount;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Node<E> node = head; node != null; node = node.next) {
            if (node != head) {
                sb.append(", ");
            }
            sb.append(node.value);
        }
        return sb.append(']').toString();
    }
}
