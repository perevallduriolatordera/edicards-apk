package net.ifeu.edicards.DataTier.NTV;

public class DepositoNTVLineaDTO {

    public String codigoArticulo;
    public int cantidad;
    public double precio;
    public double dto1;
    public double dto2;

    public DepositoNTVLineaDTO(String codigoArticulo, int cantidad, double precio, double dto1, double dto2) {
        this.codigoArticulo = codigoArticulo;
        this.cantidad = cantidad;
        this.precio = precio;
        this.dto1 = dto1;
        this.dto2 = dto2;
    }
}
