package net.ifeu.edicards.Pdf.document;

public interface IPdfDocumentGenerator {

    void createAlbaran(String guid, boolean isTransferPayment);
    void createDeposito(String guid);
}
