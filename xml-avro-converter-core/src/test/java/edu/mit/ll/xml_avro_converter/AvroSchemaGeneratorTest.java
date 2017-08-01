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

import edu.mit.ll.xml_avro_converter.sample_types.SampleCompositeType;
import edu.mit.ll.xml_avro_converter.sample_types.SampleSubtype;
import java.util.ArrayList;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AvroSchemaGeneratorTest {

  public static Schema getSampleSchema() {
    ReflectData reflectData = ReflectData.get();
    return reflectData.getSchema(SampleCompositeType.class);
  }

  public static String getPackageName(Class classs) {
    return classs.getPackage().getName();
  }

  public static String getPackageName() {
    return getPackageName(AvroSchemaGeneratorTest.class);
  }

  public static Schema getSamplePolymorphicSchema() {
    AvroSchemaGenerator schemaGenerator = new AvroSchemaGenerator();
    schemaGenerator.declarePolymorphicType(SampleSubtype.class);
    return schemaGenerator.generateSchema(SampleCompositeType.class);
  }

  @Test
  public void testSamplePolymorphicSchema() {
    // Create a sample object and test its schema
    String packageName = getPackageName(SampleCompositeType.class);

    Schema nullSchema = Schema.create(Schema.Type.NULL);
    Schema intSchema = Schema.createUnion(
            Schema.create(Schema.Type.INT), nullSchema);
    Schema actualCompositeSchema = getSamplePolymorphicSchema();

    Schema expectedCompositeSchema = Schema.createRecord(
            "SampleCompositeType", null, packageName, false);
    Schema expectedBaseRecordSchema = Schema.createRecord(
            "SampleBaseType", null, packageName, false);
    Schema expectedSubtypeRecordSchema = Schema.createRecord(
            "SampleSubtype", null, packageName, false);
    Schema expectedBaseFullSchema = Schema.createUnion(
            nullSchema,
            expectedBaseRecordSchema,
            expectedSubtypeRecordSchema);

    List<Schema.Field> baseFields = new ArrayList<>();
    baseFields.add(new Schema.Field("baseVal", intSchema, null, null));
    expectedBaseRecordSchema.setFields(baseFields);

    List<Schema.Field> subtypeFields = new ArrayList<>();
    subtypeFields.add(new Schema.Field("subtypeVal", intSchema, null, null));
    subtypeFields.add(new Schema.Field("baseVal", intSchema, null, null));
    expectedSubtypeRecordSchema.setFields(subtypeFields);

    Schema expectedRecursiveRecordSchema = Schema.createRecord(
            "SampleRecursiveType", null, packageName, false);
    Schema expectedRecursiveFullSchema = Schema.createUnion(
            nullSchema,
            expectedRecursiveRecordSchema);

    List<Schema.Field> recursiveFields = new ArrayList<>();
    recursiveFields.add(new Schema.Field("subtypeVal", intSchema, null, null));
    recursiveFields.add(new Schema.Field("recursiveSubtypeRef1",
            expectedRecursiveFullSchema, null, null));
    recursiveFields.add(new Schema.Field("recursiveSubtypeRef2",
            expectedRecursiveFullSchema, null, null));
    expectedRecursiveRecordSchema.setFields(recursiveFields);

    List<Schema.Field> compositeFields = new ArrayList<>();
    compositeFields.add(new Schema.Field("baseType1", expectedBaseFullSchema,
            null, null));
    compositeFields.add(new Schema.Field("baseType2", expectedBaseFullSchema,
            null, null));
    compositeFields.add(new Schema.Field("sampleBaseTypeArray1",
            Schema.createUnion(
                    nullSchema,
                    Schema.createArray(expectedBaseFullSchema)), null, null));
    compositeFields.add(new Schema.Field("sampleBaseTypeArray2",
            Schema.createUnion(
                    nullSchema,
                    Schema.createArray(expectedBaseFullSchema)), null, null));
    compositeFields.add(new Schema.Field("sampleBaseTypeMap1",
            Schema.createUnion(
                    nullSchema,
                    Schema.createMap(expectedBaseFullSchema)
            ), null, null));
    compositeFields.add(new Schema.Field("sampleBaseTypeMap2",
            Schema.createUnion(
                    nullSchema,
                    Schema.createMap(expectedBaseFullSchema)
            ), null, null));
    compositeFields.add(new Schema.Field("sampleRecursiveType1",
            expectedRecursiveFullSchema, null, null));
    compositeFields.add(new Schema.Field("sampleRecursiveType2",
            expectedRecursiveFullSchema, null, null));

    expectedCompositeSchema.setFields(compositeFields);
    assertTrue(actualCompositeSchema.equals(expectedCompositeSchema));
  }
}
