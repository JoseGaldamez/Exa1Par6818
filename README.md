# Integrantes del equipo

| Integrante | Número de cuenta |
| --- | --- |
| José Galdámez | 201210011668 |
| Linda Nolasco | 202110080018 |

# Apuntes UTH

Proyecto desarrollado como **examen del primer parcial de Programación Móvil 1**, en la **Universidad Tecnológica de Honduras (UTH)**, con el **Ing. Ricardo Enrique Lagos Mendoza**.

## Objetivo del proyecto

Desarrollar una aplicación Android que permita organizar apuntes académicos mediante notas de voz y descripciones escritas o dictadas. Cada apunte reúne una asignatura, un tema, una fecha, una descripción y una grabación de audio, para facilitar el registro y la consulta de lo aprendido en clase.

El proyecto pone en práctica el diseño de pantallas Android, la navegación entre actividades, la persistencia con SQLite, las operaciones de registro, consulta, actualización y eliminación, la validación de datos y el uso del micrófono con permisos en tiempo de ejecución.

## Funcionalidades principales

- **Registrar apuntes:** grabar una nota de voz con `MediaRecorder` y completar sus datos académicos.
- **Administrar asignaturas:** comenzar con Programación Móvil 1 y crear nuevas asignaturas desde un modal, guardándolas en SQLite.
- **Consultar apuntes:** listar los registros guardados, mostrar fechas en un formato legible y filtrar por asignatura mediante una consulta parametrizada.
- **Editar y eliminar:** actualizar los datos de un apunte seleccionado o eliminarlo junto con su archivo de audio.
- **Reproducir grabaciones:** escuchar las notas de voz guardadas mediante `MediaPlayer`.
- **Dictar descripciones:** utilizar `RecognizerIntent` para convertir la voz en texto, revisarlo y devolverlo al formulario. Esta función no guarda un archivo de audio.
- **Validar los datos:** exigir los campos obligatorios, comprobar fechas en formato `AAAA-MM-DD` y permitir únicamente letras, números y espacios en el tema.
- **Gestionar permisos:** solicitar `RECORD_AUDIO` en tiempo de ejecución y explicar su denegación mediante `AlertDialog.Builder`.

## Almacenamiento y tecnologías

La aplicación está desarrollada en **Java**, con interfaces en **XML** y componentes **Material Design**. Utiliza **SQLite** para guardar las asignaturas y los datos de los apuntes. Las grabaciones se conservan como archivos en el almacenamiento privado de la aplicación y su ruta queda registrada en la base de datos.

El reconocimiento de voz utiliza el servicio disponible en el dispositivo; su disponibilidad y conexión necesaria dependen de ese servicio.

## Ejecución

Abrir el proyecto en Android Studio, sincronizar Gradle y ejecutar el módulo `app` en un dispositivo o emulador con Android 7.0 (API 24) o superior. Para probar la grabación se necesita acceso al micrófono y, para el dictado, un servicio de reconocimiento de voz disponible.
