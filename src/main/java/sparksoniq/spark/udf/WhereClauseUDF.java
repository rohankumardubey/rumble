/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Authors: Stefan Irimescu, Can Berker Cikis
 *
 */

package sparksoniq.spark.udf;

import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.types.StructType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import scala.collection.mutable.WrappedArray;
import sparksoniq.jsoniq.item.BooleanItem;
import sparksoniq.jsoniq.item.Item;
import sparksoniq.jsoniq.runtime.iterator.RuntimeIterator;
import sparksoniq.semantics.DynamicContext;
import sparksoniq.spark.DataFrameUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WhereClauseUDF implements UDF1<WrappedArray, Boolean> {
    private RuntimeIterator _expression;
    private StructType _inputSchema;

    private DynamicContext _context;
    
    private transient Kryo _kryo;
    private transient Input _input;

    public WhereClauseUDF(
            RuntimeIterator expression,
            StructType inputSchema) {
        _expression = expression;
        _inputSchema = inputSchema;

        _context = new DynamicContext();
        
        _kryo = new Kryo();
        DataFrameUtils.registerKryoClassesKryo(_kryo);
        _input = new Input();
    }


    @Override
    public Boolean call(WrappedArray wrappedParameters) {
        _context.removeAllVariables();

        String[] columnNames = _inputSchema.fieldNames();
        Set<String> dependencies = _expression.getVariableDependencies();

        Object[] serializedParams = (Object[]) wrappedParameters.array();
        
        // prepare dynamic context
        for (int columnIndex = 0; columnIndex < columnNames.length; columnIndex++) {
            String var = columnNames[columnIndex];
            if(dependencies.contains(var))
            {
                byte[] bytes = (byte[]) serializedParams[columnIndex];
                List<Item> deserializedParam = (List<Item>) DataFrameUtils.deserializeByteArray(bytes, _kryo, _input);
                _context.addVariableValue(var, deserializedParam);
            }
        }

        // apply expression in the dynamic context
        _expression.open(_context);
        Item nextItem = _expression.next();
        _expression.close();

        return ((BooleanItem) nextItem).getBooleanValue();
    }
    
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        _kryo = new Kryo();
        DataFrameUtils.registerKryoClassesKryo(_kryo);
        _input = new Input();
    }
}