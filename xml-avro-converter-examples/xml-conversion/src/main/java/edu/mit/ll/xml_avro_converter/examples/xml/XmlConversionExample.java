/**
 * Copyright 2016-2017 MIT Lincoln Laboratory, Massachusetts Institute of Technology
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This material is based upon work supported by the Federal Aviation
 * Administration under Air Force Contract No. FA8721-05-C-0002 and/or
 * FA8702-15-D-0001. Any opinions, findings, conclusions or recommendations
 * expressed in this material are those of the author(s) and do not
 * necessarily reflect the views of the Federal Aviation Administration.
 *
 * Delivered to the U.S. Government with Unlimited Rights, as defined in
 * DFARS Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright
 * notice, U.S. Government rights in this work are defined by DFARS
 * 252.227-7013 or DFARS 252.227-7014 as detailed above. Use of this work
 * other than as specifically authorized by the U.S. Government may violate
 * any copyrights that exist in this work.
 */

package edu.mit.ll.xml_avro_converter.examples.xml;

import edu.mit.ll.xml_avro_converter.AvroSchemaGenerator;
import edu.mit.ll.xml_avro_converter.AvroSerializer;
import edu.mit.ll.xml_avro_converter.XmlSerializer;
import edu.mit.ll.xml_avro_converter.examples.cities.CityCollection;
import edu.mit.ll.xml_avro_converter.examples.cities.ObjectFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.validation.SchemaFactory;
import org.apache.avro.Schema;
import org.w3c.dom.Document;

public class XmlConversionExample {

  public static void main(String[] args) throws Exception {
    JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
    javax.xml.validation.Schema newSchema = SchemaFactory
            .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            .newSchema(XmlConversionExample.class.getResource("/cities.xsd"));
    XmlSerializer<CityCollection> xmlSerializer = new XmlSerializer(
            jaxbContext, newSchema);
    InputStream citiesStream = XmlConversionExample.class.getResourceAsStream(
            "/cities.xml");
    CityCollection cities = xmlSerializer.readFromXml(citiesStream);

    AvroSchemaGenerator schemaGenerator = new AvroSchemaGenerator();
    Schema citiesSchema = schemaGenerator.generateSchema(CityCollection.class);
    AvroSerializer<CityCollection> avroSerializer =
            schemaGenerator.createAvroSerializer(citiesSchema);
    ByteArrayOutputStream avroByteStream = new ByteArrayOutputStream();
    avroSerializer.writeToAvro(avroByteStream, cities);
    byte[] avroBytes = avroByteStream.toByteArray();

    CityCollection citiesFromAvro = avroSerializer.readFromAvro(
            new ByteArrayInputStream(avroBytes));
    Document citiesFromAvroDoc = xmlSerializer.writeToXml(citiesFromAvro);
    XmlSerializer.printDocument(citiesFromAvroDoc, System.out, false);

    Document cityDocument = xmlSerializer.writeToXml(cities);
    ByteArrayOutputStream xmlBytes = new ByteArrayOutputStream();
    XmlSerializer.printDocument(cityDocument, xmlBytes, true);
    System.out.println(String.format("XML document size: %d bytes",
            xmlBytes.toByteArray().length));
    System.out.println(String.format(
            "Avro document size: %d bytes", avroBytes.length));
  }
}
