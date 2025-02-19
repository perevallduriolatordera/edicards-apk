package net.ifeu.edicards.Pdf.document;

import net.ifeu.edicards.DataTier.DepositoModalidad;

public interface IPdfDocumentGenerator {

    void createAlbaran(String guid, boolean isTransferPayment, DepositoModalidad modalidad);
    void createDeposito(String guid, DepositoModalidad modalidad);
}
