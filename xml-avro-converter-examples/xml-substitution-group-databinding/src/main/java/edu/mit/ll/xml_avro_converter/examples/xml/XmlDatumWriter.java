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
