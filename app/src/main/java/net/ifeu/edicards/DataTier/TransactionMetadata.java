package net.ifeu.edicards.DataTier;

import java.util.UUID;

public class TransactionMetadata {

    public String GUID;
    public String NewCustomerFrontDocument;
    public String NewCustomerBackDocument;

    public String IngresoDocument;

    public TransactionMetadata() {
        GUID = UUID.randomUUID().toString();

    }
}
