# Plugins_TFM
Directorio de los plugins que se desarrollen para los TFMs de gestión del tráfico aéreo.

El proyecto se organiza de la siguiente forma:

- definiciones: Clases especiales uasadas en diferentes partes del código. Normalmente excepciones, interfaces, enums y clases de datos.
- etl: Clases que se encarguen de cargar los nuevos datos o limpiar los existentes.
- funciones: Contiene funciones pueden ser llamadas desde Neo4J.
- main: Clases generales que se vayan a usar en el código de diferentes personas.
- mh: Clases que implementen las metaheurísticas.
- signals: Clases para la generación de señales temporales para la detección temprana de la expansión del brote. 

Esta estructura puede cambiarse si a alguien se le ocurre alguna forma mejor de organizar el código.
