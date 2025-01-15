package net.ifeu.edicards.Html.notification.customer;

import net.ifeu.edicards.Constants.ConstantsFolders;
import net.ifeu.edicards.DataTier.Deposito;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class HtmlCustomerNotification implements ICustomerNotification<Deposito> {
    @Override
    public void notify(Deposito deposito) {
       String content = loadEmailTemplate();
       content = replaceData(deposito, content);

       String path = "/sdcard/" + ConstantsFolders.FOLDER_ROOT + "/" + ConstantsFolders.FOLDER_ENVIOS_CLIENTE + "/"
               + deposito.Cliente.Mail + "#" + deposito.NumeroAlbaran + ".html";

        try (FileWriter writer = new FileWriter(path)) {
            writer.write(content);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static String replaceData(Deposito deposito, String content) {
        content = content.replace("{{NombreCliente}}", deposito.Cliente.Razon);
        content = content.replace("{{idAlbaran}}", deposito.NumeroAlbaran);
        content = content.replace("{{formaPago}}", deposito.PagoDescripcion);

        return content;
    }
    private static String loadEmailTemplate() {
        ClassLoader classLoader = HtmlCustomerNotification.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("templates/customer_notification.html");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            return reader.lines().collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar la plantilla de email", e);
        }
    }

}
