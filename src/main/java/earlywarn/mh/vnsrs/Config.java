package earlywarn.mh.vnsrs;

import earlywarn.main.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;

/**
 * Clase que representa los datos de configuración para la metaheurística
 */
@SuppressWarnings("ProhibitedExceptionThrown")
public class Config {
	/*
	 * La memoria en la dimensión de cambio de entorno Y será <este valor> veces el valor de umbral de iteraciones
	 * para la comprobación más grande
	 */
	private static final float MULT_TAMAÑO_MEMORIA_Y = 2;

	// --- Valores leídos tal cual del XML ---
	public ConfigVNS configVNS;
	public ConfigRS configRS;
	// Parar la ejecución cuando transcurra este número de iteraciones sin una mejora en la función objetivo
	public int itParada;
	// País sobre el que se está trabajando
	public String país;
	// Rango de fechas sobre el que se está trabajando
	public LocalDate díaInicio;
	public LocalDate díaFin;

	// --- Valores derivados de los leídos del XML ---
	/*
	 * Cada vez que tardemas más de <umbralIt> iteraciones en variar al menos <nº de líneas que representa la
	 * menor de las comprobaciones de líneas en el cambio de entorno en Y>, el valor de estancamiento de esa
	 * primera comprobación aumentará en 1. El esto de comprobaciones usan n * <umbralIt> como umbral, siendo n el
	 * número de comprobación (desde n = 1 para la primera hasta n = <numComprobaciones> para la última)
	 */
	private final double umbralIt;

	/**
	 * Instancia la configuración
	 * @param rutaFicheroConfig Ruta al fichero XML que contiene la configuración, relativa a la carpeta del DBMS
	 */
	public Config(String rutaFicheroConfig) {
		Document xml;
		try {
			xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(rutaFicheroConfig);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Fichero de configuración vns-rs no encontrado", e);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException("Error al abrir el fichero de configuración vns-rs", e);
		}

		configVNS = new ConfigVNS();
		configRS = new ConfigRS();

		Element raíz = xml.getDocumentElement();

		Element elemItCambioEntorno = Utils.toLista(raíz.getElementsByTagName("itCambioEntorno")).get(0);
		configVNS.itCambioEntorno = Integer.parseInt(elemItCambioEntorno.getTextContent());
		Element elemTamañoMemoriaX = Utils.toLista(raíz.getElementsByTagName("tamañoMemoriaX")).get(0);
		configVNS.tamañoMemoriaX = Float.parseFloat(elemTamañoMemoriaX.getTextContent());
		Element elemDistanciaMemoriaX = Utils.toLista(raíz.getElementsByTagName("distanciaMemoriaX")).get(0);
		configVNS.distanciaMemoriaX = Float.parseFloat(elemDistanciaMemoriaX.getTextContent());

		Element elemEntornoY = Utils.toLista(raíz.getElementsByTagName("entornoY")).get(0);
		Element elemMaxPorcentLíneas = Utils.toLista(elemEntornoY.getElementsByTagName("maxPorcentLíneas")).get(0);
		configVNS.maxPorcentLíneas = Float.parseFloat(elemMaxPorcentLíneas.getTextContent());
		Element elemNumComprobaciones = Utils.toLista(elemEntornoY.getElementsByTagName("numComprobaciones")).get(0);
		configVNS.numComprobaciones = Integer.parseInt(elemNumComprobaciones.getTextContent());
		Element elemPorcentLíneas = Utils.toLista(elemEntornoY.getElementsByTagName("porcentLíneas")).get(0);
		configVNS.porcentLíneas = Float.parseFloat(elemPorcentLíneas.getTextContent());
		Element elemIteraciones = Utils.toLista(elemEntornoY.getElementsByTagName("iteraciones")).get(0);
		configVNS.iteraciones = Integer.parseInt(elemIteraciones.getTextContent());
		Element elemLíneasPorIt = Utils.toLista(elemEntornoY.getElementsByTagName("líneasPorIt")).get(0);
		configVNS.líneasPorIt = Float.parseFloat(elemLíneasPorIt.getTextContent());

		Element elemTInicial = Utils.toLista(raíz.getElementsByTagName("tInicial")).get(0);
		configRS.tInicial = Float.parseFloat(elemTInicial.getTextContent());
		Element elemAlfa = Utils.toLista(raíz.getElementsByTagName("alfa")).get(0);
		configRS.alfa = Float.parseFloat(elemAlfa.getTextContent());
		Element elemItReducciónT = Utils.toLista(raíz.getElementsByTagName("itReducciónT")).get(0);
		configRS.itReducciónT = Integer.parseInt(elemItReducciónT.getTextContent());

		Element elemItParada = Utils.toLista(raíz.getElementsByTagName("itParada")).get(0);
		itParada = Integer.parseInt(elemItParada.getTextContent());
		Element elemPaís = Utils.toLista(raíz.getElementsByTagName("país")).get(0);
		país = elemPaís.getTextContent();
		Element elemDíaInicio = Utils.toLista(raíz.getElementsByTagName("primerDía")).get(0);
		díaInicio = Utils.stringADate(elemDíaInicio.getTextContent());
		Element elemDíaFin = Utils.toLista(raíz.getElementsByTagName("últimoDía")).get(0);
		díaFin = Utils.stringADate(elemDíaFin.getTextContent());

		// Calcular valores derivados
		double umbralItUsuario = configVNS.iteraciones / Utils.log2(configVNS.líneasPorIt);
		umbralIt = umbralItUsuario * getDistComprobacionesY() / configVNS.porcentLíneas;
	}

	public double getUmbralIt() {
		return umbralIt;
	}

	/**
	 * @return Distancia a la que se encuentra cada una de las comprobaciones de estancamiento usadas en el cambio
	 * de entorno vertical. La distancia se mide en porcentaje de las líneas totales (0-1).
	 */
	public float getDistComprobacionesY() {
		return configVNS.maxPorcentLíneas / configVNS.numComprobaciones;
	}

	/**
	 * @return Número de entradas que deberían almacenarse en la memoria de cambio de entorno vertical
	 */
	public int getTamañoMemoriaY() {
		return ((Long) Math.round(getUmbralIt() * configVNS.numComprobaciones * MULT_TAMAÑO_MEMORIA_Y)).intValue();
	}
}
