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

package org.rumbledb.expressions.module;


import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.expressions.Expression;
import org.rumbledb.expressions.Node;
import org.rumbledb.expressions.primary.FunctionDeclaration;
import sparksoniq.semantics.visitor.AbstractNodeVisitor;

import java.util.ArrayList;
import java.util.List;

public class Prolog extends Expression {

    private final List<FunctionDeclaration> functionDeclaration;

    public Prolog(List<FunctionDeclaration> functionDeclarations, ExceptionMetadata metadata) {
        super(metadata);
        this.functionDeclaration = functionDeclarations;
    }

    public List<FunctionDeclaration> getFunctionDeclaration() {
        return this.functionDeclaration;
    }

    @Override
    public List<Node> getChildren() {
        List<Node> result = new ArrayList<>();
        if (this.functionDeclaration != null)
            this.functionDeclaration.forEach(e -> {
                if (e != null)
                    result.add(e);
            });
        return result;
    }

    @Override
    public <T> T accept(AbstractNodeVisitor<T> visitor, T argument) {
        return visitor.visitProlog(this, argument);
    }

    @Override
    public String serializationString(boolean prefix) {
        String result = "(prolog ";
        result += " (functionDecl ";
        for (FunctionDeclaration func : this.functionDeclaration) {
            result += "(" + func.serializationString(false) + ") , ";
        }
        result = result.substring(0, result.length() - 1); // remove last comma
        result += "))";
        return result;
    }
}
