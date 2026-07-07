package com.bank.shared.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A dependency-free generator for a single-page PDF containing lines of text.
 * Deterministic: identical input always yields identical bytes, which lets a
 * statement's SHA-256 digest act as an immutability seal (FR-17.2).
 */
public final class SimplePdf {

    private SimplePdf() {}

    public static byte[] of(List<String> lines) {
        StringBuilder content = new StringBuilder("BT /F1 12 Tf 50 760 Td 16 TL\n");
        for (String line : lines) {
            content.append('(').append(escape(line)).append(") Tj T*\n");
        }
        content.append("ET");
        String stream = content.toString();
        int streamLen = stream.getBytes(StandardCharsets.ISO_8859_1).length;

        List<String> bodies = new ArrayList<>();
        bodies.add("<< /Type /Catalog /Pages 2 0 R >>");
        bodies.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        bodies.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                + "/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>");
        bodies.add("<< /Length " + streamLen + " >>\nstream\n" + stream + "\nendstream");
        bodies.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            int[] offsets = new int[bodies.size() + 1];
            int pos = write(out, "%PDF-1.4\n");
            for (int i = 0; i < bodies.size(); i++) {
                offsets[i + 1] = pos;
                pos += write(out, (i + 1) + " 0 obj\n" + bodies.get(i) + "\nendobj\n");
            }
            int xrefStart = pos;
            StringBuilder xref = new StringBuilder("xref\n0 " + (bodies.size() + 1) + "\n");
            xref.append("0000000000 65535 f \n");
            for (int i = 1; i <= bodies.size(); i++) {
                xref.append(String.format("%010d 00000 n \n", offsets[i]));
            }
            xref.append("trailer\n<< /Size ").append(bodies.size() + 1)
                    .append(" /Root 1 0 R >>\nstartxref\n").append(xrefStart).append("\n%%EOF");
            write(out, xref.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static int write(ByteArrayOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
        out.write(bytes);
        return bytes.length;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
