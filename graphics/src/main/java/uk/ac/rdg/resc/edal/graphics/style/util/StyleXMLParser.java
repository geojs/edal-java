package uk.ac.rdg.resc.edal.graphics.style.util;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import uk.ac.rdg.resc.edal.graphics.style.ColourMap;
import uk.ac.rdg.resc.edal.graphics.style.ColourScale;
import uk.ac.rdg.resc.edal.graphics.style.ColourScheme;
import uk.ac.rdg.resc.edal.graphics.style.FlatOpacity;
import uk.ac.rdg.resc.edal.graphics.style.MapImage;
import uk.ac.rdg.resc.edal.graphics.style.PaletteColourScheme;
import uk.ac.rdg.resc.edal.graphics.style.RasterLayer;

public class StyleXMLParser {
    public static class ColorAdapter extends XmlAdapter<String, Color> {
        @Override
        public Color unmarshal(String s) {
            if (s.length() == 7) {
                return Color.decode(s);
            } else if (s.length() == 9) {
                Color color = Color.decode("#"+s.substring(3));
                int alpha = Integer.parseInt(s.substring(1,3), 16);
                return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            } else {
                return null;
            }
        }

        @Override
        public String marshal(Color c) {
            return "#" + String.format("%08X", c.getRGB());
        }
    }
    
    public static class FlatOpacityAdapter extends XmlAdapter<String, FlatOpacity> {
        @Override
        public FlatOpacity unmarshal(String v) throws Exception {
            return new FlatOpacity(Float.parseFloat(v));
        }

        @Override
        public String marshal(FlatOpacity v) throws Exception {
            return v.getOpacity().toString();
        }
    }

    public static String serialise(MapImage image) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(MapImage.class);

        Marshaller marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        StringWriter stringWriter = new StringWriter();
        marshaller.marshal(image, stringWriter);

        return stringWriter.toString();
    }

    public static MapImage deserialise(String xmlString) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(MapImage.class);

        Unmarshaller unmarshaller = context.createUnmarshaller();

        MapImage image = (MapImage) unmarshaller.unmarshal(new StringReader(xmlString));

        return image;
    }
    
    public static void generateSchema(final String path) throws IOException, JAXBException {
        JAXBContext context = JAXBContext.newInstance(MapImage.class);
        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                System.out.println(namespaceUri+", "+suggestedFileName);
                return new StreamResult(new File(path,suggestedFileName));
            }
        });
    }
    
    public static void main(String[] args) throws JAXBException, IOException {
//        generateSchema("/home/guy/");
        
        MapImage image = new MapImage();
//        StippleLayer layer = new StippleLayer("foam/TMP", new PatternScale(5, 305f, 280f, false));
//        image.getLayers().add(layer);
//        System.out.println(serialise(image));
//        
//        System.out.println();
//        
//        image = new Image();
        ColourScale scaleRange = new ColourScale(270f, 302f, false);
        ColourMap colourPalette = new ColourMap(Color.green, Color.cyan, new Color(0, 0, 0, 0), "rainbow", 20);
        ColourScheme colourScheme = new PaletteColourScheme(scaleRange, colourPalette);
        RasterLayer rasterLayer = new RasterLayer("TMP", colourScheme);
        rasterLayer.setOpacityTransform(new FlatOpacity(0.5f));
        image.getLayers().add(rasterLayer);
        System.out.println(serialise(image));
    }
}