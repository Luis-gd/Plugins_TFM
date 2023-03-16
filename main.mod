/*********************************************
 * OPL 22.1.0.0 Model
 * Author: Luis
 * Creation Date: 8 jun. 2022 at 13:24:10
 *********************************************/

float pesosEpidemiologicos[0..26] = [0.10, 0.20, 0.30, 0.40, 0.50, 0.60, 0.70, 0.80, 0.90,
0.45, 0.4, 0.35, 0.30, 0.25, 0.20, 0.15, 0.10, 0.05,  
0.45, 0.4, 0.35, 0.30, 0.25, 0.20, 0.15, 0.10, 0.05];

 
float pesosEconomicos[0..26] = [0.45, 0.4, 0.35, 0.30, 0.25, 0.20, 0.15, 0.10, 0.05,
0.10, 0.20, 0.30, 0.40, 0.50, 0.60, 0.70, 0.80, 0.90,
0.45, 0.4, 0.35, 0.30, 0.25, 0.20, 0.15, 0.10, 0.05];
 
float pesosSociales [0..26]= [0.45, 0.4, 0.35, 0.30, 0.25, 0.20, 0.15, 0.10, 0.05,  
0.45, 0.4, 0.35, 0.30, 0.25, 0.20, 0.15, 0.10, 0.05,
0.10, 0.20, 0.30, 0.40, 0.50, 0.60, 0.70, 0.80, 0.90];
 


main {
  var source = new IloOplModelSource("criterios.mod");
  var cplex = new IloCplex();
  var def = new IloOplModelDefinition(source);
  var ofile = new IloOplOutputFile("EjecucionCplex2.txt");


  for(var k=0;k<=26;k++){
	  var opl = new IloOplModel(def,cplex);
	
	  var data2= new IloOplDataElements();
	  data2.var1 = thisOplModel.pesosEpidemiologicos[k];
	  data2.var2 = thisOplModel.pesosEconomicos[k]/4;
	  data2.var3 = thisOplModel.pesosEconomicos[k]/4;
	  data2.var4 = thisOplModel.pesosEconomicos[k]/4;
	  data2.var5 = thisOplModel.pesosEconomicos[k]/4;
	  data2.var6 = thisOplModel.pesosSociales[k];
	  opl.addDataSource(data2);
	  opl.generate();
	  if (cplex.solve()) {  
		opl.postProcess();
	  	ofile.writeln("Numero de iteracion: " + k);
	  	ofile.writeln("Solucion: " + opl.TakeSolution);
	  	ofile.writeln("Pesos usados: " + thisOplModel.pesosEpidemiologicos[k] + " " +  thisOplModel.pesosEconomicos[k]+ " " + thisOplModel.pesosSociales[k])
		ofile.writeln("Valor de la funcion objetivo SIR (porcentaje actual): " + opl.funcionSir);
		var agregacion =(opl.funcionHomogeneidadTurismo + opl.funcionPerdidaPasajeros + opl.funcionPerdidaTurismo + opl.funcionHomogeneidadAerolineas)/4
		ofile.writeln("Valor de la funcion agregacion: " + agregacion);
		//ofile.writeln("Valor de la funcion homogeneidad turismo: " + opl.funcionHomogeneidadTurismo );
		//ofile.writeln("Valor de la funcion perdida pasajeros: " + opl.funcionPerdidaPasajeros );
		//ofile.writeln("Valor de la perdida turismo: " + opl.funcionPerdidaTurismo );
		//ofile.writeln("Valor de la homogeneidad aerolineas: " + opl.funcionHomogeneidadAerolineas );
		ofile.writeln("Valor de la funcion objetivo Conectividad (porcentaje perdido): " + opl.funcionConectividadSalida);
	  	ofile.writeln("------------------------------------------------------------------------------------");
		ofile.writeln("------------------------------------------------------------------------------------");
		writeln("Iteración terminada");
	
	  } else {
	     writeln("No solution");
	  }
	 opl.end();
	} 
	ofile.close(); 


  
	/*thisOplModel.generate();
	writeln(thisOplModel.var1);
	var data = new IloOplDataElements();
	data.var1=20;
	thisOplModel.addDataSource(data),
	writeln(data.var1);*/
  //
  //thisOplModel.var1 += 1;
  //;
    
}