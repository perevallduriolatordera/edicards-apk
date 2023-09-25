package net.ifeu.library.Csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CsvCreator {

    private String path;
    private CSVPrinter csvPrinter;

    public CsvCreator(String path, String... headers) {
        this.path = path;

        try {
            FileWriter file = new FileWriter(path);
            BufferedWriter writer = new BufferedWriter(file);
            csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withDelimiter(';'));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addLine(Object... recordValues) {
        try {
            this.csvPrinter.printRecord(recordValues);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void flush() {
        try {
            this.csvPrinter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
