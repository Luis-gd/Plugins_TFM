package earlywarn.mh;

public class Conexion {
    public String entrada;
    public String salida;

    Conexion(String entrada, String salida){
        this.entrada=entrada;
        this.salida=salida;
    }

    public int hashCode() { return entrada.hashCode()+salida.hashCode(); }

    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of Conexion or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof Conexion)) {
            return false;
        }

        // typecast o to Complex so that we can compare data members
        Conexion c = (Conexion) o;

        // Compare the data members and return accordingly
        return entrada.compareTo(c.entrada) == 0
                && salida.compareTo(c.salida) == 0;
    }
}
