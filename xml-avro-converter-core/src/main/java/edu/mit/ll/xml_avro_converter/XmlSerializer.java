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

package edu.mit.ll.xml_avro_converter;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import org.w3c.dom.Document;

// Serializes objects to/from XML
public class XmlSerializer<T> {

  private final JAXBContext jaxbContext;
  private final Schema xmlSchema;
  private NamespacePrefixMapper namespacePrefixMapper;

  public XmlSerializer(JAXBContext jaxbContext, Schema xmlSchema) {
    this.xmlSchema = xmlSchema;
    this.jaxbContext = jaxbContext;
  }

  private Marshaller createJaxbMarshaller() throws JAXBException {
    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    if (namespacePrefixMapper != null) {
      jaxbMarshaller.setProperty(
              "com.sun.xml.bind.namespacePrefixMapper", namespacePrefixMapper);
    }
    jaxbMarshaller.setSchema(xmlSchema);
    return jaxbMarshaller;
  }

  private Unmarshaller createJaxbUnmarshaller() throws JAXBException {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    unmarshaller.setSchema(xmlSchema);
    return unmarshaller;
  }

  public T readFromXml(InputStream is) throws JAXBException {
    /*
     * jaxbUnmarshaller should not be cached (it is not threadsafe)
     */
    Unmarshaller jaxbUnmarshaller = createJaxbUnmarshaller();
    Object unmarshalledObj = jaxbUnmarshaller.unmarshal(is);
    return (T) unmarshalledObj;
  }

  public Document writeToXml(T object)
          throws JAXBException, ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
    Document document = documentBuilder.newDocument();
    createJaxbMarshaller().marshal(object, document);

    return document;
  }

  // Prints a document to an output stream
  public static void printDocument(Document document, OutputStream out,
          boolean compact) {
    Transformer transformer;
    try {
      transformer = TransformerFactory.newInstance().newTransformer();
    } catch (TransformerConfigurationException ex) {
      throw new RuntimeException("Failed to print document " + document, ex);
    }
    Properties props = new Properties();
    props.setProperty(OutputKeys.ENCODING, "UTF-8");
    props.setProperty(OutputKeys.METHOD, "xml");
    if (!compact) {
      props.setProperty(OutputKeys.INDENT, "yes");
      props.setProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    }
    transformer.setOutputProperties(props);
    try {
      transformer.transform(new DOMSource(document), new StreamResult(
              new OutputStreamWriter(out, "UTF-8")));
    } catch (TransformerException | UnsupportedEncodingException ex) {
      throw new RuntimeException("Failed to print document " + document, ex);
    }
  }

  public static void printDocument(Document document, OutputStream out) {
    printDocument(document, out, true);
  }

  public NamespacePrefixMapper getNamespacePrefixMapper() {
    return namespacePrefixMapper;
  }

  public void setNamespacePrefixMapper(
          NamespacePrefixMapper namespacePrefixMapper) {
    this.namespacePrefixMapper = namespacePrefixMapper;
  }
}
