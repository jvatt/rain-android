package net.vatt.jal.weather;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ltr on 28.9.2017.
 */

class FmiXmlParser {
    private final String ns = null;
    private final XmlPullParser parser;

    class WeatherDataTuple {
        public final List<String> dataRecord;
        public final String data;
        public final String location;

        private WeatherDataTuple(List<String> dataRecord, String data, String location) {
            this.dataRecord = dataRecord;
            this.data = data;
            this.location = location;
        }
    }

    FmiXmlParser() {
        parser = Xml.newPullParser();
    }

    // Parses weather information from data returned by the FMI api stored query: fmi::forecast::hirlam::surface::point::multipointcoverage
    WeatherDataTuple readFeed(String feed) {

        List<String> dataRecord = new ArrayList<>();
        String data = "";
        String location = "";

        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(feed));
            parser.nextTag();

            parser.require(XmlPullParser.START_TAG, ns, "wfs:FeatureCollection");
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name.equals("gml:DataBlock")) {
                    data = readDataBlock(parser);
                } else if (name.equals("swe:DataRecord")) {
                    dataRecord = readDataRecord(parser);
                    break; // All done
                }
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return new WeatherDataTuple(dataRecord, data, location);
    }


    // Returns the contents of gml:DataBlock
    private String readDataBlock(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "gml:DataBlock");
        String data = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("gml:doubleOrNilReasonTupleList")) {
                data = readText(parser);
            } else {
                skip(parser);
            }
        }
        return data;
    }

    // Returns the DataRecord as a  list of strings
    private List<String> readDataRecord(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "swe:DataRecord");
        List<String> dataRecord = new ArrayList<>();
        while (parser.next() != XmlPullParser.END_TAG) {
            if(parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if(name.equals("swe:field")
                    && parser.getAttributeCount() > 0
                    && parser.getAttributeName(0).equals("name")) {
                dataRecord.add(parser.getAttributeValue(0));
                parser.next();
            }
        }
        return dataRecord;
    }

    // Extracts text value
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Skips a tag
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
