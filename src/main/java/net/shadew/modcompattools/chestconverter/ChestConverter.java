package net.shadew.modcompattools.chestconverter;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

public class ChestConverter {
    private final File inSingle;
    private final File inDouble;
    private final File outSingle;
    private final File outLeft;
    private final File outRight;
    private BufferedImage left;
    private BufferedImage right;
    private BufferedImage single;
    private BufferedImage srcSingle;
    private BufferedImage srcDouble;

    private boolean debug = false;
    private boolean flipSingle = false; // Somebody messed with the midnight chest textures - we need to flip back and front

    public ChestConverter(File inSingle, File inDouble, File outSingle, File outLeft, File outRight) {
        this.inSingle = inSingle;
        this.inDouble = inDouble;
        this.outSingle = outSingle;
        this.outLeft = outLeft;
        this.outRight = outRight;
    }

    public ChestConverter(File inSingle, File outSingle) {
        this(inSingle, null, outSingle, null, null);
    }

    public ChestConverter(File inDouble, File outLeft, File outRight) {
        this(null, inDouble, null, outLeft, outRight);
    }

    /**
     * Converts a single chest image from the 1.14- format to the 1.15+ format, but also flips the back and front textures.
     * This is one of the methods that calls the rest of the {@link ChestConverter} class.
     *
     * @param fromFolder The folder from which the images will be taken from.
     * @param toFolder The folder in which the converted images will be placed in.
     * @param name The name of the chest texture. This precedes the {@code .png} in the file name.
     */
    public static void convertSingleWithFlip(File fromFolder, File toFolder, String name) {
        ChestConverter conv = new ChestConverter(
            new File(fromFolder, name + ".png"),
            new File(toFolder, name + ".png")
        );
        conv.setFlipSingle(true);
        conv.convert();
    }

    /**
     * Converts a single chest image from the 1.14- format to the 1.15+ format.
     * This is one of the methods that calls the rest of the {@link ChestConverter} class.
     *
     * @param fromFolder The folder from which the images will be taken from.
     * @param toFolder The folder in which the converted images will be placed in.
     * @param name The name of the chest texture. This precedes the {@code .png} in the file name.
     */
    public static void convertSingle(File fromFolder, File toFolder, String name) {
        new ChestConverter(
            new File(fromFolder, name + ".png"),
            new File(toFolder, name + ".png")
        ).convert();
    }

    /**
     * Converts a single chest image from the 1.14- format to the 1.15+ format.
     * More specifically, the previous _double.png file will be converted into a {@code ..._left.png} file and a {@code ..._right.png} file.
     * This is one of the methods that calls the rest of the {@link ChestConverter} class.
     *
     * @param fromFolder The folder from which the images will be taken from.
     * @param toFolder The folder in which the converted images will be placed in.
     * @param name The name of the chest texture. This precedes the {@code _double.png} in the file name.
     */
    public static void convertDouble(File fromFolder, File toFolder, String name) {
        new ChestConverter(
            new File(fromFolder, name + "_double.png"),
            new File(toFolder, name + "_left.png"),
            new File(toFolder, name + "_right.png")
        ).convert();
    }

    /**
     * Converts a single and double chest images from the 1.14- format to the 1.15+ format.
     * This method takes the single and double images with the same name and runs them through the
     * {@code convertSingle} and {@code convertDouble} methods.
     *
     * @param fromFolder The folder from which the images will be taken from.
     * @param toFolder The folder in which the converted images will be placed in.
     * @param name The name of the chest texture. This precedes the {@code .png} and {@code _double.png} in the file names.
     */
    public static void convertBoth(File fromFolder, File toFolder, String name) {
        convertSingle(fromFolder, toFolder, name);
        convertDouble(fromFolder, toFolder, name);
    }

    private BufferedImage loadImage(File file) {
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void saveImage(BufferedImage img, File file) {
        try {
            ImageIO.write(img, "PNG", file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void copyRect(BufferedImage in, UV src, BufferedImage out, UV dest, int sox, int soy, boolean fx, boolean fy) {
        int w = Math.min(src.w, dest.w);
        int h = Math.min(src.h, dest.h);
        int sx = src.x;
        int sy = src.y;
        int dx = dest.x;
        int dy = dest.y;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int col = in.getRGB(x + sx + sox, y + sy + soy);
                int ux = fx ? w - x - 1 : x;
                int uy = fy ? h - y - 1 : y;
                out.setRGB(ux + dx, uy + dy, col);
            }
        }
    }

    private void splitCube(int w, int h, int l, int u1, int v1, int u2, int v2) {
        int i = w / 2;
        UVS from = new UVS(w, h, l, u1, v1);
        UVS to = new UVS(i, h, l, u2, v2);

        copyRect(srcDouble, from.n, right, to.s, 0, 0, true, true);
        copyRect(srcDouble, from.n, left, to.s, i, 0, true, true);

        copyRect(srcDouble, from.u, right, to.d, 0, 0, false, true);
        copyRect(srcDouble, from.u, left, to.d, i, 0, false, true);

        copyRect(srcDouble, from.d, right, to.u, 0, 0, false, true);
        copyRect(srcDouble, from.d, left, to.u, i, 0, false, true);

        // copyRect(src, from.e, right, to.e, 0, 0, true, true);
        copyRect(srcDouble, from.e, left, to.e, 0, 0, true, true);

        copyRect(srcDouble, from.w, right, to.w, 0, 0, true, true);
        // copyRect(src, from.w, left, to.w, i, 0, true, true);

        copyRect(srcDouble, from.s, right, to.n, i, 0, true, true);
        copyRect(srcDouble, from.s, left, to.n, 0, 0, true, true);
    }

    private void flipCube(int w, int h, int l, int u1, int v1, int u2, int v2) {
        UVS from = new UVS(w, h, l, u1, v1);
        UVS to = new UVS(w, h, l, u2, v2);

        if(flipSingle) {
            copyRect(srcSingle, from.u, single, to.d, 0, 0, false, false);
            copyRect(srcSingle, from.d, single, to.u, 0, 0, false, false);
            copyRect(srcSingle, from.n, single, to.n, 0, 0, true, true);
            copyRect(srcSingle, from.e, single, to.e, 0, 0, false, true);
            copyRect(srcSingle, from.w, single, to.w, 0, 0, false, true);
            copyRect(srcSingle, from.s, single, to.s, 0, 0, true, true);
        } else {
            copyRect(srcSingle, from.u, single, to.d, 0, 0, false, true);
            copyRect(srcSingle, from.d, single, to.u, 0, 0, false, true);
            copyRect(srcSingle, from.n, single, to.s, 0, 0, true, true);
            copyRect(srcSingle, from.e, single, to.e, 0, 0, true, true);
            copyRect(srcSingle, from.w, single, to.w, 0, 0, true, true);
            copyRect(srcSingle, from.s, single, to.n, 0, 0, true, true);
        }
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setFlipSingle(boolean flipSingle) {
        this.flipSingle = flipSingle;
    }

    public void convert() {
        if(inDouble != null) {
            left = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            right = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            srcDouble = loadImage(inDouble);

            if(debug) {
                UVS chest = new UVS(15, 10, 14, 0, 19);
                UVS lid = new UVS(15, 5, 14, 0, 0);
                UVS button = new UVS(1, 4, 1, 0, 0);
                chest.test(left);
                chest.test(right);
                lid.test(left);
                lid.test(right);
                button.test(left);
                button.test(right);
            }

            splitCube(30, 10, 14, 0, 19, 0, 19);
            splitCube(30, 5, 14, 0, 0, 0, 0);
            splitCube(2, 4, 1, 0, 0, 0, 0);

            saveImage(left, outLeft);
            saveImage(right, outRight);
        }

        if(inSingle != null) {
            single = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            srcSingle = loadImage(inSingle);

            if(debug) {
                UVS chest = new UVS(14, 10, 14, 0, 19);
                UVS lid = new UVS(14, 5, 14, 0, 0);
                UVS button = new UVS(2, 4, 1, 0, 0);
                chest.test(single);
                lid.test(single);
                button.test(single);
            }

            flipCube(14, 10, 14, 0, 19, 0, 19);
            flipCube(14, 5, 14, 0, 0, 0, 0);
            flipCube(2, 4, 1, 0, 0, 0, 0);

            saveImage(single, outSingle);
        }
    }

    private static class UV {
        private final int x;
        private final int y;
        private final int w;
        private final int h;

        private UV(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        private void test(Graphics2D g) {
            g.fillRect(x, y, w, h);
        }
    }

    private static class UVS {
        private final UV n;
        private final UV e;
        private final UV s;
        private final UV w;
        private final UV u;
        private final UV d;

        private UVS(int wdt, int hgt, int len, int texu, int texv) {
            int uvw = len + wdt + len + wdt;
            int udw = wdt + wdt;

            int du = texu + uvw / 2;
            int uu = du - udw / 2;
            u = new UV(uu, texv, wdt, len);
            d = new UV(du, texv, wdt, len);

            int neswv = texv + len;
            w = new UV(texu, neswv, len, hgt);

            int nu = texu + len;
            n = new UV(nu, neswv, wdt, hgt);

            int eu = nu + wdt;
            e = new UV(eu, neswv, len, hgt);

            int su = eu + len;
            s = new UV(su, neswv, wdt, hgt);
        }

        public void test(BufferedImage img) {
            Graphics2D g = (Graphics2D) img.getGraphics();

            g.setColor(Color.BLUE);
            n.test(g);
            g.setColor(Color.GREEN);
            e.test(g);
            g.setColor(Color.YELLOW);
            s.test(g);
            g.setColor(Color.RED);
            w.test(g);
            g.setColor(Color.ORANGE);
            u.test(g);
            g.setColor(Color.MAGENTA);
            d.test(g);

            g.dispose();
        }
    }
}
