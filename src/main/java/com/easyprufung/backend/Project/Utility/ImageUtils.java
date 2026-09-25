package com.easyprufung.backend.Project.Utility;
import java.awt.image.BufferedImage;
import java.awt.Color;

public class ImageUtils {
    /**
     * Makes white (or near-white) pixels transparent.
     * @param image Input image (BufferedImage, any type)
     * @param tolerance Tolerance for white (0 = only pure white, 255 = all colors)
     * @return BufferedImage with alpha channel
     */
    public static BufferedImage removeWhiteBackground(BufferedImage image, int tolerance) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Ensure the output image has alpha channel
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                Color color = new Color(rgb, true);

                // Check if pixel is "white enough"
                if (isWhite(color, tolerance)) {
                    // Set alpha to 0 (transparent)
                    newImage.setRGB(x, y, 0x00FFFFFF & rgb);
                } else {
                    // Copy original pixel (with alpha if present)
                    newImage.setRGB(x, y, rgb | 0xFF000000);
                }
            }
        }
        return newImage;
    }

    private static boolean isWhite(Color color, int tolerance) {
        return color.getRed()   >= (255 - tolerance) &&
                color.getGreen() >= (255 - tolerance) &&
                color.getBlue()  >= (255 - tolerance);
    }
}