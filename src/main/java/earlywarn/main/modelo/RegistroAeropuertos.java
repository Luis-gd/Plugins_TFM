package earlywarn.main.modelo;

import org.neo4j.graphdb.GraphDatabaseService;

import java.time.LocalDate;
import java.util.TreeMap;

/**
 * Permite almacenar una serie de aeropuertos según se va necesitando acceder a sus datos.
 * La primera vez que se trate de acceder a los datos de un aeropuerto, éste se insertará en el registro. A partir de
 * entonces, los accesos al mismo aeropuerto devolverán la misma instancia.
 */
public class RegistroAeropuertos {
	private final GraphDatabaseService db;
	private final TreeMap<String, Aeropuerto> aeropuertos;
	private final LocalDate díaInicio, díaFin;

	/**
	 * Crea una nueva instancia del registro. Los aeropuertos que se vayan añadiendo tendrán especificadas las fechas
	 * indicadas, que se usarán a la hora de determinar qué vuelos se tienen en cuenta para calcular sus propiedades
	 * (como por ejemplo, el número de vuelos de cada aeropuerto)
	 * @param díaInicio Primer día a tener en cuenta para calcular las propiedades de los aeropuertos insertados
	 * @param díaFin Último día a tener en cuenta para calcular las propiedades de los aeropuertos insertados
	 * @param db Conexión a la BD
	 */
	public RegistroAeropuertos(LocalDate díaInicio, LocalDate díaFin, GraphDatabaseService db) {
		this.díaInicio = díaInicio;
		this.díaFin = díaFin;
		this.db = db;
		aeropuertos = new TreeMap<>();
	}

	/**
	 * Obtiene un aeropuerto almacenado en el registro. Si aún no existe, lo inserta y devuelve la nueva instancia
	 * creada
	 * @param id Código IATA del aeropuerto
	 * @return Instancia que representa el aeropuerto que tiene el id especificado
	 */
	public Aeropuerto get(String id) {
		Aeropuerto aeropuerto;
		aeropuerto = aeropuertos.get(id);
		if (aeropuerto == null) {
			aeropuerto = new Aeropuerto(id, díaInicio, díaFin, db);
			aeropuertos.put(id, aeropuerto);
		}
		return aeropuerto;
	}
}
