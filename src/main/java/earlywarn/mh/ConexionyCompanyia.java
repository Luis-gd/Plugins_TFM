package earlywarn.mh;

public class ConexionyCompanyia {
    public Conexion conexion;
    public String companyia;

    ConexionyCompanyia(Conexion conexion, String companyia){
        this.conexion=conexion;
        this.companyia=companyia;
    }

    public int hashCode() { return companyia.hashCode()+conexion.hashCode(); }

    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of Conexion or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof ConexionyCompanyia)) {
            return false;
        }

        // typecast o to Complex so that we can compare data members
        ConexionyCompanyia c = (ConexionyCompanyia) o;

        // Compare the data members and return accordingly
        return companyia.compareTo(c.companyia) == 0
                && conexion.equals(c.conexion) == true;
    }
}
