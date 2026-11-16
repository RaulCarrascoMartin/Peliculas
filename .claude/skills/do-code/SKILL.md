---
name: do-code
description: Implementar o revisar el código del issue en el que se está (backend Java + Spring por capas controller → domain ← persistence). Subcomandos: "impl <plan>" (hace las tareas `- [ ]` de un fichero, como un plan_<algo>.md o el review.md de la revisión) y "review" (un subagente revisa el código y deja cada hallazgo como una tarea en review.md, sin tocar el código). Úsalo cuando el usuario diga "/do-code", pida implementar un plan o revisar el código de la rama.
argument-hint: <impl <plan.md> | review>
allowed-tools: Bash(git diff *), Bash(git status *), Bash(git branch *), Bash(./mvnw *), Bash(mvn *), Read, Write, Edit, Grep, Glob, Skill, Agent
---

# do-code

Skill corta: el trabajo lo hace el agente y las reglas de código viven en las skills `k-back-*`. Recibe el subcomando en `$ARGUMENTS`. No lleva sustantivo: actúa sobre el issue en el que se está, como `docker login`.

| Orden | Qué hace |
|---|---|
| `/do-code impl <plan>` | Hace las tareas `- [ ]` del fichero: el `plan_<algo>.md` de un issue que no es CRUD, o el `review.md` de la revisión |
| `/do-code review` | Un subagente revisa el código de la rama con las reglas de calidad del proyecto. No toca nada: deja cada hallazgo como una tarea en `review.md` |

Sin subcomando o con uno desconocido: pregunta cuál de los dos quiere, en una sola pregunta.

## Reglas de código

No se definen aquí. Carga `k-back-project`, que indica qué skill rige cada paquete:

| Paquete | Skill |
|---|---|
| `controller` | `k-back-controller` |
| `domain` | `k-back-service` |
| `persistence` | `k-back-repository` |
| `config`, `Application.java` | `k-back-project` |

## impl <plan>

Se lanza en una conversación nueva, que solo contiene el plan y el código que toca.

1. Lee el fichero del argumento. Si no existe, no es un `.md` o está vacío, para y pide la ruta.
2. Carga `k-back-project` y las skills de las capas que toque el plan.
3. Haz las tareas `- [ ]` una detrás de otra, en orden, y marca cada una `- [x]` al terminarla. Cada clase se escribe con la skill de su capa y lleva sus pruebas.
4. Tras cada tarea ejecuta los tests del backend (`./mvnw test`). Si fallan, **arregla el código** y vuelve a pasarlos. No retoques el test: si crees que el que está mal es el test, para y dilo.
5. No hagas commits: los hace `/do-git-flow commit` al final.
6. No cambies nada fuera de las tareas del fichero. Si detectas problemas ajenos, menciónalos al final.
7. Termina con un informe corto: tareas hechas, tareas que no, resultado de los tests y supuestos que el usuario debería confirmar.

Lo que falte en el plan no se inventa en silencio: se pregunta o se anota como supuesto.

## review

Solo lectura: lanza un **subagente** (Agent) con el contexto limpio, distinto de quien escribió el código. Nunca revisa el agente que lo ha escrito.

1. Alcance: los cambios de la rama (`git diff` contra `develop` y archivos nuevos). Si no hay, pregunta qué revisar.
2. El subagente contrasta cada archivo con la skill de su capa y comprueba:
   - Dependencias: `domain` no importa `controller` ni `persistence`; `controller` no importa `persistence`; todo entra por constructor.
   - Modelos anémicos, con las reglas de negocio en el servicio.
   - Comunicación entre capas solo con DTO, y mappers con MapStruct.
   - Toda clase creada o modificada tiene su prueba.
3. Escribe `review.md` en la raíz del worktree: cada hallazgo es una tarea `- [ ] <ruta:línea> — <regla incumplida> — <arreglo propuesto>`, ordenada por severidad (bloqueante, importante, menor).
4. No edita código. Para aplicar los hallazgos, el usuario lanza `/do-code impl review.md` en una conversación nueva.
