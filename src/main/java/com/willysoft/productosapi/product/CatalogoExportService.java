package com.willysoft.productosapi.product;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.willysoft.productosapi.product.dto.CategoriaProductos;
import com.willysoft.productosapi.product.dto.ProductResponse;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Genera la "Lista de Precios" (catálogo agrupado por categoría) en Excel y PDF.
 */
@Service
public class CatalogoExportService {

    private static final Color VERDE = new Color(59, 109, 17);

    private DecimalFormat formatoMoneda() {
        DecimalFormatSymbols s = new DecimalFormatSymbols(new Locale("es", "AR"));
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", s);
    }

    private String iva(BigDecimal alicuota) {
        return (alicuota != null ? formatoMoneda().format(alicuota) : "0,00") + " %";
    }

    private String fechaTexto() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /** Bytes del logo de la app (logui.png). Null si no se encuentra (la exportación sigue sin logo). */
    private byte[] logoBytes() {
        try (InputStream is = getClass().getResourceAsStream("/static/img/logui.png")) {
            return is != null ? is.readAllBytes() : null;
        } catch (IOException e) {
            return null;
        }
    }

    // ──────────────────────────── Excel ────────────────────────────

    public byte[] toExcel(List<CategoriaProductos> grupos) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Lista de precios");

            org.apache.poi.ss.usermodel.Font fTitulo = wb.createFont();
            fTitulo.setBold(true);
            fTitulo.setFontHeightInPoints((short) 16);
            CellStyle titulo = wb.createCellStyle();
            titulo.setFont(fTitulo);

            org.apache.poi.ss.usermodel.Font fCat = wb.createFont();
            fCat.setBold(true);
            fCat.setColor(IndexedColors.WHITE.getIndex());
            CellStyle catStyle = wb.createCellStyle();
            catStyle.setFont(fCat);
            catStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            catStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            org.apache.poi.ss.usermodel.Font fHead = wb.createFont();
            fHead.setBold(true);
            CellStyle header = wb.createCellStyle();
            header.setFont(fHead);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle money = wb.createCellStyle();
            money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));

            // Logo de la app arriba a la derecha (si está disponible).
            byte[] logo = logoBytes();
            if (logo != null) {
                int pic = wb.addPicture(logo, Workbook.PICTURE_TYPE_PNG);
                CreationHelper helper = wb.getCreationHelper();
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setCol1(3);
                anchor.setRow1(0);
                Picture picture = drawing.createPicture(anchor, pic);
                picture.resize(0.16);
            }

            int r = 0;
            Row tRow = sheet.createRow(r++);
            Cell tCell = tRow.createCell(0);
            tCell.setCellValue("Lista de Precios — WillySoft · invenFact");
            tCell.setCellStyle(titulo);

            Row fRow = sheet.createRow(r++);
            fRow.createCell(0).setCellValue("Generado: " + fechaTexto());
            r++; // fila en blanco

            for (CategoriaProductos g : grupos) {
                Row catRow = sheet.createRow(r++);
                Cell c = catRow.createCell(0);
                c.setCellValue(g.categoria().nombre() + "  —  IVA " + iva(g.categoria().alicuotaIva()));
                c.setCellStyle(catStyle);

                Row hRow = sheet.createRow(r++);
                String[] cols = {"Producto", "Precio final (ARS)", "Precio final (USD)"};
                for (int i = 0; i < cols.length; i++) {
                    Cell hc = hRow.createCell(i);
                    hc.setCellValue(cols[i]);
                    hc.setCellStyle(header);
                }

                for (ProductResponse p : g.productos()) {
                    Row pr = sheet.createRow(r++);
                    pr.createCell(0).setCellValue(p.nombre());
                    Cell ars = pr.createCell(1);
                    if (p.precioFinalArs() != null) {
                        ars.setCellValue(p.precioFinalArs().doubleValue());
                        ars.setCellStyle(money);
                    }
                    if (p.precioFinalUsd() != null) {
                        Cell usd = pr.createCell(2);
                        usd.setCellValue(p.precioFinalUsd().doubleValue());
                        usd.setCellStyle(money);
                    }
                }
                r++; // separación entre categorías
            }

            sheet.setColumnWidth(0, 12000);
            sheet.setColumnWidth(1, 5000);
            sheet.setColumnWidth(2, 5000);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel", e);
        }
    }

    // ───────────────────────────── PDF ─────────────────────────────

    public byte[] toPdf(List<CategoriaProductos> grupos) {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Logo de la app (si está disponible).
            byte[] logo = logoBytes();
            if (logo != null) {
                Image img = Image.getInstance(logo);
                img.scaleToFit(190, 60);
                doc.add(img);
            }

            Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, VERDE);
            Paragraph titulo = new Paragraph("Lista de Precios", fTitulo);
            titulo.setSpacingBefore(6);
            doc.add(titulo);
            Font fSub = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            doc.add(new Paragraph("WillySoft · invenFact", fSub));
            doc.add(new Paragraph("Generado: " + fechaTexto(), fSub));

            Font fCat = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.WHITE);
            Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font fCell = FontFactory.getFont(FontFactory.HELVETICA, 10);

            for (CategoriaProductos g : grupos) {
                PdfPTable catTable = new PdfPTable(1);
                catTable.setWidthPercentage(100);
                catTable.setSpacingBefore(12);
                catTable.setSpacingAfter(4);
                PdfPCell catCell = new PdfPCell(new Phrase(
                        g.categoria().nombre() + "   (IVA " + iva(g.categoria().alicuotaIva()) + ")", fCat));
                catCell.setBackgroundColor(VERDE);
                catCell.setPadding(6);
                catCell.setBorder(0);
                catTable.addCell(catCell);
                doc.add(catTable);

                PdfPTable table = new PdfPTable(new float[]{5f, 2.5f, 2.5f});
                table.setWidthPercentage(100);
                headerCell(table, "Producto", fHead, Element.ALIGN_LEFT);
                headerCell(table, "Final (ARS)", fHead, Element.ALIGN_RIGHT);
                headerCell(table, "Final (USD)", fHead, Element.ALIGN_RIGHT);

                DecimalFormat fmt = formatoMoneda();
                for (ProductResponse p : g.productos()) {
                    dataCell(table, p.nombre(), fCell, Element.ALIGN_LEFT);
                    dataCell(table, p.precioFinalArs() != null ? "$ " + fmt.format(p.precioFinalArs()) : "—",
                            fCell, Element.ALIGN_RIGHT);
                    dataCell(table, p.precioFinalUsd() != null ? "US$ " + fmt.format(p.precioFinalUsd()) : "—",
                            fCell, Element.ALIGN_RIGHT);
                }
                doc.add(table);
            }

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    private void headerCell(PdfPTable table, String texto, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(new Color(90, 140, 50));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void dataCell(PdfPTable table, String texto, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(4);
        table.addCell(cell);
    }
}
