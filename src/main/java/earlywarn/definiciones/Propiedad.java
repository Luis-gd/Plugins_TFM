package earlywarn.definiciones;

import earlywarn.main.Propiedades;

/**
 * Lista todas las propiedades que se pueden fijar en la BD.
 * @see Propiedades
 */
public enum Propiedad {
	// Propiedad de prueba
	@SuppressWarnings("unused")
	TEST,
	// True si se ha realizado la operación ETL de conversión de relaciones entre Aeropuerto y AirportOperationDay
	ETL_RELACIONES_AOD
}
