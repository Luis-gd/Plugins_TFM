package earlywarn.mh.vnsrs;

import earlywarn.main.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;

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

	// --- Valores leídos tal cual del XML
	// Vns
	public float tamañoMemoriaX;
	public float distanciaMemoriaX;
	public int itCambioEntorno;

	public float maxPorcentLíneas;
	public int numComprobaciones;
	public float porcentLíneas;
	public int iteraciones;
	public float líneasPorIt;

	// RS
	public float tInicial;
	public float alfa;
	public int itReducciónT;
	public int itParada;

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

		Element raíz = xml.getDocumentElement();

		Element elemItCambioEntorno = Utils.toLista(raíz.getElementsByTagName("itCambioEntorno")).get(0);
		itCambioEntorno = Integer.parseInt(elemItCambioEntorno.getTextContent());
		Element elemTamañoMemoriaX = Utils.toLista(raíz.getElementsByTagName("tamañoMemoriaX")).get(0);
		tamañoMemoriaX = Float.parseFloat(elemTamañoMemoriaX.getTextContent());
		Element elemDistanciaMemoriaX = Utils.toLista(raíz.getElementsByTagName("distanciaMemoriaX")).get(0);
		distanciaMemoriaX = Float.parseFloat(elemDistanciaMemoriaX.getTextContent());

		Element elemEntornoY = Utils.toLista(raíz.getElementsByTagName("entornoY")).get(0);
		Element elemMaxPorcentLíneas = Utils.toLista(elemEntornoY.getElementsByTagName("maxPorcentLíneas")).get(0);
		maxPorcentLíneas = Float.parseFloat(elemMaxPorcentLíneas.getTextContent());
		Element elemNumComprobaciones = Utils.toLista(elemEntornoY.getElementsByTagName("numComprobaciones")).get(0);
		numComprobaciones = Integer.parseInt(elemNumComprobaciones.getTextContent());
		Element elemPorcentLíneas = Utils.toLista(elemEntornoY.getElementsByTagName("porcentLíneas")).get(0);
		porcentLíneas = Float.parseFloat(elemPorcentLíneas.getTextContent());
		Element elemIteraciones = Utils.toLista(elemEntornoY.getElementsByTagName("iteraciones")).get(0);
		iteraciones = Integer.parseInt(elemIteraciones.getTextContent());
		Element elemLíneasPorIt = Utils.toLista(elemEntornoY.getElementsByTagName("líneasPorIt")).get(0);
		líneasPorIt = Float.parseFloat(elemLíneasPorIt.getTextContent());

		Element elemTInicial = Utils.toLista(raíz.getElementsByTagName("tInicial")).get(0);
		tInicial = Float.parseFloat(elemTInicial.getTextContent());
		Element elemAlfa = Utils.toLista(raíz.getElementsByTagName("alfa")).get(0);
		alfa = Float.parseFloat(elemAlfa.getTextContent());
		Element elemItReducciónT = Utils.toLista(raíz.getElementsByTagName("itReducciónT")).get(0);
		itReducciónT = Integer.parseInt(elemItReducciónT.getTextContent());
		Element elemItParada = Utils.toLista(raíz.getElementsByTagName("itParada")).get(0);
		itParada = Integer.parseInt(elemItParada.getTextContent());

		// Calcular valores derivados
		double umbralItUsuario = iteraciones / Utils.log2(líneasPorIt);
		umbralIt = umbralItUsuario * getDistComprobacionesY() / porcentLíneas;
	}

	public double getUmbralIt() {
		return umbralIt;
	}

	/**
	 * @return Distancia a la que se encuentra cada una de las comprobaciones de estancamiento usadas en el cambio
	 * de entorno vertical. La distancia se mide en porcentaje de las líneas totales (0-1).
	 */
	public float getDistComprobacionesY() {
		return maxPorcentLíneas / numComprobaciones;
	}

	/**
	 * @return Número de entradas que deberían almacenarse en la memoria de cambio de entorno vertical
	 */
	public int getTamañoMemoriaY() {
		return ((Long) Math.round(getUmbralIt() * numComprobaciones * MULT_TAMAÑO_MEMORIA_Y)).intValue();
	}
}
