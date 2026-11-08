package com.dsklabs.epam.tecnicaltest.q05hashset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Service;

/**
 * Responde a la pregunta 5: <i>"¿Cómo se ordena un HashSet de enteros?"</i>.
 *
 * <p><b>La respuesta:</b> no se puede ordenar un {@code HashSet} en el sitio. Un {@code Set} no
 * tiene orden y un {@code HashSet} garantiza <i>ni siquiera</i> el orden de inserción: itera por
 * los cubos ("buckets") de su tabla hash. Ordenar exige siempre una estructura adicional
 * ({@code TreeSet}, {@code LinkedHashSet}) o copiar a una {@code List} y ordenarla.
 *
 * <p><b>El porqué (lo que hay que saber explicar):</b> un {@code HashSet} es, por dentro, un
 * {@code HashMap} con las claves y valores apuntando al mismo objeto. Para colocar un elemento el
 * JDK calcula su {@code hashCode()}, lo dispersa con {@code h ^ (h >>> 16)} y se queda el índice
 * del cubo con {@code & (tableSize - 1)}. Los elementos se guardan en el cubo y se recorren cubo a
 * cubo, así que <b>el orden de iteración es el orden de los cubos</b>: ni el de inserción ni el
 * natural.
 *
 * <p>Ese reparto por cubos es lo que da las dos propiedades del hash: <b>velocidad</b> (buscar es
 * O(1) en media) y <b>ordenación por hash</b> (los hashes bien repartidos reparten los elementos
 * de forma "aparentemente aleatoria" pero determinista para un conjunto dado).
 *
 * <p><b>El detalle fino que hay que saber (y que engaña a mucha gente):</b> con los enteros
 * consecutivos {@code 1..N} el orden de iteración de un {@code HashSet} <b>coincide con el
 * orden natural</b>, porque el índice de cubo es {@code valor & (tableSize - 1)} y, mientras la
 * tabla sea mayor que el mayor valor, sale el propio valor. Es una <b>casualidad del hash</b>, no
 * una garantía del {@code Set}: en cuanto los valores no son consecutivos, el recorrido por
 * cubos se desordena. Por eso la demo usa valores no consecutivos.
 *
 * @see SortStrategy
 */
@Service
public class HashSetOrderingService {

    /**
     * Capacidad por defecto de la tabla de un {@code HashSet} recién creado.
     *
     * <p>El JDK no expone el tamaño real de su tabla interna, así que todos los índices de cubo de
     * esta demo se calculan contra este valor. Es una aproximación honesta: si el conjunto tiene
     * más de 12 elementos ({@code 16 * 0.75}), la tabla se ha redimensionado a 32 (y a 64, 128...
     * según crezca) y los índices reales serían otros.
     */
    public static final int DEFAULT_TABLE_SIZE = 16;

    /** Factor de carga por defecto de un {@code HashMap}/{@code HashSet}. */
    public static final double DEFAULT_LOAD_FACTOR = 0.75;

    /** Umbral del JDK: un cubo con 8 entradas o más se convierte en un árbol rojo-negro. */
    public static final int TREEIFY_THRESHOLD = 8;

    /**
     * Capacidad mínima de la tabla para que el JDK prefiera <i>treeificar</i> el cubo.
     *
     * <p>Por debajo de 64, el JDK redimensiona la tabla en lugar de convertir el cubo en árbol.
     */
    public static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * Ordena (copiando) el conjunto recibido con la estrategia indicada.
     *
     * @param input    conjunto de entrada; se copia, nunca se modifica
     * @param strategy estrategia de ordenación
     * @return la lista ordenada
     */
    public List<Integer> sort(Set<Integer> input, SortStrategy strategy) {
        SortStrategy selected = strategy == null ? SortStrategy.TREE : strategy;

        return switch (selected) {
            case TREE -> new ArrayList<>(new TreeSet<>(input));
            case LINKED -> new ArrayList<>(new LinkedHashSet<>(input));
            case LIST -> {
                List<Integer> list = new ArrayList<>(input);
                Collections.sort(list);
                yield list;
            }
            case STREAM -> input.stream().sorted().toList();
        };
    }

    /**
     * Explica por qué el conjunto itera en el orden en que itera.
     *
     * @param input conjunto a analizar
     * @return mapa con el orden de iteración, el hash de cada elemento, su cubo y el
     *         comportamiento de la tabla
     */
    public Map<String, Object> explain(Set<Integer> input) {
        List<Integer> iterationOrder = new ArrayList<>(input);

        List<Map<String, Object>> elements = new ArrayList<>();
        Map<Integer, List<Integer>> byBucket = new LinkedHashMap<>();

        for (Integer value : iterationOrder) {
            int hash = value.hashCode();
            int bucket = bucketIndex(hash, DEFAULT_TABLE_SIZE);

            Map<String, Object> element = new LinkedHashMap<>();
            element.put("valor", value);
            element.put("hashCode", hash);
            element.put("indiceDeCubo", bucket);
            element.put("hashDisperso", hash ^ (hash >>> 16));
            elements.add(element);

            byBucket.computeIfAbsent(bucket, k -> new ArrayList<>()).add(value);
        }

        Map<String, Integer> bucketSummary = new LinkedHashMap<>();
        byBucket.forEach((bucket, values) -> bucketSummary.put(String.valueOf(bucket), values.size()));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ordenDeIteracion", iterationOrder);
        data.put("cantidadDeElementos", iterationOrder.size());
        data.put("elementos", elements);
        data.put("elementosPorCubo", bucketSummary);
        data.put("capacidadDeLaTabla", DEFAULT_TABLE_SIZE);
        data.put("factorDeCarga", DEFAULT_LOAD_FACTOR);
        data.put("umbralDeRedimensionado", (int) (DEFAULT_TABLE_SIZE * DEFAULT_LOAD_FACTOR));
        data.put("nota",
                "Los índices de cubo se calculan contra una tabla de " + DEFAULT_TABLE_SIZE
                        + " posiciones (la capacidad por defecto de un HashSet nuevo). El JDK no "
                        + "expone el tamaño real de su tabla interna: si el conjunto supera las "
                        + (int) (DEFAULT_TABLE_SIZE * DEFAULT_LOAD_FACTOR)
                        + " entradas, la tabla se redimensiona y los índices reales serían otros.");
        data.put("explicacion",
                "El orden de iteración de un HashSet es el ORDEN DE LOS CUBOS de su tabla hash, "
                        + "no el orden de inserción ni el natural (ordenado). Para cada elemento se "
                        + "calcula hashCode(), se dispersa con h ^ (h >>> 16) y se toma el cubo con "
                        + "& (tableSize - 1). Los elementos de un mismo cubo se recorren entre sí, "
                        + "así que dos elementos que colisionan mantienen su orden de inserción "
                        + "dentro del cubo. Con los enteros consecutivos 1..N el recorrido por "
                        + "cubos sale en orden natural (casualidad del hash, no una garantía).");

        return data;
    }

    /**
     * La colisión clásica de Java: {@code "Ea"} y {@code "FB"} tienen el mismo {@code hashCode()}.
     *
     * <p>Son cadenas <b>distintas</b> ({@code equals} devuelve {@code false}), así que las dos
     * sobreviven en el conjunto, pero caen en el mismo cubo. Dentro del cubo, la lista es una
     * lista enlazada y las dos se guardan en orden de inserción.
     *
     * @return los dos hashes, sus cubos y la explicación
     */
    public Map<String, Object> collisions() {
        String first = "Ea";
        String second = "FB";

        int firstHash = first.hashCode();
        int secondHash = second.hashCode();
        int bucket = bucketIndex(firstHash, DEFAULT_TABLE_SIZE);

        Set<String> set = new LinkedHashSet<>();
        set.add(first);
        set.add(second);

        Map<String, Object> firstEntry = new LinkedHashMap<>();
        firstEntry.put("cadena", first);
        firstEntry.put("hashCode", firstHash);
        firstEntry.put("indiceDeCubo", bucket);

        Map<String, Object> secondEntry = new LinkedHashMap<>();
        secondEntry.put("cadena", second);
        secondEntry.put("hashCode", secondHash);
        secondEntry.put("indiceDeCubo", bucketIndex(secondHash, DEFAULT_TABLE_SIZE));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("primera", firstEntry);
        data.put("segunda", secondEntry);
        data.put("mismoHashCode", firstHash == secondHash);
        data.put("mismoCubo", bucket == bucketIndex(secondHash, DEFAULT_TABLE_SIZE));
        data.put("igualesPorEquals", first.equals(second));
        data.put("ordenDeIteracion", new ArrayList<>(set));
        data.put("capacidadDeLaTabla", DEFAULT_TABLE_SIZE);
        data.put("explicacion",
                "\"Ea\" y \"FB\" son cadenas distintas (equals devuelve false) pero comparten "
                        + "hashCode() == " + firstHash + ", la histórica colisión del JDK. "
                        + "Por eso ambas caen en el mismo cubo (" + bucket + ") y conviven en el "
                        + "set: la colisión de hash no implica pérdida de elementos. Dentro del "
                        + "cubo se guardan en orden de inserción, así que al iterar se ve \"Ea\" "
                        + "antes que \"FB\".");

        return data;
    }

    /**
     * Demuestra la treeificación: 8 elementos que caen en el mismo cubo.
     *
     * <p>Con una tabla de 16 posiciones, los enteros {@code 1, 17, 33, 49, 65, 81, 97 y 113}
     * tienen todos {@code hash & 15 == 1}: ocho entradas en el cubo 1. Cuando un cubo llega a 8
     * entradas, el JDK lo convierte en un árbol rojo-negro ({@code treeifyBin}) para que las
     * búsquedas sigan siendo O(log n) en vez de degradarse a O(n).
     *
     * <p><b>Matiz importante:</b> {@code treeifyBin} solo treeifica si la tabla tiene 64
     * posiciones o más ({@code MIN_TREEIFY_CAPACITY}); por debajo redimensiona la tabla, porque
     * es más barato repartir de nuevo que construir un árbol.
     *
     * @return los elementos, su cubo y las reglas del JDK
     */
    public Map<String, Object> treeification() {
        List<Integer> colliding = List.of(1, 17, 33, 49, 65, 81, 97, 113);

        Set<Integer> set = new LinkedHashSet<>(colliding);

        List<Map<String, Object>> elements = new ArrayList<>();
        for (Integer value : colliding) {
            Map<String, Object> element = new LinkedHashMap<>();
            element.put("valor", value);
            element.put("hashCode", value.hashCode());
            element.put("indiceDeCubo", bucketIndex(value.hashCode(), DEFAULT_TABLE_SIZE));
            elements.add(element);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("elementos", elements);
        data.put("cantidadDeElementos", colliding.size());
        data.put("indiceDeCuboComun", bucketIndex(colliding.get(0), DEFAULT_TABLE_SIZE));
        data.put("capacidadDeLaTabla", DEFAULT_TABLE_SIZE);
        data.put("umbralDeTreeificacion", TREEIFY_THRESHOLD);
        data.put("capacidadMinimaParaTreeificar", MIN_TREEIFY_CAPACITY);
        data.put("umbralDeDesTreeificacion", 6);
        data.put("ordenDeIteracion", new ArrayList<>(set));
        data.put("explicacion",
                "Los ocho enteros 1, 17, 33, 49, 65, 81, 97 y 113 tienen (hash & 15) == 1: con una "
                        + "tabla de 16 posiciones los ocho caen en el cubo 1. Cuando un cubo "
                        + "alcanza " + TREEIFY_THRESHOLD + " entradas (TREEIFY_THRESHOLD), el JDK "
                        + "invoca treeifyBin y lo convierte en un árbol rojo-negro, de modo que "
                        + "buscar en él sigue siendo O(log n) en lugar de O(n). El matiz: "
                        + "treeifyBin solo treeifica si la tabla tiene " + MIN_TREEIFY_CAPACITY
                        + " posiciones o más; por debajo, el JDK redimensiona la tabla en vez de "
                        + "construir el árbol. Si el cubo baja a 6 entradas, el árbol se "
                        + "desconvierte (UNTREEIFY_THRESHOLD).");

        return data;
    }

    /**
     * Índice del cubo de un hash, tal y como lo calcula el JDK.
     *
     * <p>El JDK dispersa el hash con {@code h ^ (h >>> 16)} (para que los bits altos también
     * influyan) y toma los bits bajos con {@code & (tableSize - 1)}, que vale la máscara
     * porque la capacidad siempre es una potencia de dos.
     *
     * @param hash      hashCode del elemento
     * @param tableSize tamaño de la tabla (potencia de dos)
     * @return el índice del cubo, entre 0 y {@code tableSize - 1}
     */
    private int bucketIndex(int hash, int tableSize) {
        return (hash ^ (hash >>> 16)) & (tableSize - 1);
    }
}
