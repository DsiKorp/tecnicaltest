#!/usr/bin/env bash
# ==========================================================================
# Pregunta 3 del artículo EPAM: MERGE vs REBASE.
#
# El script monta un repositorio de usar y tirar en un directorio temporal y reproduce, paso a
# paso, las tres situaciones que hay que saber explicar en una entrevista:
#
#   1. git merge: conserva el historial, aparece un commit de merge y el grafo se bifurca.
#   2. git rebase: reescribe el historial, deja un grafo lineal y cambia los SHA de los commits.
#   3. git stash: no es una rama; es un commit escondido en el ref refs/stash.
#
# Toda la transcripción se escribe en target/git-demo/transcript.txt y también se imprime por
# pantalla (el endpoint GET /q03/transcript la sirve).
#
#   ./scripts/git-merge-vs-rebase.sh
#
# No toca el repositorio del proyecto: todo ocurre en un mktemp -d que se borra al salir.
# ==========================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OUTPUT_DIR="${PROJECT_DIR}/target/git-demo"
TRANSCRIPT="${OUTPUT_DIR}/transcript.txt"

TMP_DIR="$(mktemp -d)"
cleanup() {
    rm -rf "${TMP_DIR}"
}
trap cleanup EXIT

REPO="${TMP_DIR}/demo"

# --------------------------------------------------------------------------- utilidades

# Separador de sección: todas las líneas del transcript llevan un marcador.
seccion() {
    printf '\n==========================================================================\n'
    printf '  %s\n' "$1"
    printf '==========================================================================\n\n'
}

# Línea de explicación dentro de una sección.
explicacion() {
    printf '\n-- %s\n' "$1"
}

# Ejecuta un comando mostrando la línea de comando, su salida y su código de retorno.
# No aborta el script cuando el comando falla: algunos pasos (el merge en conflicto) fallan
# a propósito y esa salida es justo lo que hay que enseñar.
ejecutar() {
    printf '\n$ %s\n' "$*"
    set +e
    "$@"
    local codigo=$?
    set -e
    printf '\n[código de salida: %s]\n' "${codigo}"
}

# --------------------------------------------------------------------------- demo

main() {
    seccion "0. PREPARACIÓN: repositorio de usar y tirar"

    explicacion "Se crea un repositorio vacío en un directorio temporal. El repositorio del proyecto no se toca."
    mkdir -p "${REPO}"
    git init -q -b main "${REPO}"
    cd "${REPO}"

    explicacion "Identidad local, para que los commits no dependan de la configuración global de la máquina."
    git config user.name "Demo EPAM"
    git config user.email "demo@epam.local"
    git config commit.gpgsign false
    git config core.pager cat

    printf 'linea 1: version inicial\nlinea 2: contenido compartido\nlinea 3: version inicial\n' > app.txt
    git add app.txt
    git commit -q -m "Commit inicial"
    ejecutar git log --oneline --decorate

    seccion "1. MERGE: integrar feature en main (el historial se conserva)"

    explicacion "Se crea la rama 'feature' y se cambia la MISMA linea que luego se cambiara en main."
    explicacion "Los dos cambios tocaran la linea 1: eso garantiza el conflicto."
    git checkout -q -b feature
    printf 'linea 1: version feature\nlinea 2: contenido compartido\nlinea 3: version feature\n' > app.txt
    git commit -q -am "Cambio en feature (conflicto con main)"

    git checkout -q main
    printf 'linea 1: version main\nlinea 2: contenido compartido\nlinea 3: version main\n' > app.txt
    git commit -q -am "Cambio en main (conflicto con feature)"

    explicacion "Grafo ANTES del merge: dos ramas que salen del mismo commit (un DAG con bifurcacion)."
    ejecutar git log --graph --oneline --all

    explicacion "git merge feature: integra los cambios creando un commit de merge."
    explicacion "Aqui el conflicto es inevitable, porque las dos ramas cambiaron la misma linea."
    ejecutar git merge feature

    explicacion "El conflicto deja marcadores en el fichero:<<<<<<<, ======= y >>>>>>>."
    ejecutar cat app.txt

    explicacion "git status explica en que estado quedo el repositorio."
    ejecutar git status

    explicacion "git merge --abort deshace el merge y deja el repositorio como estaba."
    ejecutar git merge --abort
    ejecutar git status

    explicacion "Conclusion del merge: el historial NO se reescribe, los SHA de los commits que ya"
    explicacion "existian siguen siendo los mismos, y en el grafo aparece una bifurcacion que se"
    explicacion "cierra con un commit de merge. Es el historial 'real' de lo que fue pasando."

    seccion "2. REBASE: replay de main sobre feature (el historial se reescribe)"

    explicacion "git rebase feature cambia la base de main: replaya los commits de main encima de feature."
    explicacion "Antes del rebase, estos son los SHA de los commits de main."
    SHA_ANTES="$(git rev-parse --short main)"
    SHA_ANTES_LOG="$(git log --format=%h main)"
    printf '\nSHA de la punta de main ANTES del rebase: %s\n' "${SHA_ANTES}"
    printf 'Historial de main ANTES del rebase:\n%s\n' "${SHA_ANTES_LOG}"

    explicacion "Grafo ANTES del rebase: dos ramas que salen del mismo commit (igual que antes)."
    ejecutar git log --graph --oneline --all

    ejecutar git rebase feature

    explicacion "El rebase también detecta el conflicto: hay que resolverlo y continuar."
    ejecutar cat app.txt

    explicacion "Se resuelve el conflicto a mano (version combinada) y se continua el rebase."
    printf 'linea 1: version combinada\nlinea 2: contenido compartido\nlinea 3: version combinada\n' > app.txt
    git add app.txt
    GIT_EDITOR=true git rebase --continue

    explicacion "DESPUES del rebase, estos son los SHA de los commits de main."
    SHA_DESPUES="$(git rev-parse --short main)"
    SHA_DESPUES_LOG="$(git log --format=%h main)"
    printf '\nSHA de la punta de main DESPUES del rebase: %s\n' "${SHA_DESPUES}"
    printf 'Historial de main DESPUES del rebase:\n%s\n' "${SHA_DESPUES_LOG}"

    explicacion "Grafo DESPUES del rebase: el historial es LINEAL, ya no hay bifurcacion."
    ejecutar git log --graph --oneline --all

    explicacion "Comparativa de SHA: el commit de main es el MISMO contenido y el MISMO mensaje,"
    explicacion "pero es un commit NUEVO, con SHA distinto. Eso es reescribir el historial."
    printf '\nSHA antes  : %s\n' "${SHA_ANTES}"
    printf 'SHA despues: %s\n' "${SHA_DESPUES}"
    if [ "${SHA_ANTES}" != "${SHA_DESPUES}" ]; then
        printf '=> Los SHA han cambiado: el historial se ha reescrito.\n'
    else
        printf '=> Los SHA no han cambiado (no deberia ocurrir en esta demo).\n'
    fi

    seccion "3. STASH: guardar cambios sin commitear"

    explicacion "Se ensucia el directorio de trabajo con un cambio sin confirmar sobre un fichero"
    explicacion "YA versionado. Ojo: un fichero nuevo sin versionar NO se guarda con 'git stash';"
    explicacion "para eso hace falta 'git stash -u' (untracked)."
    printf 'linea 2: contenido compartido CAMBIADO sin commitear\n' > app.txt
    ejecutar git status
    ejecutar git diff

    explicacion "git stash guarda los cambios en el area de trabajo y deja el repositorio limpio."
    ejecutar git stash
    ejecutar git status

    explicacion "git stash list muestra los stashes guardados (son commits, NO ramas)."
    ejecutar git stash list

    explicacion "git stash pop recupera el ultimo stash y lo aplica sobre el directorio de trabajo."
    ejecutar git stash pop
    ejecutar git status

    explicacion "Tras el pop, la lista de stashes vuelve a estar vacia."
    ejecutar git stash list

    seccion "4. STASH vs RAMA LOCAL: no son lo mismo"

    explicacion "Una RAMA LOCAL es una cadena real de commits: tiene historia, se puede ramificar,"
    explicacion "se puede push y se puede integrar con un merge. Se crea aqui para contrastarla."
    git branch rama-local
    ejecutar git branch -vv
    ejecutar git log --oneline rama-local

    explicacion "Un STASH es un commit suelto guardado en el ref refs/stash, no una rama."
    explicacion "No se puede ramificar, no tiene historia propia y suele haber uno solo."
    explicacion "Para poder enseñarlo se crea uno nuevo: se ensucia el directorio y se guarda."
    printf 'linea 2: contenido compartido CAMBIADO otra vez\n' > app.txt
    ejecutar git stash
    ejecutar git stash list
    ejecutar git stash show --stat 'stash@{0}'
    explicacion "El stash se referencia como stash@{0}, stash@{1}, ... (una pila), nunca como un nombre de rama."

    seccion "5. RESUMEN"

    explicacion "MERGE  -> conserva el historial. Crea un commit de merge. El grafo es un DAG con"
    explicacion "          intersecciones. Los SHA de los commits existentes NO cambian."
    explicacion "REBASE -> reescribe el historial. Mueve la base de la rama sobre la punta de otra"
    explicacion "          rama. El grafo queda lineal, pero los commits se recrean con SHA NUEVOS."
    explicacion "REGLA DE ORO -> nunca se rebasa una rama PUBLICA (main, master, release)."
    explicacion "          Si alguien ya se ha descargado los commits antiguos, al rebase aparecera"
    explicacion "          historia duplicada y conflictos absurdos."
    explicacion "STASH  -> es un commit escondido en refs/stash, no una rama. Se usa para aparcar"
    explicacion "          cambios en caliente; lo normal es uno solo."
    explicacion "STASH vs RAMA LOCAL -> la rama es historia publicable; el stash es un parche"
    explicacion "          provisional en el area de trabajo."

    explicacion "Transcript completo guardado en:"
    printf '  %s\n' "${TRANSCRIPT}"
}

mkdir -p "${OUTPUT_DIR}"

set +e
main 2>&1 | tee "${TRANSCRIPT}"
set -e

exit 0
