/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package org.rumbledb.runtime.functions.input;

import org.apache.spark.api.java.JavaRDD;
import org.rumbledb.api.Item;
import org.rumbledb.context.DynamicContext;
import org.rumbledb.exceptions.CannotRetrieveResourceException;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.expressions.ExecutionMode;
import org.rumbledb.items.parsing.JSONSyntaxToItemMapper;
import org.rumbledb.runtime.RDDRuntimeIterator;
import org.rumbledb.runtime.RuntimeIterator;

import sparksoniq.spark.SparkSessionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class JsonFileFunctionIterator extends RDDRuntimeIterator {

    private static final long serialVersionUID = 1L;

    public JsonFileFunctionIterator(
            List<RuntimeIterator> arguments,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(arguments, executionMode, iteratorMetadata);
    }

    @Override
    public JavaRDD<Item> getRDDAux(DynamicContext context) {
        String url = this.children.get(0).materializeFirstItemOrNull(context).getStringValue();
        url = url.replaceAll(" ", "%20");
        URI uri = FileSystemUtil.resolveURI(this.staticURI, url, getMetadata());

        int partitions = -1;
        if (this.children.size() > 1) {
            partitions = this.children.get(1).materializeFirstItemOrNull(context).getIntValue();
        }

        JavaRDD<String> strings;
        if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {
            InputStream is = FileSystemUtil.getDataInputStream(
                uri,
                context.getRumbleRuntimeConfiguration(),
                getMetadata()
            );
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            List<String> lines = new ArrayList<>();
            String line = null;
            try {
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            } catch (IOException e) {
                throw new CannotRetrieveResourceException("Cannot read " + uri, getMetadata());
            }
            if (partitions == -1) {
                strings = SparkSessionManager.getInstance()
                    .getJavaSparkContext()
                    .parallelize(lines);
            } else {
                strings = SparkSessionManager.getInstance()
                    .getJavaSparkContext()
                    .parallelize(
                        lines,
                        partitions
                    );
            }
        } else {
            if (!FileSystemUtil.exists(uri, context.getRumbleRuntimeConfiguration(), getMetadata())) {
                throw new CannotRetrieveResourceException("File " + uri + " not found.", getMetadata());
            }

            String path = uri.toString();
            if (uri.getScheme().contentEquals("file")) {
                path = path.replaceAll("%20", " ");
            }

            if (partitions == -1) {
                strings = SparkSessionManager.getInstance()
                    .getJavaSparkContext()
                    .textFile(path);
            } else {
                strings = SparkSessionManager.getInstance()
                    .getJavaSparkContext()
                    .textFile(
                        path,
                        partitions
                    );
            }
        }
        return strings.mapPartitions(new JSONSyntaxToItemMapper(getMetadata()));
    }
}
