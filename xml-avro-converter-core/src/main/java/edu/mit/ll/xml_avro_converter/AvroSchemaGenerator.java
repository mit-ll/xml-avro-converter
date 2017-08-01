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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.specific.SpecificData;

public final class AvroSchemaGenerator {

  private final ReflectData reflectData;
  private final Map<String, Set<Schema>> polymorphicTypeSchemas;
  private final RecordCache recordCache;
  private final Map<String, Schema> customSchemas;

  public AvroSchemaGenerator() {
    this(new ReflectData());
  }

  public AvroSchemaGenerator(ReflectData reflectData) {
    this.reflectData = reflectData;
    polymorphicTypeSchemas = new HashMap<>();
    recordCache = new RecordCache();
    customSchemas = new HashMap<>();
  }

  public void declarePolymorphicType(Type... types) {
    // Declare a polymorphic type
    recordCache.clear();
    for (Type type : types) {
      Class superType = ((Class) type).getSuperclass();
      while (!superType.equals(java.lang.Object.class)) {
        String typeName = superType.getName();
        Set<Schema> subtypes = polymorphicTypeSchemas.get(typeName);
        if (subtypes == null) {
          subtypes = new HashSet<>();
          polymorphicTypeSchemas.put(typeName, subtypes);
        }
        Schema subtypeSchema = reflectData.getSchema(type);
        subtypes.add(subtypeSchema);
        superType = superType.getSuperclass();
      }
    }
  }

  private Schema computeNewRecordSchema(Schema oldRecordSchema) {

    List<Field> newFields = new ArrayList<>();
    // Create a new schema for each of the fields
    for (Field oldField : oldRecordSchema.getFields()) {
      Schema newFieldSchema;
      Schema oldFieldSchema = oldField.schema();
      switch (oldFieldSchema.getType()) {
        case ARRAY:
          newFieldSchema = getPolymorphicTypes(Schema.createArray(
                  getPolymorphicTypes(oldFieldSchema.getElementType())));
          break;
        case MAP:
          newFieldSchema = getPolymorphicTypes(Schema.createMap(
                  getPolymorphicTypes(oldFieldSchema.getValueType())));
          break;
        default:
          newFieldSchema = getPolymorphicTypes(oldFieldSchema);
          break;
      }
      newFields.add(new Field(oldField.name(), newFieldSchema, oldField.doc(),
              oldField.defaultVal(), oldField.order()));
    }

    return recordCache.set(oldRecordSchema, newFields);
  }

  public Schema polymorphizeSchema(Schema rootSchema) {
    for (Schema s : new AvroSchemaIterator(rootSchema)) {
      if (s.getType() != Schema.Type.RECORD) {
        continue;
      }

      if (recordCache.isFieldsSet(s)) {
        continue;
      }

      // Compute a new record schema for s
      computeNewRecordSchema(s);
    }

    String emptySchemaName;
    while ((emptySchemaName = recordCache.getSchemaWithoutFields()) != null) {
      // Create remaining schemas
      Schema customSchema = customSchemas.get(emptySchemaName);
      if (customSchema != null) {
        computeNewRecordSchema(customSchema);
        continue;
      }
      Type polymorphicType;
      try {
        polymorphicType = Class.forName(emptySchemaName);
      } catch (ClassNotFoundException ex) {
        throw new SchemaGenerationException("Unable to find class for "
                + emptySchemaName, ex);
      }
      computeNewRecordSchema(reflectData.getSchema(polymorphicType));
    }

    switch (rootSchema.getType()) {
      case RECORD:
        return recordCache.get(rootSchema);
      case ARRAY:
      case MAP:
      case UNION:
        throw new UnsupportedOperationException("Schemas with ARRAY, MAP, or"
                + " UNION-type root schemas are not supported yet");
      default:
        return rootSchema;
    }
  }

  public Schema getPolymorphicTypes(Schema schema) {
    // Get the polymorphic types for schema
    switch (schema.getType()) {
      case MAP:
      case ARRAY:
        List<Schema> newTypes = new ArrayList<>();
        newTypes.add(Schema.create(Schema.Type.NULL));
        newTypes.add(schema);
        schema = Schema.createUnion(newTypes);
        return schema;
    }

    if (schema.getType() != Schema.Type.UNION) {
      List<Schema> newTypes = new ArrayList<>();
      newTypes.add(Schema.create(Schema.Type.NULL));
      newTypes.add(schema);
      schema = Schema.createUnion(newTypes);
    }

    Deque<Schema> polyTypes = new LinkedList<>(schema.getTypes());
    List<Schema> finalSchemaTypes = new ArrayList<>();
    while (!polyTypes.isEmpty()) {
      Schema unionType = polyTypes.removeFirst();
      if (unionType.getType() == Schema.Type.RECORD) {
        unionType = recordCache.getOrCreate(unionType);
      }
      finalSchemaTypes.add(unionType);

      Collection<Schema> subTypes = null;
      if (isNamedType(unionType)) {
        subTypes = polymorphicTypeSchemas.get(
                SpecificData.getClassName(unionType));
      }
      if (subTypes == null) {
        continue;
      }
      polyTypes.addAll(subTypes);
    }

    return Schema.createUnion(orderUnionSchemas(finalSchemaTypes));
  }

  private List<Schema> orderUnionSchemas(List<Schema> unionSchemas) {
    // Arrange the schemas in order
    List<Schema> orderedSchemas = new LinkedList<>();
    Map<String, Schema> namedSchemas = new TreeMap<>();
    boolean booleanAdded = false, bytesAdded = false, doubleAdded = false,
            floatAdded = false, intAdded = false, longAdded = false,
            nullAdded = false, stringAdded = false;
    for (Schema unionSchema : unionSchemas) {
      switch (unionSchema.getType()) {
        case BOOLEAN:
          booleanAdded = true;
          break;
        case BYTES:
          bytesAdded = true;
          break;
        case DOUBLE:
          doubleAdded = true;
          break;
        case FLOAT:
          floatAdded = true;
          break;
        case INT:
          intAdded = true;
          break;
        case LONG:
          longAdded = true;
          break;
        case NULL:
          nullAdded = true;
          break;
        case STRING:
          stringAdded = true;
          break;
        case ENUM:
        case FIXED:
        case RECORD:
          namedSchemas.put(unionSchema.getFullName(), unionSchema);
          break;
        default:
          throw new SchemaGenerationException("Unsupported operation: Schema"
                  + " of type " + unionSchema.getType() + " in union");
      }
    }
    if (booleanAdded) {
      orderedSchemas.add(Schema.create(Schema.Type.BOOLEAN));
    }
    if (bytesAdded) {
      orderedSchemas.add(Schema.create(Schema.Type.BYTES));
    }
    if (doubleAdded) {
      orderedSchemas.add(Schema.create(Schema.Type.DOUBLE));
    }
    if (floatAdded) {
      orderedSchemas.add(Schema.create(Schema.Type.FLOAT));
    }
    if (intAdded) {
      orderedSchemas.add(Schema.create(Schema.Type.INT));
    }
    if (longAdded) {
      orderedSchemas.add(Schema.create(Schema.Type.LONG));
    }
    if (nullAdded) {
      orderedSchemas.add(Schema.create(Schema.Type.NULL));
    }
    if (stringAdded) {
      orderedSchemas.add(Schema.create(Schema.Type.STRING));
    }
    for (Schema schema : namedSchemas.values()) {
      orderedSchemas.add(schema);
    }
    return orderedSchemas;
  }

  public static String unionTypesToString(Schema schema) {
    return unionTypesToString(schema.getTypes());
  }

  public static String unionTypesToString(Collection<Schema> types) {
    List<String> typeNames = new ArrayList();
    for (Schema s : types) {
      typeNames.add(s.getName());
    }
    return String.join(", ", typeNames);
  }

  // Create a schema for type
  public Schema generateSchema(Type type) {
    return polymorphizeSchema(reflectData.getSchema(type));
  }

  // Return whether unionType is a named type
  private boolean isNamedType(Schema unionType) {
    try {
      unionType.getNamespace();
    } catch (AvroRuntimeException e) {
      return false;
    }
    return true;
  }

  // Declares that schemas has a custom name not on the classpath
  public void declareCustomNamedSchema(Schema schema) {
    String typeName = schema.getFullName();
    if (!(isNamedType(schema))) {
      throw new SchemaGenerationException("Schema provided for " + typeName
              + " is not a NamedSchema");
    }
    Schema customType = customSchemas.get(typeName);
    if (customType == null) {
      customSchemas.put(typeName, schema);
      return;
    }
    if (!customType.equals(schema)) {
      throw new SchemaGenerationException(
              "Attempted to redefine schema for existing custom type "
                      + typeName);
    }
  }

  // A cache of record schemas
  private static class RecordCache {

    private final Map<String, Schema> cachedRecords;
    private final Set<String> emptyCachedRecords;

    public RecordCache() {
      cachedRecords = new HashMap<>();
      emptyCachedRecords = new HashSet<>();
    }

    public void clear() {
      cachedRecords.clear();
      emptyCachedRecords.clear();
    }

    // Create a schema for template
    public Schema create(Schema template) {
      if (template.getType() != Schema.Type.RECORD) {
        String errMsg = String.format("Attempted to cache non-Record schema "
                + " of type %s: %s",
                template.getType(), template.getFullName());
        throw new SchemaGenerationException(errMsg);
      }

      String fullName = template.getFullName();
      if (cachedRecords.containsKey(fullName)) {
        String errMsg = String.format("Attempted to cache schema for %s but"
                + " it already exists", fullName);
        throw new SchemaGenerationException(errMsg);
      }

      Schema newSchema = Schema.createRecord(template.getName(),
              template.getDoc(), template.getNamespace(), template.isError());
      cachedRecords.put(fullName, newSchema);
      emptyCachedRecords.add(SpecificData.getClassName(template));
      return newSchema;
    }

    public Schema get(Schema s) {
      return get(s.getFullName());
    }

    public Schema get(String fullName) {
      return cachedRecords.get(fullName);
    }

    // Get a schema for s or create it
    public Schema getOrCreate(Schema s) {
      Schema newSchema = get(s);
      if (newSchema == null) {
        newSchema = create(s);
      }
      return newSchema;
    }

    // Set the fields for the schema
    public Schema set(Schema schema, List<Field> fields) {
      String fullName = SpecificData.getClassName(schema);

      Schema newSchema = getOrCreate(schema);

      if (!emptyCachedRecords.contains(fullName)) {
        String errMsg = "Fields have already been set for " + fullName;
        throw new SchemaGenerationException(errMsg);
      }

      newSchema.setFields(fields);
      emptyCachedRecords.remove(fullName);
      return newSchema;
    }

    // Return whether fields are set for s
    public boolean isFieldsSet(Schema s) {
      return isComplete(s.getFullName());
    }

    public boolean isComplete(String fullName) {
      return cachedRecords.containsKey(fullName)
              && (!emptyCachedRecords.contains(fullName));
    }

    public String getSchemaWithoutFields() {
      for (String s : emptyCachedRecords) {
        return s;
      }
      return null;
    }
  }

  public AvroSerializer createAvroSerializer(Schema schema) {
    return new AvroSerializer(schema, reflectData);
  }
}
