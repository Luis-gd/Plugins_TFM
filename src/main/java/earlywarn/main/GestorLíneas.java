package earlywarn.main;

import earlywarn.definiciones.ICálculoFitness;
import earlywarn.definiciones.IDCriterio;
import earlywarn.definiciones.IllegalOperationException;
import earlywarn.main.modelo.EstadoLínea;
import earlywarn.main.modelo.Línea;
import earlywarn.main.modelo.criterio.Criterio;
import org.neo4j.graphdb.GraphDatabaseService;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Permite abrir y cerrar líneas durante la ejecución del programa. Lleva un registro con los valores de todos los
 * criterios que dependen de las líneas abiertas y permite consultar su valor porcentual en cualquier momento.
 */
public class GestorLíneas {
	// Lista de criterios almacenados, cada uno identificado por un valor de un enum
	private final Map<IDCriterio, Criterio> criterios;

	// Mapa que almacena el estado de cada línea
	private final Map<String, EstadoLínea> líneas;

	// Clase usada para calcular el fitness final. Puede ser null.
	private ICálculoFitness cálculoFitness;

	/**
	 * Crea una instancia del gestor. El método está protegido ya que se debe usar {@link GestorLíneasBuilder} para
	 * instanciar esta clase.
	 * @param idPaís ID del país sobre el que se va a trabajar. Solo se trabajará con líneas que tengan aeropuertos de
	 *               este país como destino.
	 * @param díaInicio Primer día a tener en cuenta a la hora de determinar las líneas sobre las que se va a trabajar
	 * @param díaFin Último día a tener en cuenta a la hora de determinar las líneas sobre las que se va a trabajar
	 * @param db Instancia de la base de datos
	 */
	protected GestorLíneas(String idPaís, LocalDate díaInicio, LocalDate díaFin, GraphDatabaseService db) {
		líneas = new TreeMap<>();
		criterios = new EnumMap<>(IDCriterio.class);
		for (String idLínea : new Consultas(db).getLíneas(díaInicio, díaFin, idPaís)) {
			EstadoLínea estadoLínea = new EstadoLínea(new Línea(idLínea, díaInicio, díaFin, db), true);
			líneas.put(idLínea, estadoLínea);
		}
	}

	/**
	 * Añade un nuevo critero al gestor. Usado por {@link GestorLíneasBuilder} para añadir criterios al crear la
	 * instancia. No se pueden añadir más criterios una vez creada.
	 * @param criterio Criterio a añadir
	 */
	protected void _añadirCriterio(Criterio criterio) {
		criterios.put(criterio.id, criterio);
	}

	/**
	 * Añade un método de cálculo de fitness al gestor, lo que le permite agrupar los valores de cada criterio
	 * en uno solo. Usado por {@link GestorLíneasBuilder}.
	 * @param cálculoFitness Clase usada para calcular el fitness
	 */
	protected void _añadirCálculoFitness(ICálculoFitness cálculoFitness) {
		this.cálculoFitness = cálculoFitness;
	}

	/**
	 * Abre las líneas identificadas por los IDs incluidos en la lista indicada. Si una línea ya estaba abierta, se
	 * ignorará.
	 * @param líneas Lista con los IDs de las lineas que se quieren abrir.
	 */
	public void abrirLíneas(List<String> líneas) {
		abrirCerrarLíneas(líneas, true);
	}

	/**
	 * Cierra las líneas identificadas por los IDs incluidos en la lista indicada. Si una línea ya estaba cerrada, se
	 * ignorará.
	 * @param líneas Lista con los IDs de las lineas que se quieren cerrar.
	 */
	public void cerrarLíneas(List<String> líneas) {
		abrirCerrarLíneas(líneas, false);
	}

	/**
	 * Obtiene el valor porcentual del criterio indicado. Un valor de 1 indica que el criterio tiene el mejor valor
	 * posible, un valor de 0 indica que el criterio tiene el peor valor posible.
	 * @param id ID del criterio a consultar
	 * @return Valor porcentual (entre 0 y 1) del criterio indicado
	 * @throws IllegalArgumentException Si el criterio indicado no se ha añadido a este gestor al instanciarlo
	 */
	public double getPorcentajeCriterio(IDCriterio id) {
		Criterio criterio = criterios.get(id);
		if (criterio != null) {
			return criterio.getPorcentaje();
		} else {
			throw new IllegalArgumentException("El criterio especificado no está incluido en este gestor");
		}
	}

	/**
	 * Obtiene el valor de fitness actual dados los valores de todos los criterios. Requiere que se haya especificado
	 * un método de cálculo de fitness al instanciar esta clase
	 * @return Valor de fitness (entre 0 y 1) que representa la calidad de la solución actual
	 * @throws IllegalOperationException Si no se ha especificado un método de cálculo de fitness al crear esta
	 * instancia
	 */
	public double getFitness() {
		if (cálculoFitness != null) {
			return cálculoFitness.calcularFitness(criterios.values());
		} else {
			throw new IllegalOperationException("No se puede calcular el fitness de la solución si no se ha " +
				"especificado un método de cálculo");
		}
	}

	private void abrirCerrarLíneas(List<String> líneas, boolean abrir) {
		for (String idLínea : líneas) {
			EstadoLínea estadoLínea = this.líneas.get(idLínea);
			if (estadoLínea.abierta != abrir) {
				estadoLínea.abierta = !estadoLínea.abierta;
				// Recalcular los valores de todos los criteros
				for (Criterio criterio : criterios.values()) {
					criterio.recalcular(estadoLínea.línea, abrir);
				}
			}
		}
	}
}
