# Pregunta 3 — Merge vs rebase

> Respuesta de la entrevista, por escrito. Es la misma respuesta que sirve `GET /q03/summary`,
> pero escrita para leerse (y para repasarla antes de entrar).

---

## La respuesta en 30 segundos

- **`merge` conserva el historial.** Integra una rama creando un **commit de merge** (un commit
  con dos padres). El grafo queda como un **DAG con bifurcaciones e intersecciones** y los
  **SHA de los commits que ya existían no cambian**.
- **`rebase` reescribe el historial.** Mueve la base de la rama sobre la punta de otra rama:
  replaya (recrea) los commits encima de la nueva base. El grafo queda **lineal**, pero los
  commits son **nuevos** y sus **SHA cambian**.
- **Regla de oro:** nunca se rebasa una rama **pública** (`main`, `master`, `release/*`).
- **`stash` no es una rama:** es un estante de cambios del *working directory*.

---

## 1. `git merge`: el historial se conserva

`git merge feature` integra la rama `feature` en la rama actual.

- Crea un **commit de merge**, que es un commit con **dos padres**: el último commit de la rama
  actual y el último commit de la rama integrada.
- El grafo pasa a ser un **DAG con intersecciones**: se ve de dónde viene cada cambio.
- **No reescribe nada.** Los commits que ya existían conservan su SHA, sus mensajes, sus
  autores y sus fechas.
- Es **seguro** aunque la rama ya esté publicada: nadie tiene que volver a descargarse nada.

El coste es estético y también operativo: el grafo se llena de nodos, los commits de merge
ensucian el log y `git bisect` / `git blame` se vuelven incómodos (hay que saltar de un merge a
otro merge para encontrar el cambio real).

## 2. `git rebase`: el historial se reescribe

`git rebase feature` **cambia la base** de la rama actual: coge los commits de la rama actual y
los **recrea** encima de la punta de `feature`.

- El resultado es un **historial lineal**, sin bifurcaciones: ideal para revisar un PR.
- Los commits recreados tienen **el mismo contenido y el mismo mensaje**, pero son **commits
  nuevos** y por tanto tienen **SHA nuevos**. Los commits antiguos dejan de existir para Git.
- Por eso, si otra persona ya se había descargado los commits antiguos, al hacer `pull` se
  encuentra con **dos versiones de la misma historia**.

El historial lineal no es "más limpio" por magia: es limpio porque se ha **tirado a la basura**
la historia anterior.

## 3. La regla de oro: nunca rebases una rama pública

**Nunca rebases `main`, `master` ni una rama de release.**

El motivo es el SHA. El rebase recrea los commits con SHA nuevos, así que:

1. Tú haces `git rebase main` sobre tu rama de feature y abres el pull request.
2. Mientras tanto, otra persona se ha descargado tu rama **antes** del rebase.
3. Cuando esa persona hace `pull`, Git le dice que hay **historia duplicada** (los commits viejos
   y los nuevos son la misma historia con distinto SHA) y aparecen **conflictos que no existen
   de verdad** en el repositorio.

La alternativa es `merge` para integrar ramas publicadas. Lo habitual en un equipo es:

- rebase **tu** rama de feature sobre `main` mientras la rama es tuya y nadie más la tiene
  (`git pull --rebase` para mantenerla al día);
- **merge** de la feature a `main` cuando la rama ya es pública, idealmente con
  `git merge --no-ff feature` para que quede explícito el commit de integración.

## 4. `stash` frente a rama local

No son dos formas de hacer lo mismo.

| | **Stash** | **Rama local** |
|---|---|---|
| Qué es | Un commit guardado en el ref `refs/stash` | Una cadena real de commits |
| Para qué | Aparcar cambios en caliente | Developar una feature, dejar un punto de retorno |
| ¿Tiene historia? | No | Sí |
| ¿Se puede ramificar? | No | Sí |
| ¿Se puede pushear? | No | Sí |
| ¿Se integra con merge? | No | Sí |
| Cuántos suele haber | Uno (es una pila: `stash@{0}`, `stash@{1}`…) | Las que haga falta |

Uso típico del stash: estás a mitad de un cambio y te toca cambiar de rama (o hacer un `pull`
urgente). `git stash` aparca los cambios, `git stash list` muestra lo que hay aparcado y
`git stash pop` recupera el último y lo aplica sobre el directorio de trabajo.

Aviso práctico: `git stash` **no** guarda ficheros **sin versionar**; para eso está
`git stash -u` (untracked). Y conviene no acumular stashes: son parches sin contexto y es muy
fácil olvidar que están ahí.

## 5. Cómo verlo en acción

```bash
./scripts/git-merge-vs-rebase.sh
```

El script monta un repositorio de usar y tirar en un directorio temporal y reproduce:

1. un `merge` que **termina en conflicto** (con los marcadores `<<<<<<<`, `=======`, `>>>>>>>`),
   el `git status` y el `git merge --abort`;
2. un `rebase` que también entra en conflicto, se resuelve y deja un **historial lineal**, con
   la **comparación de SHA** antes y después (`git log --format=%h`);
3. el **grafo antes y después** (`git log --graph --oneline --all`), para que la diferencia entre
   el DAG del merge y la recta del rebase se vea de un vistazo;
4. el ciclo `git stash` / `git stash list` / `git stash pop` y su comparación con una rama local.

Toda la transcripción se guarda en `target/git-demo/transcript.txt` y se sirve en
`GET /q03/transcript`.

## 6. Si preguntan por el detalle fino

- **`git merge --no-ff`**: fuerza el commit de merge aunque el merge sea "fast-forward"
  (cuando la rama integrada ya está contenida en la actual). Se usa para que el grafo diga
  siempre "aquí se integró una rama", que es información útil.
- **`git rebase -i` (interactivo)**: permite reordenar, reescribir el mensaje, aplastar
  (`squash`/`fixup`) o dejar commits en el sitio (`edit`) antes de continuar.
- **Conflictos durante el rebase**: se resuelven como los de un merge (`git status` para ver los
  ficheros en conflicto, se editan, `git add`, `git rebase --continue`) y se puede abandonar con
  `git rebase --abort`.
- **Falsos conflictos**: durante un rebase, Git reaplica commit a commit; por eso un conflicto
  puede aparecer en un commit intermedio aunque el resultado final sea trivial. Es la otra cara
  de reescribir la historia.
