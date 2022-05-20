package earlywarn.main;

import earlywarn.definiciones.ICálculoFitness;
import earlywarn.definiciones.IDCriterio;
import earlywarn.definiciones.IllegalOperationException;
import earlywarn.definiciones.OperaciónLínea;
import earlywarn.main.modelo.EstadoLínea;
import earlywarn.main.modelo.Línea;
import earlywarn.main.modelo.criterio.Criterio;
import org.neo4j.graphdb.GraphDatabaseService;

import java.time.LocalDate;
import java.util.*;

/**
 * Permite abrir y cerrar líneas durante la ejecución del programa. Lleva un registro con los valores de todos los
 * criterios que dependen de las líneas abiertas y permite consultar su valor porcentual en cualquier momento.
 */
public class GestorLíneas {
	// Lista de criterios almacenados, cada uno identificado por un valor de un enum
	private final Map<IDCriterio, Criterio> criterios;

	// Mapa que almacena el estado de cada línea
	private final Map<String, EstadoLínea> líneas;
	// Número de líneas actualmente abiertas
	private int numAbiertas;

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
		numAbiertas = líneas.size();
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
	 * Abre o cierra las líneas identificadas por los IDs incluidos en la lista indicada. Si alguna de las líneas
	 * indicadas ya estaba en el estado objetivo, se ignorará.
	 * @param líneas Lista con los IDs de las lineas que se quieren abrir o cerrar.
	 * @param operación Operación a realizar (apertura o cierre)
	 */
	public void abrirCerrarLíneas(List<String> líneas, OperaciónLínea operación) {
		boolean abrir = operación == OperaciónLínea.ABRIR;
		for (String idLínea : líneas) {
			EstadoLínea estadoLínea = this.líneas.get(idLínea);
			if (estadoLínea.abierta != abrir) {
				estadoLínea.abierta = !estadoLínea.abierta;
				// Recalcular los valores de todos los criteros
				for (Criterio criterio : criterios.values()) {
					criterio.recalcular(estadoLínea.línea, abrir);
				}

				if (operación == OperaciónLínea.ABRIR) {
					numAbiertas++;
				} else {
					numAbiertas--;
				}
			}
		}
	}

	/**
	 * @return Número de líneas totales
	 */
	public int getNumLíneas() {
		return líneas.size();
	}

	/**
	 * @return Número de líneas actualmente abiertas
	 */
	public int getNumAbiertas() {
		return numAbiertas;
	}

	/**
	 * @return Número de líneas actualmente cerradas
	 */
	public int getNumCerradas() {
		return líneas.size() - numAbiertas;
	}

	/**
	 * @return Lista con todas las líneas que se encuentran actualmente cerradas
	 */
	public List<String> getCerradas() {
		List<String> ret = new ArrayList<>();
		for (EstadoLínea estadoLínea : líneas.values()) {
			if (!estadoLínea.abierta) {
				ret.add(estadoLínea.línea.id);
			}
		}
		return ret;
	}

	/**
	 * Devuelve los identificadores de las líneas que ocupen cada una de las posiciones indicadas dentro del conjunto
	 * de todas las líneas que se encuentren en el estado indicado.
	 * Por ejemplo, si el estado especificado es "abierta" y las posiciones son 1, 3 y 12, se devolverán la primera
	 * línea abierta, la tercera y la decimosegunda.
	 * @param posiciones Lista con las posiciones de las líneas a devolver
	 * @param getAbiertas Si es true, solo se considerarán las líneas abiertas. Si es false, las cerradas.
	 * @return Lista con las líneas que ocupan las posiciones indicadas de entre todas las que tienen el estado
	 * indicado.
	 */
	public List<String> getPorPosiciónYEstado(List<Integer> posiciones, boolean getAbiertas) {
		List<String> ret = new ArrayList<>();

		List<Integer> posicionesOrdenadas = new ArrayList<>(posiciones);
		posicionesOrdenadas.sort(null);

		Iterator<EstadoLínea> itLíneas = líneas.values().iterator();
		// Lleva la cuenta de cuántas líneas hemos encontrado hasta ahora que estén en el estado que buscamos
		int procesadas = 0;
		for (Integer posActual : posicionesOrdenadas) {
			boolean encontrada = false;
			while (!encontrada) {
				EstadoLínea entradaActual = itLíneas.next();
				if (entradaActual.abierta == getAbiertas) {
					// Esta es la línea nº <procesadas> que está en el estado que buscamos
					if (procesadas == posActual) {
						ret.add(entradaActual.línea.id);
						encontrada = true;
					}
					procesadas++;
				}
			}
		}
		return ret;
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
}
