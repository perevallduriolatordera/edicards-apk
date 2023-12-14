package net.ifeu.edicards.Pdf.document;

public interface IPdfDocumentGenerator {

    boolean createAlbaran(String guid, boolean isTransferPayment);
    boolean createDeposito(String guid);
}
