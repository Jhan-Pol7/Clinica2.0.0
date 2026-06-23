package pe.edu.upeu.clinica.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

// Generador de códigos QR a partir de texto, usando ZXing.
// Lo usan el ticket Jasper (imagen QR en el reporte) y cualquier otra vista
// que quiera mostrar un QR. Mantiene la API mínima: texto -> BufferedImage.
public final class QrGenerator {

    private QrGenerator() {}

    // Crea una imagen QR cuadrada de 'size' píxeles de lado a partir del texto.
    // Corrección de errores nivel M (equilibrio entre densidad y tolerancia a
    // manchas de la ticketera térmica). UTF-8 para tildes y la "ñ".
    public static BufferedImage generar(String texto, int size) {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = new QRCodeWriter()
                    .encode(texto == null ? "" : texto, BarcodeFormat.QR_CODE, size, size, hints);

            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    img.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return img;
        } catch (Exception e) {
            throw new RuntimeException("Error generando QR: " + e.getMessage(), e);
        }
    }
}
