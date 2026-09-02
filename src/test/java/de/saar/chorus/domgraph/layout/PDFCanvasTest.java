package de.saar.chorus.domgraph.layout;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;

import org.junit.Test;

public class PDFCanvasTest {
    @Test
    public void exportsPdf() throws Exception {
        File output = File.createTempFile("utool-openpdf-", ".pdf");
        output.deleteOnExit();

        PDFCanvas canvas = new PDFCanvas(output.getAbsolutePath());
        canvas.drawNodeAt(10, 10, "node", "test", null, "test");
        canvas.finish();

        FileInputStream input = new FileInputStream(output);
        try {
            byte[] header = new byte[4];
            assertEquals(4, input.read(header));
            assertEquals("%PDF", new String(header, "US-ASCII"));
        } finally {
            input.close();
        }
    }
}
