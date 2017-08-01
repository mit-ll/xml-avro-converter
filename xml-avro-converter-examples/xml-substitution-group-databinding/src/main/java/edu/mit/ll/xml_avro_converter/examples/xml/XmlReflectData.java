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
