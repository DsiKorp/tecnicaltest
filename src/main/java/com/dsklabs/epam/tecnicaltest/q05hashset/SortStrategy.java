package com.dsklabs.epam.tecnicaltest.q05hashset;

/**
 * Las cuatro formas de "ordenar" un {@code HashSet}.
 *
 * <p><b>La respuesta corta a la pregunta:</b> un {@code Set} no tiene orden. Un {@code HashSet}
 * ni siquiera garantiza el <i>orden de inserción</i>. Ordenar exige, siempre, una estructura de
 * datos extra (o copiar a otra colección que sí sabe ordenarse).
 *
 * <p>Cada constante de este enum es una de esas estructuras extra:
 *
 * <ul>
 *   <li>{@link #TREE} — {@code TreeSet}: orden natural ({@code Comparable}).</li>
 *   <li>{@link #LINKED} — {@code LinkedHashSet}: conserva el orden de inserción.</li>
 *   <li>{@link #LIST} — {@code ArrayList} + {@code Collections.sort}: se copia y se ordena.</li>
 *   <li>{@link #STREAM} — {@code stream().sorted().toList()}: la vía funcional.</li>
 * </ul>
 */
public enum SortStrategy {

    /** Copia a un {@code TreeSet}: el resultado queda en orden natural (ascendente para enteros). */
    TREE("TreeSet", "Copia a un TreeSet: queda en orden natural (Comparable) de los elementos."),

    /** Copia a un {@code LinkedHashSet}: mantiene el orden de inserción, no ordena por valor. */
    LINKED("LinkedHashSet", "Copia a un LinkedHashSet: mantiene el orden de inserción, no ordena por valor."),

    /** Copia a un {@code ArrayList} y la ordena in-place con {@code Collections.sort}. */
    LIST("ArrayList + Collections.sort", "Copia a un ArrayList y lo ordena in-place con Collections.sort."),

    /** Ordena el flujo con {@code sorted()} y lo materializa con {@code toList()}. */
    STREAM("Stream.sorted().toList()", "Ordena el flujo con sorted() y lo materializa con toList().");

    private final String displayName;
    private final String description;

    SortStrategy(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /** Nombre legible de la estructura usada, para la respuesta HTTP. */
    public String displayName() {
        return displayName;
    }

    /** Explicación en español de qué hace exactamente esta estrategia. */
    public String description() {
        return description;
    }

    /**
     * Traduce el parámetro de la URL ({@code ?strategy=tree}) a la constante del enum.
     *
     * <p>Devuelve {@code null} si el valor no existe o no es válido: quien llama decide el
     * comportamiento por defecto (en este proyecto, {@link #TREE}).
     *
     * @param value texto recibido, puede ser {@code null}
     * @return la constante correspondiente o {@code null}
     */
    public static SortStrategy from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        for (SortStrategy strategy : values()) {
            if (strategy.name().equalsIgnoreCase(normalized)) {
                return strategy;
            }
        }
        return null;
    }
}
