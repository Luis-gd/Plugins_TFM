/*********************************************
 * OPL 22.1.0.0 Model
 * Author: luis
 * Creation Date: 9 may. 2022 at 12:08:07
 *********************************************/
 // Constantes
 
// Valor máximo para las restricciones si estas se usan 
// float MaxPorcentajePasajerosPerdidos = 0.2;
// float MaxPorcentajeDineroPerdidoRegion = 0.2;
// float MaxDiferenciaCompanyias = 0.2;
// float MaxDiferenciaEconomicaAeropuertos = 0.2;
// float MaxConectividadPerdida = 0.2;

// Estructuras:
 
 tuple conexion {
  string salida;
  string entrada;
}

{conexion} conexiones = {};
{string} NombreAeropuertosEspanya = {};
{string} NombreAeropuertosSalida = {};
{string} NombreCompanyias={};
 
int Aeropuertos;
int AeropuertosEspanyoles;
int Companyias;

execute{  
  var llegada = "";
  var f = new IloOplInputFile("datos/aeropuertos_entradas.csv");
  var data = f.readline().split(",");
  while (!f.eof) {
    data = f.readline().split(",");
    NombreAeropuertosEspanya.add(data[0]);
    AeropuertosEspanyoles++;
  }
  f.close();
  
  f = new IloOplInputFile("datos/aeropuertos_salida.csv");
  data = f.readline().split(",");
  while (!f.eof) {
    data = f.readline().split(",");
    NombreAeropuertosSalida.add(data[0]);
    Aeropuertos++;
  }
  f.close();
  
  var f = new IloOplInputFile("datos/companyias.csv");
  var data = f.readline().split(",");
  while (!f.eof) {
    data = f.readline().split(",");
    if (data[0] != "UNKNOWN"){
	    NombreCompanyias.add(data[0]);
	    Companyias++;
   }	    
  }
  f.close();
  
  var f = new IloOplInputFile("datos/sir.csv");
  var data = f.readline().split(",");
  while (!f.eof) {
    data = f.readline().split(",");
	conexiones.add(data[2],data[1]);
  }
  f.close();
}

{string} ListaConexionesPorAeropuertoEspanyol[i in NombreAeropuertosEspanya];

execute{  
  var f = new IloOplInputFile("datos/sir.csv");
  var data = f.readline().split(",");
  var cont = 0;
  var anterior = "";
  var pasos = 0;
  while (!f.eof) {
		data = f.readline().split(",");
		ListaConexionesPorAeropuertoEspanyol[data[1]].add(data[2]); 
		
   }   
   f.close(); 	
   
}


{string} ListaConexionesPorSalida[i in NombreAeropuertosSalida];

execute{  
  var f = new IloOplInputFile("datos/sir.csv");
  var data = f.readline().split(",");
  var cont = 0;
  var anterior = "";
  var pasos = 0;
  while (!f.eof) {
		data = f.readline().split(",");
		ListaConexionesPorSalida[data[2]].add(data[1]); 
   }   
   
   f.close(); 	 
}

float ListaRiesgosEspanyoles[i in conexiones] = 0;

execute{  
  var f = new IloOplInputFile("datos/sir.csv");
  var data = f.readline().split(",");
  while (!f.eof) {
		data = f.readline().split(",");
		ListaRiesgosEspanyoles[conexiones.get(data[2],data[1])] 
		= ListaRiesgosEspanyoles[conexiones.get(data[2],data[1])] + Opl.floatValue(data[0]); 
   }   
   
   f.close(); 	
   //writeln(ListaRiesgosEspanyoles);
   
}

int ListaPasajeros[i in conexiones] = 0;

execute{
  var f = new IloOplInputFile("datos/pasajeros_por_vuelo.csv");
  var data = f.readline().split(",");
  while (!f.eof) {
		data = f.readline().split(",");
		ListaPasajeros[conexiones.get(data[2],data[1])] 
		= ListaPasajeros[conexiones.get(data[2],data[1])] + Opl.floatValue(data[0]); 
   }  
   
   f.close();
}

int ListaPasajerosCompanyia[i in conexiones] [c in NombreCompanyias] = 0;

execute{
  var f = new IloOplInputFile("datos/pasajeros_por_vuelo_y_companyias.csv");
  var data = f.readline().split(",");
  
  while (!f.eof) {
		data = f.readline().split(",");
		if (data[1] != "UNKNOWN" && data[3]!= null && data[2]!= null && data[1]!= null ){
			ListaPasajerosCompanyia[conexiones.get(data[3],data[2])][data[1]] 
				= ListaPasajerosCompanyia[conexiones.get(data[3],data[2])][data[1]] + Opl.floatValue(data[0]); 
  		}
  				
 }     
   f.close();
}


int vuelosEntrantesConexion[i in conexiones] = 0;
int vuelosSalientes[i in NombreAeropuertosSalida] = 0;
int vuelosSalientesAEspanya[i in NombreAeropuertosSalida] = 0;
float conectividadAeropuertosSalientes [i in NombreAeropuertosSalida] = 0;

execute{  
  var f = new IloOplInputFile("datos/datosParaConectividad.csv");
  var data = f.readline().split(",");
  var cont = 0;
  var anterior = "";
  var pasos = 0;
  while (!f.eof) {
		data = f.readline().split(",");
		vuelosEntrantesConexion[conexiones.get(data[0], data[1])] = Opl.intValue(data[2]);
		vuelosSalientesAEspanya[data[0]] = vuelosSalientesAEspanya[data[0]] + Opl.intValue(data[2]);
		vuelosSalientes[data[0]] = Opl.intValue(data[3]);
		conectividadAeropuertosSalientes[data[0]] = Opl.floatValue(data[4]); 

		
   } 
     
   for(var aeropuerto in NombreAeropuertosSalida){
     if (vuelosSalientes[aeropuerto] != 0){
       conectividadAeropuertosSalientes[aeropuerto] = conectividadAeropuertosSalientes[aeropuerto]*
     vuelosSalientesAEspanya[aeropuerto]/vuelosSalientes[aeropuerto];
     }
   }
   
   f.close(); 	
   
}


float ListaDineroConexion[i in conexiones] = 0;

execute{
  var f = new IloOplInputFile("datos/dinero_por_vuelo.csv");
  var data = f.readline().split(",");
  while (!f.eof) {
		data = f.readline().split(",");
		ListaDineroConexion[conexiones.get(data[2],data[1])]
		= ListaDineroConexion[conexiones.get(data[2],data[1])] + Opl.floatValue(data[0]); 
   }  
   
   f.close();
  //writeln(ListaDineroConexion);
}

int ListaNumerosVuelos[i in conexiones] = 0;

execute{
  var f = new IloOplInputFile("datos/sir.csv");
  var data = f.readline().split(",");
  while (!f.eof) {
		data = f.readline().split(",");
		ListaNumerosVuelos[conexiones.get(data[2],data[1])] 
		= ListaNumerosVuelos[conexiones.get(data[2],data[1])] +1; 
   }  
   
   f.close();
}

dvar boolean EstadoDeConexion[conexiones] in 0..1;

////////////

dexpr float pasajerosPorCompanyia[k in NombreCompanyias] = 1 - 
	(sum(i in conexiones) ListaPasajerosCompanyia[i][k]*EstadoDeConexion[i]/
		(0.000000000001+sum(i in conexiones) ListaPasajerosCompanyia[i][k]));

dexpr float media = (sum(w in NombreCompanyias) pasajerosPorCompanyia[w])/Companyias;


///////////////


dexpr float proporcionVuelosAeropuerto[j in NombreAeropuertosEspanya] = 
(1 - sum(i in ListaConexionesPorAeropuertoEspanyol[j]) (ListaNumerosVuelos[<i,j>]
  	 * EstadoDeConexion[<i,j>])/sum(i in ListaConexionesPorAeropuertoEspanyol[j])
  	  ListaNumerosVuelos[<i,j>]);
  	  
dexpr float media2 = ((1/AeropuertosEspanyoles) *  sum (e in NombreAeropuertosEspanya) 
  	 (1 - sum(i in ListaConexionesPorAeropuertoEspanyol[e]) (ListaNumerosVuelos[<i,e>]
  	 * EstadoDeConexion[<i,e>])/
  	 sum(i in ListaConexionesPorAeropuertoEspanyol[e]) ListaNumerosVuelos[<i,e>] ));

// Objetivo
dexpr float funcionSir = (sum(i in conexiones) (ListaRiesgosEspanyoles[i]*EstadoDeConexion[i])/
sum(i in conexiones) ListaRiesgosEspanyoles[i]);


//Objetivo
dexpr float funcionPerdidaPasajeros = (1 - sum(i in conexiones) (ListaPasajeros[i]
  	 * EstadoDeConexion[i])/sum(i in conexiones) ListaPasajeros[i]);
  
 // Objetivo (Dinero medio)
dexpr float funcionPerdidaTurismo = (1 - sum(i in conexiones) (ListaDineroConexion[i]* EstadoDeConexion[i])/
  	sum(i in conexiones) ListaDineroConexion[i]);
  	
// objetivo (Pasajeros por compañia)
dexpr float funcionHomogeneidadAerolineas = 
(sum (k in NombreCompanyias) abs( pasajerosPorCompanyia[k] - media ) / Companyias);

dexpr float funcionHomogeneidadTurismo = ((1/AeropuertosEspanyoles) * sum (j in NombreAeropuertosEspanya) abs(
  	proporcionVuelosAeropuerto[j] - media2));
  	
	
dexpr float funcionConectividadSalida = (sum(i in NombreAeropuertosSalida) (conectividadAeropuertosSalientes[i] *
(1 - (sum(j in ListaConexionesPorSalida[i]) (EstadoDeConexion[<i,j>] * 
    vuelosEntrantesConexion[<i,j>]) / (sum(j in ListaConexionesPorSalida[i]) 
  	vuelosEntrantesConexion[<i,j>]+0.0000000000001))))/sum(i in NombreAeropuertosSalida) conectividadAeropuertosSalientes[i]);
  	


float var1 = ...;
float var2 = ...;
float var3 = ...;
float var4 = ...;
float var5 = ...;
float var6 = ...;


 minimize
 	
 	var1 * funcionSir + var2*funcionPerdidaPasajeros + var3 *funcionPerdidaTurismo
    + var4 *funcionHomogeneidadAerolineas + var5 * funcionHomogeneidadTurismo + var6 * funcionConectividadSalida;
 
 
subject to{

}

tuple TakeSolutionT{ 
	conexion con;
	int value; 
};

{TakeSolutionT} TakeSolution = {<i, EstadoDeConexion[i]> | i in conexiones};
  
  
