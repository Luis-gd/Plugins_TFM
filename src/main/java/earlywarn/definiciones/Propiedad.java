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
	ETL_RELACIONES_AOD,
	// True si se ha realizado la operación ETL que elimina vuelos que no tengan datos SIR
	ETL_BORRAR_VUELOS_SIN_SIR,
	// True si se han convertido las fechas de vuelos a tipo date
	ETL_CONVERTIR_FECHAS_VUELOS,
	// True si se ha calculado el número de pasajeros de cada avión
	ETL_PASAJEROS,
	// True si se han cargado los datos de conectividad para cada aeropuerto
	ETL_CONECTIVIDAD,
	// True si se han cargado los datos del ratio de turistas para los diferentes países y regiones
	ETL_RATIO_TURISTAS
}
