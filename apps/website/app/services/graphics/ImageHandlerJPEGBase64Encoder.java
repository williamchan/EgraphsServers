package services.graphics;

/*

   Copyright 2001,2003  The Apache Software Foundation

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

import org.apache.batik.svggen.DefaultImageHandler;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.apache.batik.svggen.SVGGraphics2DRuntimeException;
import org.apache.batik.util.Base64EncoderStream;
import org.apache.batik.util.XMLConstants;
import org.w3c.dom.Element;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * Wanton adaptation of batik 1.6 source code for the PNG version of the base64 encoder.
 * Does it by embedding JPG instead. This results in much more manageable SVG size when
 * drawing images.
 *
 * @see org.apache.batik.svggen.ImageHandlerBase64Encoder
 */
public class ImageHandlerJPEGBase64Encoder extends DefaultImageHandler {

    private static String DATA_PROTOCOL_JPEG_PREFIX = "data:image/jpeg;base64,";

    private final int quality;
    /**
     * Build an <code>ImageHandlerBase64Encoder</code> instance.
     */
    public ImageHandlerJPEGBase64Encoder(int quality) {
        super();

        this.quality = quality;
    }

    /**
     * The handler should set the xlink:href tag and the width and
     * height attributes.
     */
    public void handleHREF(Image image, Element imageElement,
                           SVGGeneratorContext generatorContext)
            throws SVGGraphics2DIOException {
        if (image == null)
            throw new SVGGraphics2DRuntimeException(ERR_IMAGE_NULL);

        int width = image.getWidth(null);
        int height = image.getHeight(null);

        if (width==0 || height==0) {
            handleEmptyImage(imageElement);
        } else {
            if (image instanceof RenderedImage) {
                handleHREF((RenderedImage)image, imageElement,
                        generatorContext);
            } else {
                BufferedImage buf =
                        new BufferedImage(width, height,
                                BufferedImage.TYPE_BYTE_GRAY);

                Graphics2D g = buf.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
                handleHREF((RenderedImage)buf, imageElement,
                        generatorContext);
            }
        }
    }

    /**
     * The handler should set the xlink:href tag and the width and
     * height attributes.
     */
    public void handleHREF(RenderableImage image, Element imageElement,
                           SVGGeneratorContext generatorContext)
            throws SVGGraphics2DIOException {
        if (image == null){
            throw new SVGGraphics2DRuntimeException(ERR_IMAGE_NULL);
        }

        RenderedImage r = image.createDefaultRendering();
        if (r == null) {
            handleEmptyImage(imageElement);
        } else {
            handleHREF(r, imageElement, generatorContext);
        }
    }

    protected void handleEmptyImage(Element imageElement) {
        imageElement.setAttributeNS(XMLConstants.XLINK_NAMESPACE_URI,
                ATTR_XLINK_HREF, DATA_PROTOCOL_JPEG_PREFIX);
        imageElement.setAttributeNS(null, SVG_WIDTH_ATTRIBUTE, "0");
        imageElement.setAttributeNS(null, SVG_HEIGHT_ATTRIBUTE, "0");
    }

    /**
     * This version of handleHREF encodes the input image into a
     * JPEG image whose bytes are then encoded with Base64. The
     * resulting encoded data is used to set the url on the
     * input imageElement, using the data: protocol.
     */
    public void handleHREF(RenderedImage image, Element imageElement,
                           SVGGeneratorContext generatorContext)
            throws SVGGraphics2DIOException {

        //
        // Setup Base64Encoder stream to byte array.
        //
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Base64EncoderStream b64Encoder = new Base64EncoderStream(os);
        try {
            //
            // Now, encode the input image to the base 64 stream.
            //
            encodeImage(image, b64Encoder);

            // Close the b64 encoder stream (terminates the b64 streams).
            b64Encoder.close();
        } catch (IOException e) {
            // Should not happen because we are doing in-memory processing
            throw new SVGGraphics2DIOException(ERR_UNEXPECTED, e);
        }

        //
        // Finally, write out url
        //
        imageElement.setAttributeNS(XMLConstants.XLINK_NAMESPACE_URI,
                ATTR_XLINK_HREF,
                DATA_PROTOCOL_JPEG_PREFIX +
                        os.toString());

    }

    public void encodeImage(RenderedImage buf, OutputStream os)
            throws SVGGraphics2DIOException {
        try{
            ImageIO.write(buf, "jpeg", os);
        } catch(IOException e) {
            // We are doing in-memory processing. This should not happen.
            throw new SVGGraphics2DIOException(ERR_UNEXPECTED);
        }
    }

    public BufferedImage buildBufferedImage(Dimension size) {
        return new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_BYTE_GRAY);
    }
}