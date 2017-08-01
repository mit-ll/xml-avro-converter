- [A First Example](#a-first-example)
- [Conversion of XML Schemas and Data](#conversion-of-xml-schemas-and-data)
- [XML Substitution Groups](#xml-substitution-groups)
  - [Substitution Group Avro Schema](#substitution-group-avro-schema)
  - [Substitution Group Serialization to Avro](#substitution-group-serialization-to-avro)
  - [Substitution Group Deserialization from Avro](#substitution-group-deserialization-from-avro)
- [XML Nillable Elements](#xml-nillable-elements)
  - [Nillable Element Avro Schema](#nillable-element-avro-schema)
  - [Nillable Element Serialization to Avro](#nillable-element-serialization-to-avro)
  - [Nillable Element Deserialization from Avro](#nillable-element-deserialization-from-avro)

# A First Example

The first example demonstrates how `xml-avro-converter` can be used to generate an Avro schema from an existing Java class. This example demonstrates how `xml-avro-converter` can be used to automatically incorporate inherited types in the data model:

```java
package edu.mit.ll.xml_avro_converter.examples.polymorphism;

import edu.mit.ll.xml_avro_converter.AvroSchemaGenerator;
import edu.mit.ll.xml_avro_converter.AvroSerializer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.Schema;

public class PolymorphismExample {

  public static void main(String[] args) throws IOException {
    AvroSchemaGenerator schemaGenerator = new AvroSchemaGenerator();
    Schema departureQueueSchema = schemaGenerator.generateSchema(
            DepartureQueue.class);

    System.out.println("Schema without subtypes: "
            + departureQueueSchema.toString(true));

    schemaGenerator.declarePolymorphicType(
            CargoAircraft.class,
            PassengerAircraft.class);
    departureQueueSchema = schemaGenerator.generateSchema(
            DepartureQueue.class);
    System.out.println("Schema with subtypes added: "
            + departureQueueSchema.toString(true));

    DepartureQueue queue = getSampleData();
    AvroSerializer<DepartureQueue> serializer = new AvroSerializer(
            departureQueueSchema);
    ByteArrayOutputStream departureQueueAvroStream
            = new ByteArrayOutputStream();
    serializer.writeToAvro(departureQueueAvroStream, queue);
    System.out.println("Serialized sample data to "
            + departureQueueAvroStream.toByteArray().length + " bytes");
  }

  private static DepartureQueue getSampleData() {
    DepartureQueue departureQueue = new DepartureQueue();
    departureQueue.name = "sample departure queue";
    departureQueue.aircraft = new ArrayList<>();

    CargoAircraft sampleCargoAircraft = new CargoAircraft();
    sampleCargoAircraft.identifier = "sample cargo aircraft";
    sampleCargoAircraft.cargoCapacity = 1800.0;
    departureQueue.aircraft.add(sampleCargoAircraft);

    PassengerAircraft samplePassengerAircraft = new PassengerAircraft();
    samplePassengerAircraft.identifier = "sample passenger aircraft";
    samplePassengerAircraft.passengerCapacity = 150;
    departureQueue.aircraft.add(samplePassengerAircraft);

    return departureQueue;
  }

  private static class DepartureQueue {

    public String name;
    public List<Aircraft> aircraft;
  }

  private static abstract class Aircraft {

    public String identifier;
  }

  private static class CargoAircraft extends Aircraft {

    public double cargoCapacity;
  }

  private static class PassengerAircraft extends Aircraft {

    public int passengerCapacity;
  }
}
```

This example can be found in `xml-avro-converter-examples/polymorphism` and can be executed from that directory with the command: `mvn compile exec:java`. The program should output:

```json
Schema without subtypes: {
  "type" : "record",
  "name" : "DepartureQueue",
  "namespace" : "edu.mit.ll.xml_avro_converter.examples.polymorphism.PolymorphismExample$",
  "fields" : [ {
    "name" : "name",
    "type" : [ "null", "string" ]
  }, {
    "name" : "aircrafts",
    "type" : [ "null", {
      "type" : "array",
      "items" : [ "null", {
        "type" : "record",
        "name" : "Aircraft",
        "fields" : [ {
          "name" : "identifier",
          "type" : [ "null", "string" ]
        } ]
      } ]
    } ]
  } ]
}
Schema with subtypes added: {
  "type" : "record",
  "name" : "DepartureQueue",
  "namespace" : "edu.mit.ll.xml_avro_converter.examples.polymorphism.PolymorphismExample$",
  "fields" : [ {
    "name" : "name",
    "type" : [ "null", "string" ]
  }, {
    "name" : "aircrafts",
    "type" : [ "null", {
      "type" : "array",
      "items" : [ "null", {
        "type" : "record",
        "name" : "Aircraft",
        "fields" : [ {
          "name" : "identifier",
          "type" : [ "null", "string" ]
        } ]
      }, {
        "type" : "record",
        "name" : "CargoAircraft",
        "fields" : [ {
          "name" : "cargoCapacity",
          "type" : [ "double", "null" ]
        }, {
          "name" : "identifier",
          "type" : [ "null", "string" ]
        } ]
      }, {
        "type" : "record",
        "name" : "PassengerAircraft",
        "fields" : [ {
          "name" : "passengerCapacity",
          "type" : [ "int", "null" ]
        }, {
          "name" : "identifier",
          "type" : [ "null", "string" ]
        } ]
      } ]
    } ]
  } ]
}
Serialized sample data to 91 bytes
```

Note the importance of the call to `AvroSchemaGenerator.declarePolymorphicType()`. Without it, the generated schema for `DepartureQueue.aircraft` is an array of only the abstract `Aircraft` type. The Avro serializer would not be able to substitute an instance of the `CargoAircraft` or `PassengerAircraft` types into this list because the schema is not aware of them. After these types have been declared to the `AvroSchemaGenerator` instance, the call to `generateSchema()` automatically generates a new schema where the sub-schema for the `DepartureQueue.aircraft` type has been replaced with a union of `Aircraft` along with all of the subtypes which were declared.

Next, an instance of `AvroSerializer` is used to serialize an object to an Avro bytestream. `AvroSerializer` wraps the `ReflectDatumReader` and `ReflectDatumWriter` classes provided by the Avro library. A serializer is created for the `DepartureQueue` class using the newly generated schema and then the `writeToAvro()` method is called with an output stream and the sample object passed as arguments. The `AvroSerializer` class is provided as a convenience for reading and writing objects with a single function call, but it is not required to use this abstraction - the user may write the Java object to an Avro data stream by directly using the Avro API if he/she so chooses. Note that the bytestream only contains the serialized binary data, and it does *not* include the Avro schema.

# Conversion of XML Schemas and Data

This section demonstrates how to convert an XML schema which is sufficiently simple that no translation logic is needed.

To demonstrate, consider the example in `xml-avro-converter-examples/xml-conversion`, which can again be executed from that directory with the command `mvn compile exec:java`. The project attempts to convert data which must conform to the following XSD:

```xsd
<?xml version="1.0" encoding="UTF-8" ?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns="urn:edu:mit:ll:xml_avro_converter:examples:cities"
    elementFormDefault="qualified"
    targetNamespace="urn:edu:mit:ll:xml_avro_converter:examples:cities">
  <xs:simpleType name="nameType">
    <xs:restriction base="xs:string"/>
  </xs:simpleType>
  <xs:simpleType name="countryType">
    <xs:restriction base="xs:string"/>
  </xs:simpleType>
  <xs:simpleType name="directionType">
    <xs:restriction base="xs:string">
      <xs:enumeration value="E"/>
      <xs:enumeration value="N"/>
      <xs:enumeration value="S"/>
      <xs:enumeration value="W"/>
    </xs:restriction>
  </xs:simpleType>
  <xs:complexType name="positionType">
    <xs:sequence>
      <xs:element name="degrees" type="xs:int"/>
      <xs:element name="minutes" type="xs:int"/>
      <xs:element name="direction" type="directionType"/>
    </xs:sequence>
  </xs:complexType>
  <xs:complexType name="city">
    <xs:sequence>
      <xs:element name="name" type="nameType"/>
      <xs:element name="country" type="countryType"/>
      <xs:element name="latitude" type="positionType"/>
      <xs:element name="longitude" type="positionType"/>
      <xs:element name="population" type="xs:long" minOccurs="0"/>
    </xs:sequence>
  </xs:complexType>
  <xs:element name="cityCollection">
    <xs:complexType>
      <xs:sequence>
        <xs:element name="city" type="city" minOccurs="0"
            maxOccurs="unbounded"/>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>
```

The following is a sample document which validates against the XSD:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<cityCollection xmlns="urn:edu:mit:ll:xml_avro_converter:examples:cities">
  <city>
    <name>Alert</name>
    <country>Canada</country>
    <latitude>
      <degrees>82</degrees>
      <minutes>30</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>62</degrees>
      <minutes>20</minutes>
      <direction>W</direction>
    </longitude>
  </city>
  <city>
    <name>Nord</name>
    <country>Denmark</country>
    <latitude>
      <degrees>81</degrees>
      <minutes>36</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>16</degrees>
      <minutes>40</minutes>
      <direction>W</direction>
    </longitude>
  </city>
  <city>
    <name>Eureka</name>
    <country>Canada</country>
    <latitude>
      <degrees>79</degrees>
      <minutes>59</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>85</degrees>
      <minutes>56</minutes>
      <direction>W</direction>
    </longitude>
    <population>8</population>
  </city>
</cityCollection>
```

The Maven project generates the Java classes from the XSD prior to compiling source code for the sample program. The sample program in this example project then reads the sample document from the classpath and converts it to Avro. Then it converts the Avro data back into an XML document and prints it for comparison:

```java
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
```

The program begins by instantiating an `XmlSerializer` to facilitiate XML serialization and deserialization. A `JAXBContext` is created using the `ObjectFactory` that was generated by JAXB, and the XSD is read into the program so the marshaller can validate input and output documents. Then an instance of `XmlSerializer` is instantiated (using the `JAXBContext` and the XML schema), and the data is read into Java by calling `readFromXml()` on the `XmlSerializer`. Like the `AvroSerializer` class, it is not required to use it, and JAXB API can be used directly.

Next, the Avro schema is created for the `CityCollection` type and used to convert the Java object to an Avro-formatted byte stream. As in the previous example, an `AvroSerializer` is created by the `AvroSchemaGenerator` instance and the serializer writes the Avro data to a `byte[]`. `readFromAvro()` is then called on the same `AvroSerializer` instance to convert the Avro bytestream back into a new Java object which is then converted into an XML `Document` using the `writeToXml()` method of the `XmlSerializer` object. Finally, the `Document` generated from the Avro data is printed to `System.out` for the user to read (demonstrating once again that the XML documents are identical) and the sizes of the XML and Avro documents are printed for comparison. The program should output:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<cityCollection xmlns="urn:edu:mit:ll:xml_avro_converter:examples:cities">
  <city>
    <name>Alert</name>
    <country>Canada</country>
    <latitude>
      <degrees>82</degrees>
      <minutes>30</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>62</degrees>
      <minutes>20</minutes>
      <direction>W</direction>
    </longitude>
  </city>
  <city>
    <name>Nord</name>
    <country>Denmark</country>
    <latitude>
      <degrees>81</degrees>
      <minutes>36</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>16</degrees>
      <minutes>40</minutes>
      <direction>W</direction>
    </longitude>
  </city>
  <city>
    <name>Eureka</name>
    <country>Canada</country>
    <latitude>
      <degrees>79</degrees>
      <minutes>59</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>85</degrees>
      <minutes>56</minutes>
      <direction>W</direction>
    </longitude>
    <population>8</population>
  </city>
</cityCollection>
XML document size: 868 bytes
Avro document size: 102 bytes
```

The file size of the document is significantly reduced by conversion to Avro format, but remains identical when converted back to XML.

# XML Substitution Groups

In certain cases, substitution groups in the XML schema present a complexity which require manual intervention in the data conversion process.

Continuing from the previous example, suppose that now that it is desirable for the XML schema to accept the country element either in its full name or as an ISO-standard country code:

```patch
diff --git a/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/resources/cities.xsd b/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/resources/cities.xsd
index 36c6142..2610cff 100644
--- a/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/resources/cities.xsd
+++ b/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/resources/cities.xsd
@@ -9,6 +9,14 @@
   <xs:simpleType name="countryType">
     <xs:restriction base="xs:string"/>
   </xs:simpleType>
+  <xs:simpleType name="countryIsoCodeType">
+    <xs:restriction base="countryType">
+      <xs:pattern value="[A-Z]{2}"/>
+    </xs:restriction>
+  </xs:simpleType>
+  <xs:element name="country" type="countryType"/>
+  <xs:element name="countryIsoCode" type="countryIsoCodeType"
+      substitutionGroup="country"/>
   <xs:simpleType name="directionType">
     <xs:restriction base="xs:string">
       <xs:enumeration value="E"/>
@@ -27,7 +35,7 @@
   <xs:complexType name="city">
     <xs:sequence>
       <xs:element name="name" type="nameType"/>
-      <xs:element name="country" type="countryType"/>
+      <xs:element ref="country"/>
       <xs:element name="latitude" type="positionType"/>
       <xs:element name="longitude" type="positionType"/>
       <xs:element name="population" type="xs:long" minOccurs="0"/>
```

This means that `country` could instead be provided as such:

```patch
diff --git a/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/resources/cities.xml b/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/resources/cities.xml
index 4d4006a..2cd695d 100644
--- a/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/resources/cities.xml
+++ b/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/resources/cities.xml
@@ -16,7 +16,7 @@
   </city>
   <city>
     <name>Nord</name>
-    <country>Denmark</country>
+    <countryIsoCode>DK</countryIsoCode>
     <latitude>
       <degrees>81</degrees>
       <minutes>36</minutes>
```

When JAXB generates the code for the `City` class, the `country` element is no longer a simple string:

```java
public class City {

    @XmlElement(required = true)
    protected String name;
    @XmlElementRef(name = "country", namespace = "urn:edu:mit:ll:xml_avro_converter:examples:cities", type = JAXBElement.class)
    protected JAXBElement<String> country;
    @XmlElement(required = true)
    protected PositionType latitude;
    @XmlElement(required = true)
    protected PositionType longitude;
    protected Long population;
```

The JAXB code generator has provided a string type wrapped in a generic `JAXBElement` type because Java's object model [cannot completely describe] the `country` field's data constraints, due to the new substitution group.

[cannot completely describe]: http://stackoverflow.com/questions/3658378

## Substitution Group Avro Schema

Attempting to create an Avro schema for this class will now result in the following error message because Avro cannot determine how to handle this generic type:

    Exception in thread "main" org.apache.avro.AvroTypeException: Unknown type: T

In order to resolve this, is it necessary to provide custom databindings for this portion of the Avro schema, and also to override Avro's default logic for serializing and deserializing these data elements.

First, a custom subclass of Avro's `ReflectData` class must be passed to the AvroSchemaGenerator instance. `schemaGenerator` will use this class when generating the Avro schema. The custom `ReflectData` class will also be used to provide custom `DatumReader` and `DatumWriter` objects for Avro to serialize and deserialize Java objects.

```patch
diff --git a/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlConversionExample.java b/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlConversionExample.java
index 3877c01..8c690fc 100644
--- a/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlConversionExample.java
+++ b/xml-avro-converter-examples/xml-substitution-group-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlConversionExample.java
@@ -27,7 +27,8 @@ public class XmlConversionExample {
             "/cities.xml");
     CityCollection cities = xmlSerializer.readFromXml(citiesStream);
 
-    AvroSchemaGenerator schemaGenerator = new AvroSchemaGenerator();
+    AvroSchemaGenerator schemaGenerator = new AvroSchemaGenerator(
+            new XmlReflectData());
     Schema citiesSchema = schemaGenerator.generateSchema(CityCollection.class);
     AvroSerializer<CityCollection> avroSerializer =
             schemaGenerator.createAvroSerializer(citiesSchema);
```

The custom `ReflectData` instance adds the following additional functionality:

```java
package edu.mit.ll.xml_avro_converter.examples.xml;

import com.sun.xml.bind.api.impl.NameConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlElementRef;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.reflect.ReflectData;

public class XmlReflectData extends ReflectData {

  private Schema createCountrySchema(XmlElementRef annotationData) {
    List<Field> fields = new ArrayList<>();
    fields.add(new Field("typeName", Schema.create(Schema.Type.STRING),
            null, null));
    fields.add(new Field("typeData", Schema.create(Schema.Type.STRING),
            null, null));
    String namespace = NameConverter.standard.toPackageName(
            annotationData.namespace());
    String name = NameConverter.standard.toClassName(annotationData.name());
    return Schema.createRecord(name, null, namespace, false, fields);
  }

  @Override
  protected Schema createFieldSchema(java.lang.reflect.Field field,
          Map<String, Schema> names) {
    XmlElementRef annotationData = field.getAnnotation(XmlElementRef.class);
    if (annotationData == null) {
      return super.createFieldSchema(field, names);
    }

    if (annotationData.name().equals("country")) {
      return createCountrySchema(annotationData);
    }

    throw new UnsupportedOperationException("Custom schema for "
            + annotationData + " not supported");
  }

  @Override
  public DatumWriter createDatumWriter(Schema schema) {
    return new XmlDatumWriter(schema, this);
  }

  @Override
  public DatumReader createDatumReader(Schema writer, Schema reader) {
    return new XmlDatumReader(writer, reader);
  }

  @Override
  public DatumReader createDatumReader(Schema schema) {
    return new XmlDatumReader(schema);
  }
}
```

`XmlReflectData` overrides `createFieldSchema()` in the parent class, which is the method which creates fields of record-type Avro schemas. `XmlReflectData.createFieldSchema()` inspects the annotation data on the field that's passed into it. When it matches the expected annotation data for the `country` field, it calls `createCountrySchema()` which returns a special record for the `country` field. The special schema is a record-type Avro schema with two fields: the name of the type (`typeName`, which in this case could be either `country` or `countryIsoCode`), and the content of the field itself (`typeData`). Since both `country` and `countryIsoCode` are defined as strings in the XSD, this primitive type is the appropriate schema for `typeData`. However, for substitution groups with more complex types, a record schema generated for the substitution group's base type would be the appropriate schema for `typeData`. This can be achieved by calling `getSchema()` using the base type of the substitution group as the first argument.

The Avro schema for the `country` field now looks like:

```json
[ "null", {
  "type" : "record",
  "name" : "Country",
  "namespace" : "edu.mit.ll.xml_avro_converter.examples.cities",
  "fields" : [ {
    "name" : "typeName",
    "type" : [ "null", "string" ]
  }, {
    "name" : "typeData",
    "type" : [ "null", "string" ]
  } ]
} ]
```

## Substitution Group Serialization to Avro

Now that the schema has been changed, the Avro library cannot natively serialize a Java object to Avro format. When Avro reaches the `country` field of a `City` object, the schema will indicate that the data type in that field is an object with two string types (named `typeName` and `typeData`). But instead, it will really encounter an object of type `JAXBElement<String>`. At this point in the serialization, Avro will return the following error:

    Caused by: org.apache.avro.AvroTypeException: Unknown type: T

Why does it produce this message? When it encounters a type that it does not expect in the schema, it attempts to create the schema for the new type, which, once again, it cannot do because Avro cannot natively serialize generic Java types (unless they are a list or a map).

At the bottom of the definition of `XmlReflectData`, the `createDatumWriter()` method is overridden to return an instance of `XmlDatumWriter`, a subclass of Avro's `ReflectDatumWriter` class. `XmlDatumWriter` implements the custom code necessary to serialize the `country` field -- this code is provided below:

```java
package edu.mit.ll.xml_avro_converter.examples.xml;

import com.sun.xml.bind.api.impl.NameConverter;
import edu.mit.ll.xml_avro_converter.examples.cities.City;
import java.io.IOException;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.apache.avro.Schema;
import org.apache.avro.io.Encoder;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumWriter;

public class XmlDatumWriter<T> extends ReflectDatumWriter<T> {

  public XmlDatumWriter(Schema root, ReflectData reflectData) {
    super(root, reflectData);
  }

  private void writeCountry(Object datum, Schema.Field f, Encoder out)
          throws IOException {
    Schema schema = f.schema();
    JAXBElement<String> country = ((City) datum).getCountry();
    QName name = country.getName();
    String packageName = NameConverter.standard.toPackageName(
            name.getNamespaceURI());
    String typeName = packageName + "."
            + NameConverter.standard.toClassName(name.getLocalPart());
    String recordName = packageName + ".Country";
    int unionIndex = schema.getIndexNamed(recordName);
    out.writeIndex(unionIndex);

    Schema countrySchema = schema.getTypes().get(unionIndex);
    Schema nameSchema = countrySchema.getField("typeName").schema();
    write(nameSchema, typeName, out);
    Schema dataSchema = countrySchema.getField("typeData").schema();
    write(dataSchema, country.getValue(), out);
  }

  @Override
  protected void writeField(Object datum, Schema.Field f, Encoder out,
          Object state)
          throws IOException {
    if (!(datum instanceof City)) {
      super.writeField(datum, f, out, state);
      return;
    }
    if (f.name().equals("country")) {
      writeCountry(datum, f, out);
      return;
    }
    super.writeField(datum, f, out, state);
  }
}
```

`writeField()` scans for the `country` field and calls `writeCountry()` when it is observed to handle the actual serialization. `writeCountry()` writes to the encoder three times. The Avro schema specifies via a union that `country` can either be a null or a record, but since the XML schema forbids the `country` field from being null, the code simply assumes that it is never null and always of the `Country` type. The name of the `Country` type is looked up in the union and the value is written using the `writeIndex()` method of the `Encoder object.

The other two calls to `write()` (defined in `XmlDatumWriter`'s superclass) are for the `typeName` and `typeData` properties. The fields must be written in the order they are specified when the schema is created. When the `country` field is provided as the full country name, `typeName` is set to `edu.mit.ll.xml_avro_converter.examples.cities.Country`, but when it is specified as an ISO code, it is set to `edu.mit.ll.xml_avro_converter.examples.cities.CountryIsoCode`. The `XmlDatumReader` will use this distinction to determine which field type to return during deserialization. The last value written is the actual value of the `country` field.

## Substitution Group Deserialization from Avro

Once again, it is necessary to instruct Avro how to read the binary data for the custom type back into Java. The default `ReflectDatumReader` will not be able to find a Java type which corresponds to the structure provided in the schema, so it will instead simply deserialize the stream into an instance of `Record` (a generic Avro type for encapsulating data), which implements the `GenericRecord` type. Then it sets the `country` field of the Java object to this `Record` instance which JAXB cannot serialize and so produces the following error message:

    Caused by: com.sun.istack.SAXException2: class org.apache.avro.generic.GenericData$Record nor any of its super class is known to this context.
    javax.xml.bind.JAXBException: class org.apache.avro.generic.GenericData$Record nor any of its super class is known to this context.

`XmlReflectData` also provides a custom instance of `DatumReader` which is implemented to deserialize the byte stream appropriately:

```java
package edu.mit.ll.xml_avro_converter.examples.xml;

import edu.mit.ll.xml_avro_converter.SchemaGenerationException;
import edu.mit.ll.xml_avro_converter.examples.cities.ObjectFactory;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.io.ResolvingDecoder;
import org.apache.avro.reflect.ReflectDatumReader;

public class XmlDatumReader<T> extends ReflectDatumReader<T> {

  public XmlDatumReader(Schema writer, Schema reader) {
    super(writer, reader);
  }

  public XmlDatumReader(Schema schema) {
    super(schema);
  }

  private Object readCountry(Object old, ResolvingDecoder in)
          throws IOException {
    String typeName = null, typeData = null;
    for (Schema.Field field : in.readFieldOrder()) {
      String fieldName = field.name();
      switch (fieldName) {
        case "typeName":
          typeName = (String) readWithoutConversion(old, field.schema(), in);
          break;
        case "typeData":
          typeData = (String) readWithoutConversion(old, field.schema(), in);
          break;
        default:
          throw new SchemaGenerationException("Unexpected field: "
                  + fieldName);
      }
    }
    if (typeName == null || typeData == null) {
      throw new SchemaGenerationException(
              "typeName and/or typeData was not defined");
    }
    ObjectFactory objf = new ObjectFactory();
    if (typeName.endsWith("CountryIsoCode")) {
      return objf.createCountryIsoCode(typeData);
    }
    if (typeName.endsWith("Country")) {
      return objf.createCountry(typeData);
    }
    throw new SchemaGenerationException("Unhandled type: " + typeName);
  }

  @Override
  protected Object readRecord(Object old, Schema expected,
          ResolvingDecoder in) throws IOException {
    if (!expected.getNamespace().equals(
            "edu.mit.ll.xml_avro_converter.examples.cities")) {
      return super.readRecord(old, expected, in);
    }
    if (expected.getName().equals("Country")) {
      return readCountry(old, in);
    }
    return super.readRecord(old, expected, in);
  }
}
```

`XmlDatumReader` overrides the `readRecord()` method to search for the `country` field. Upon arriving at it, the `typeName` and `typeData` fields are read back into corresponding string variables. Based on the content of the `typeName` string, the method calls either `createCountry()` or `createCountryIsoCode()` with the `typeData` variable as the sole argument and returns the result.

Now it is possible to perform full serialization and deserialization. The full example can be run from `xml-avro-converter-examples/xml-substitution-group-databinding` with `mvn compile exec:java`. The output now shows as:

```
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<cityCollection xmlns="urn:edu:mit:ll:xml_avro_converter:examples:cities">
  <city>
    <name>Alert</name>
    <country>Canada</country>
    <latitude>
      <degrees>82</degrees>
      <minutes>30</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>62</degrees>
      <minutes>20</minutes>
      <direction>W</direction>
    </longitude>
  </city>
  <city>
    <name>Nord</name>
    <countryIsoCode>DK</countryIsoCode>
    <latitude>
      <degrees>81</degrees>
      <minutes>36</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>16</degrees>
      <minutes>40</minutes>
      <direction>W</direction>
    </longitude>
  </city>
  <city>
    <name>Eureka</name>
    <country>Canada</country>
    <latitude>
      <degrees>79</degrees>
      <minutes>59</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>85</degrees>
      <minutes>56</minutes>
      <direction>W</direction>
    </longitude>
    <population>8</population>
  </city>
</cityCollection>
XML document size: 877 bytes
Avro document size: 272 bytes
```

Once again, the document is preserved identically, and the Avro conversion has preserved the use of the `<countryIsoCode>` tag in the second `<city>` element.

# XML Nillable Elements

Under certain circumstances, setting an XML type to nillable can also result in JAXB generating an element of the generic `JAXBElement` type.

Amending the previous example, suppose that the population may now be nillable:

```patch
diff --git a/xml-avro-converter-examples/xml-nillable-databinding/src/main/resources/cities.xsd b/xml-avro-converter-examples/xml-nillable-databinding/src/main/resources/cities.xsd
index 2610cff..53e21de 100644
--- a/xml-avro-converter-examples/xml-nillable-databinding/src/main/resources/cities.xsd
+++ b/xml-avro-converter-examples/xml-nillable-databinding/src/main/resources/cities.xsd
@@ -38,7 +38,8 @@
       <xs:element ref="country"/>
       <xs:element name="latitude" type="positionType"/>
       <xs:element name="longitude" type="positionType"/>
-      <xs:element name="population" type="xs:long" minOccurs="0"/>
+      <xs:element name="population" type="xs:long" minOccurs="0"
+          nillable="true"/>
     </xs:sequence>
   </xs:complexType>
   <xs:element name="cityCollection">
```

This means that population could be provided in three possible types. It could be omitted entirely, provided with a positive integer value, or provided with the `nil` attribute set:

```patch
diff --git a/xml-avro-converter-examples/xml-nillable-databinding/src/main/resources/cities.xml b/xml-avro-converter-examples/xml-nillable-databinding/src/main/resources/cities.xml
index 2cd695d..9149a2d 100644
--- a/xml-avro-converter-examples/xml-nillable-databinding/src/main/resources/cities.xml
+++ b/xml-avro-converter-examples/xml-nillable-databinding/src/main/resources/cities.xml
@@ -13,6 +13,8 @@
       <minutes>20</minutes>
       <direction>W</direction>
     </longitude>
+    <population xsi:nil="true"
+        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
   </city>
   <city>
     <name>Nord</name>
```

(Semantically, a *nil* datum might signal that any pre-existing value is invalid, while an empty, or *null*, field means that it should not be changed.)

## Nillable Element Avro Schema

The following code is added to support further customization of the schema:

```patch
diff --git a/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlReflectData.java b/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlReflectData.java
index 429a170..156b138 100644
--- a/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlReflectData.java
+++ b/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlReflectData.java
@@ -25,6 +25,18 @@ public class XmlReflectData extends ReflectData {
     return Schema.createRecord(name, null, namespace, false, fields);
   }
 
+  private Schema createPopulationSchema(XmlElementRef annotationData) {
+    List<Field> fields = new ArrayList<>();
+    fields.add(new Field("nil", Schema.create(Schema.Type.BOOLEAN),
+            null, null));
+    fields.add(new Field("data", Schema.create(Schema.Type.LONG),
+            null, null));
+    String namespace = NameConverter.standard.toPackageName(
+            annotationData.namespace());
+    String name = NameConverter.standard.toClassName(annotationData.name());
+    return Schema.createRecord(name, null, namespace, false, fields);
+  }
+
   @Override
   protected Schema createFieldSchema(java.lang.reflect.Field field,
           Map<String, Schema> names) {
@@ -37,6 +49,10 @@ public class XmlReflectData extends ReflectData {
       return createCountrySchema(annotationData);
     }
 
+    if (annotationData.name().equals("population")) {
+      return createPopulationSchema(annotationData);
+    }
+
     throw new UnsupportedOperationException("Custom schema for "
             + annotationData + " not supported");
   }
```

The `population` field now has the following schema:

```json
[ "null", {
  "type" : "record",
  "name" : "Population",
  "namespace" : "edu.mit.ll.xml_avro_converter.examples.cities",
  "fields" : [ {
    "name" : "nil",
    "type" : [ "boolean", "null" ]
  }, {
    "name" : "data",
    "type" : [ "long", "null" ]
  } ]
} ]
```

Ideally, a nested union of type `[null, [null, int]]` would be preferable, but nested union types are not currently allowed by Avro, so a record schema is used intsead. Once again, there are two fields in the `Population` type: `nil` (a `boolean`) and `data` (a `long`, to store the actual value, if provided).

## Nillable Element Serialization to Avro

The following changes are added to support serializing to this custom schema:

```patch
diff --git a/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlDatumWriter.java b/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlDatumWriter.java
index fde0fae..ff91b46 100644
--- a/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlDatumWriter.java
+++ b/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlDatumWriter.java
@@ -36,6 +36,28 @@ public class XmlDatumWriter<T> extends ReflectDatumWriter<T> {
     write(dataSchema, country.getValue(), out);
   }
 
+  private void writePopulation(Object datum, Schema.Field f, Encoder out)
+          throws IOException {
+    City cityDatum = (City) datum;
+    JAXBElement<Long> population = cityDatum.getPopulation();
+    Schema schema = f.schema();
+    if (population == null) {
+      int unionIndex = schema.getIndexNamed("null");
+      out.writeIndex(unionIndex);
+      out.writeNull();
+      return;
+    }
+    String className = datum.getClass().getPackage().getName()
+            + ".Population";
+    int unionIndex = schema.getIndexNamed(className);
+    out.writeIndex(unionIndex);
+    Schema populationSchema = schema.getTypes().get(unionIndex);
+    Schema nilSchema = populationSchema.getField("nil").schema();
+    write(nilSchema, population.isNil(), out);
+    Schema dataSchema = populationSchema.getField("data").schema();
+    write(dataSchema, population.getValue(), out);
+  }
+
   @Override
   protected void writeField(Object datum, Schema.Field f, Encoder out,
           Object state)
@@ -48,6 +70,10 @@ public class XmlDatumWriter<T> extends ReflectDatumWriter<T> {
       writeCountry(datum, f, out);
       return;
     }
+    if (f.name().equals("population")) {
+      writePopulation(datum, f, out);
+      return;
+    }
     super.writeField(datum, f, out, state);
   }
 }
```

If `population` is null, the index for a null element is written to the union and a null element is written using the `writeNull()` method. If `population` is non-null, the index for the record is written into the union. Then the `population.isNil()` value is written to the `nil` field of the record, and the value stored within the `JAXBElement` is written to the `data` field.

## Nillable Element Deserialization from Avro

The below code demonstrates how to read the nillable element back from Avro into Java:

```patch
diff --git a/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlDatumReader.java b/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlDatumReader.java
index b63e253..fbeaeaf 100644
--- a/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlDatumReader.java
+++ b/xml-avro-converter-examples/xml-nillable-databinding/src/main/java/edu/mit/ll/xml_avro_converter/examples/xml/XmlDatumReader.java
@@ -49,6 +49,34 @@ public class XmlDatumReader<T> extends ReflectDatumReader<T> {
     throw new SchemaGenerationException("Unhandled type: " + typeName);
   }
 
+  private Object readPopulation(Object old, ResolvingDecoder in)
+          throws IOException {
+    Boolean nil = null;
+    Long data = null;
+    for (Schema.Field field : in.readFieldOrder()) {
+      String fieldName = field.name();
+      switch (fieldName) {
+        case "nil":
+          nil = (Boolean) readWithoutConversion(old, field.schema(), in);
+          break;
+        case "data":
+          data = (Long) readWithoutConversion(old, field.schema(), in);
+          break;
+        default:
+          throw new SchemaGenerationException("Unexpected field: "
+                  + fieldName);
+      }
+    }
+    if (nil == false && data == null) {
+      throw new SchemaGenerationException("Population.nil was false"
+              + " but data was null");
+    }
+    ObjectFactory objf = new ObjectFactory();
+    JAXBElement<Long> population = objf.createCityPopulation(data);
+    population.setNil(nil);
+    return population;
+  }
+
   @Override
   protected Object readRecord(Object old, Schema expected,
           ResolvingDecoder in) throws IOException {
@@ -59,6 +87,9 @@ public class XmlDatumReader<T> extends ReflectDatumReader<T> {
     if (expected.getName().equals("Country")) {
       return readCountry(old, in);
     }
+    if (expected.getName().equals("Population")) {
+      return readPopulation(old, in);
+    }
     return super.readRecord(old, expected, in);
   }
 }
```

Again, the fields are read in the order specified by the schema. `nil` is read back into a `boolean` field and `data` is read back into a `long`. Then a validation check is run to ensure that `data` is null if `nil` is true. Finally, a JAXB `ObjectFactory` is instantiated once again and used to create the `population` Java field and the value is returned. Note that it is not necessary here to handle the case where the `population` field is omitted. If Avro encounters a null value, then `readRecord()` would not be called on this portion of the schema, so that case is handled automatically.

Now, when the XML document is reproduced after conversion through Avro, the `nil` element is preserved in the `population` field of the first `<city>` element:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<cityCollection xmlns="urn:edu:mit:ll:xml_avro_converter:examples:cities">
  <city>
    <name>Alert</name>
    <country>Canada</country>
    <latitude>
      <degrees>82</degrees>
      <minutes>30</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>62</degrees>
      <minutes>20</minutes>
      <direction>W</direction>
    </longitude>
    <population xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
  </city>
  <city>
    <name>Nord</name>
    <countryIsoCode>DK</countryIsoCode>
    <latitude>
      <degrees>81</degrees>
      <minutes>36</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>16</degrees>
      <minutes>40</minutes>
      <direction>W</direction>
    </longitude>
  </city>
  <city>
    <name>Eureka</name>
    <country>Canada</country>
    <latitude>
      <degrees>79</degrees>
      <minutes>59</minutes>
      <direction>N</direction>
    </latitude>
    <longitude>
      <degrees>85</degrees>
      <minutes>56</minutes>
      <direction>W</direction>
    </longitude>
    <population>8</population>
  </city>
</cityCollection>
XML document size: 959 bytes
Avro document size: 278 bytes
```

