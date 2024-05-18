package net.ifeu.edicards.DataTier.NTV;

import java.util.List;
import java.util.Map;

public class DepositoNTVDTO {
    public String IdCliente;
    public Map<String, DepositoNTVLineaDTO> Lineas;
    public String File;

    public DepositoNTVDTO(String idCliente, Map<String, DepositoNTVLineaDTO> lineas) {
        this.IdCliente = idCliente;
        this.Lineas = lineas;
    }
}


