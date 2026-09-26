# PetCare — Sistema de Gestión de Boxes Veterinarios

Aplicación de consola para practicar Kotlin, herencia, polimorfismo, colecciones,
excepciones y corrutinas. Usa Kotlin 2.4.10, Java 21 y Gradle Kotlin DSL.

## Ejecutar en IntelliJ IDEA

1. Abre esta carpeta como proyecto Gradle.
2. Selecciona JDK 21 y sincroniza Gradle.
3. Ejecuta `main` en `src/main/kotlin/org/example/Main.kt`, o la tarea Gradle `run`.

Desde PowerShell, con Java 21 configurado:

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat run --console=plain
```

Si esta terminal no encuentra Java, en este equipo se puede indicar temporalmente:

```powershell
$env:JAVA_HOME = 'C:\Users\Pc\.jdks\ms-21.0.12.1'
```

La demostración tarda aproximadamente 48 segundos más el inicio de Gradle:
cinco entradas de 3 segundos y cinco salidas de 6,5 segundos.
Las fechas de atención se simulan para producir exactamente los minutos pedidos;
las esperas de los sensores son reales. Fuera de la demostración, una salida sin
fecha explícita utiliza `LocalDateTime.now()`.

## Archivos y responsabilidades

Todos los archivos de producción están en `src/main/kotlin/org/example/`.

| Archivo | Responsabilidad |
| --- | --- |
| Main.kt | Coordina la demostración con `runBlocking` e incluye los ejemplos de errores. |
| TipoDueno.kt | Enum con PARTICULAR, CONVENIO y MUNICIPAL. |
| Paciente.kt | Datos inmutables, validación, cálculo final y excepciones de código y tarifa. |
| Canino.kt | Costo por hora y descuento de convenio. |
| Felino.kt | Costo por hora y gratuidad antes de los 20 minutos. |
| Exotico.kt | Costo por hora, recargo silvestre y detalle de esa condición. |
| EstadoBox.kt | Cuatro estados, con datos propios de cada variante. |
| Box.kt | Número, estado y consultas exhaustivas del estado. |
| Ticket.kt | Datos del comprobante y paciente asociado, sin una clase intermedia. |
| PetCare.kt | Diez boxes, operaciones, historial de tickets, ingresos, consultas y manejo de errores. |
| Reporte.kt | Formato y presentación de boxes, tickets, recaudaciones, consultas y cierre. |
| DatosPrueba.kt | Construye los cinco pacientes exactos del enunciado. |

`src/test/kotlin/org/example/PetCareTest.kt` verifica las reglas y operaciones.
`build.gradle.kts` configura Kotlin, la aplicación y sus dependencias.
El archivo TipoDueno se conservó y se movió al directorio del paquete.
El Main de ejemplo fue reemplazado por la demostración.

## Conceptos para explicar el código

- **Clase y constructor:** una clase describe objetos. El constructor recibe los
  datos para crearlos, por ejemplo nombre, especie y fecha de ingreso.
- **Propiedad y val:** las propiedades almacenan datos. `val` no permite reasignar
  la propiedad; `var` sí. Los datos del paciente y `esSilvestre` son `val`.
- **Herencia:** Canino, Felino y Exotico heredan los datos y comportamiento común
  de Paciente. Paciente es abstracta y no se instancia directamente.
- **Método abstracto y override:** `calcularCosto` exige que cada subclase aporte
  su implementación mediante `override`. El método público `calcularTarifa`
  valida, llama al cálculo particular y aplica las reglas comunes.
- **Polimorfismo:** una `List<Paciente>` puede contener los tres tipos. La llamada
  a calcular el costo ejecuta la implementación del tipo real del paciente.
- **Enum class:** TipoDueno representa exactamente tres valores. Una propiedad
  de ese tipo no acepta un String arbitrario como `"OTRO"`.
- **Sealed class:** EstadoBox define una jerarquía cerrada. Libre no necesita
  datos; EnAtencion lleva paciente; EnProceso y FueraDeServicio llevan motivo.
  Los `when` enumeran las cuatro variantes sin un `else` que oculte alguna.
- **Data class:** Ticket agrupa los datos de una atención y su paciente. Kotlin genera funciones útiles
  como igualdad por contenido, `toString` y `copy`.
- **MutableList:** permite agregar elementos. `MutableList(10) { indice ->
  Box(indice + 1) }` crea diez boxes distintos, numerados del 1 al 10.
  La lista interna nunca se aumenta ni disminuye. Las vistas públicas son copias
  de solo lectura de la estructura, no copias profundas de sus elementos.
- **Regex:** `[A-Za-z]{2}[0-9]{2}[A-Za-z]{2}` significa dos letras, dos dígitos y
  dos letras. `matches` comprueba todo el texto. Se aceptan letras mayúsculas y
  minúsculas; no se añaden espacios ni se corrige automáticamente el código.
- **Excepciones:** `throw` comunica un error; `try` ejecuta una operación que puede
  fallar y `catch` permite responder. El programa muestra mensajes, sin imprimir
  stack traces. Un error en una operación no impide ejecutar la siguiente.
- **Corrutinas:** `suspend` permite suspender una operación. `delay` cede la
  ejecución durante la espera sin bloquear el hilo como `Thread.sleep`.
  `runBlocking` conecta el main normal con las funciones suspendidas y bloquea
  ese hilo hasta finalizar. Solo se usa en Main.
- **Mutex:** coordina operaciones para evitar modificaciones simultáneas. Esta
  implementación procesa entradas, salidas y mantenimiento por turnos, incluso
  si varios llamadores lanzan corrutinas. No pretende procesar sensores en paralelo.
- **Finally:** restaura Libre si se cancela una entrada y restaura EnAtencion si
  una salida falla o se cancela. Una cancelación se propaga como cancelación, no
  como error de datos.

## Tarifas y resultados

Se usa `minutos / 60.0` para preservar fracciones de hora. El cálculo no redondea
pasos intermedios; la presentación muestra dos decimales. Los importes de esta
práctica usan Double y las pruebas numéricas comparan con una tolerancia pequeña.

1. Canino: horas × 12.000. Si tiene convenio, multiplicar por 0,80.
2. Felino: cero si el tiempo es menor a 20 minutos; desde 20 minutos,
   horas × 9.000. No se aplica descuento de convenio a Felinos.
3. Exótico: horas × 20.000; si es silvestre, multiplicar por 1,30.
4. Para todos, aplicar IVA multiplicando por 1,19.
5. Finalmente, si el dueño es municipal, multiplicar por 0,50.

Los tiempos negativos o no finitos se rechazan. Los costos negativos o cero se
rechazan salvo la gratuidad del Felino. La tarifa final también se valida.

| Código | Paciente | Minutos | Cálculo | Total |
| --- | --- | ---: | --- | ---: |
| CA12CD | Max, Canino convenio | 75 | 1,25 × 12.000 × 0,80 × 1,19 | $14.280 |
| CA99ZA | Luna, Canino particular | 180 | 3 × 12.000 × 1,19 | $42.840 |
| FE22TO | Misi, Felino particular | 18 | Menos de 20 minutos | $0 |
| EX44RG | Loro, Exótico municipal silvestre | 120 | 2 × 20.000 × 1,30 × 1,19 × 0,50 | $30.940 |
| EX77RG | Iguana, Exótico particular | 45 | 0,75 × 20.000 × 1,19 | $17.850 |

Total: **$105.910**. Caninos: **$57.120**. Felinos: **$0**.
Exóticos: **$48.790**. Promedio: **$21.182**.
El tipo con más ingresos es Canino; Luna tiene el mayor tiempo, 180 minutos.
Al cierre quedan 10 boxes disponibles. Los tickets se numeran del 1 al 5.
La numeración es consecutiva dentro de cada instancia del sistema/turno.

## Cinco consultas

1. `cantidadBoxesDisponibles`: `count` cuenta los boxes cuyo estado es Libre.
2. `pacientesConConvenio`: `filter` selecciona atenciones de dueños convenio y
   `map` obtiene los pacientes. Para los datos de prueba devuelve Max.
3. `ingresoPromedio`: `map` obtiene importes y `average` calcula su media.
   Incluye al Felino que pagó cero. Sin historial devuelve cero.
4. `codigosFinalizados`: `map` obtiene los códigos del historial en su orden.
5. `pacienteConMayorTiempo`: `maxByOrNull` busca la atención más larga y devuelve
   su paciente. Sin historial devuelve null; en un empate devuelve el primero.

El reporte de cierre imprime todos los tickets y los totales pedidos.
Si hay empate entre tipos con ingresos máximos, muestra todos esos tipos.
Si no hay atenciones, muestra `Sin atenciones`. Si solo hubo Felinos con atención
gratuita, identifica `Felino` aunque su recaudación sea cero.
La creación inicial controla los errores de cada paciente por separado y omite
los datos inválidos sin impedir la creación de los demás.

## Errores y estado del sistema

| Excepción | Acción |
| --- | --- |
| CodigoAtencionInvalidoException | Rechaza la construcción del paciente y no lo ingresa. |
| TarifaInvalidaException | No genera ticket ni ingresos; la salida restaura al paciente en su box. |
| PacienteNoEncontradoException | No modifica boxes ni historial. |
| SistemaSinCapacidadException | No asigna un paciente a boxes no disponibles. |

Main demuestra `123ABC`, salida de un código ausente, tarifa cero no permitida,
tiempo negativo y ausencia de capacidad. La prueba de capacidad usa otra
instancia con diez boxes fuera de servicio, sin alterar los cinco casos pedidos.
También se prueba con diez boxes ocupados en las pruebas automáticas.

## Verificación de requisitos

| Requisito | Implementación y verificación |
| --- | --- |
| R1 | Paciente y tres subclases; propiedades val; tarifas y detalle silvestre. |
| R2 | EstadoBox y Box; diez números únicos; cuatro estados en cada when. |
| R3 | Regex, validaciones, IVA y beneficio municipal centralizados; ingresos por tipo. |
| R4 | Historial de Ticket con su paciente; cinco consultas y Reporte. |
| R5 | Funciones suspend; delay(3000) y delay(6500); Mutex y restauración ante fallos. |
| R6 | Cuatro excepciones; captura por operación sin stack traces. |
| R7 | Archivos separados; paquete org.example; PascalCase y camelCase. |

Las pruebas cubren los cinco importes, tipos de dueño, frontera de 20 minutos,
códigos inválidos, tiempos inválidos, diez boxes, estados durante esperas,
historial, ingresos, consultas vacías, capacidad, reintento tras error,
cancelaciones y entradas concurrentes. `kotlinx-coroutines-test` permite avanzar
tiempo virtual y comprobar exactamente las esperas sin esperar segundos reales
en cada prueba. Es una dependencia exclusiva de pruebas.


