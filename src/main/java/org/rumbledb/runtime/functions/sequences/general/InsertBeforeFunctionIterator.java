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

package org.rumbledb.runtime.functions.sequences.general;

import org.apache.spark.api.java.JavaRDD;
import org.rumbledb.api.Item;
import org.rumbledb.exceptions.ExceptionMetadata;
import org.rumbledb.exceptions.IteratorFlowException;
import org.rumbledb.exceptions.NonAtomicKeyException;
import org.rumbledb.exceptions.UnexpectedTypeException;
import org.rumbledb.items.IntegerItem;
import org.rumbledb.runtime.HybridRuntimeIterator;
import org.rumbledb.runtime.RuntimeIterator;
import org.rumbledb.runtime.functions.base.LocalFunctionCallIterator;
import sparksoniq.jsoniq.ExecutionMode;
import sparksoniq.semantics.DynamicContext;

import java.util.List;

public class InsertBeforeFunctionIterator extends HybridRuntimeIterator {


    private static final long serialVersionUID = 1L;
    private RuntimeIterator sequenceIterator;
    private RuntimeIterator positionIterator;
    private RuntimeIterator insertIterator;
    private Item nextResult;
    private int insertPosition; // position to start inserting
    private int currentPosition; // current position
    private boolean insertingNow; // check if currently iterating over insertIterator
    private boolean insertingCompleted;

    public InsertBeforeFunctionIterator(
            List<RuntimeIterator> parameters,
            ExecutionMode executionMode,
            ExceptionMetadata iteratorMetadata
    ) {
        super(parameters, executionMode, iteratorMetadata);
        this.sequenceIterator = this.children.get(0);
        this.positionIterator = this.children.get(1);
        this.insertIterator = this.children.get(2);
    }

    @Override
    protected JavaRDD<Item> getRDDAux(DynamicContext context) {
        return null;
    }

    @Override
    protected void openLocal() {
        init(this.currentDynamicContextForLocalExecution);
        this.currentPosition = 1; // initialize index as the first item
        this.insertingNow = false;
        this.insertingCompleted = false;

        this.sequenceIterator.open(this.currentDynamicContextForLocalExecution);
        this.insertIterator.open(this.currentDynamicContextForLocalExecution);
        setNextResult();
    }

    @Override
    protected void closeLocal() {
        this.sequenceIterator.close();
        this.insertIterator.close();
    }

    @Override
    protected void resetLocal(DynamicContext context) {
        this.currentPosition = 1; // initialize index as the first item
        this.insertingNow = false;
        this.insertingCompleted = false;

        this.sequenceIterator.reset(this.currentDynamicContextForLocalExecution);
        this.insertIterator.reset(this.currentDynamicContextForLocalExecution);
        setNextResult();
    }

    @Override
    protected boolean hasNextLocal() {
        return this.hasNext;
    }

    @Override
    protected Item nextLocal() {
        if (this.hasNext()) {
            Item result = this.nextResult; // save the result to be returned
            setNextResult(); // calculate and store the next result
            return result;
        }
        throw new IteratorFlowException(FLOW_EXCEPTION_MESSAGE + "insert-before function", getMetadata());
    }

    private void init(DynamicContext context) {
        Item positionItem = this.positionIterator.materializeFirstItemOrNull(context);
        this.insertPosition = positionItem.getIntegerValue();
    }

    public void setNextResult() {
        this.nextResult = null;

        // don't check for insertion triggers once insertion is completed
        if (!this.insertingCompleted) {
            if (!this.insertingNow) {
                if (this.insertPosition <= this.currentPosition) { // start inserting if condition is met
                    if (this.insertIterator.hasNext()) {
                        this.insertingNow = true;
                        this.nextResult = this.insertIterator.next();
                    } else {
                        this.insertingNow = false;
                        this.insertingCompleted = true;
                    }
                }
            } else { // if inserting
                if (this.insertIterator.hasNext()) { // return an item from insertIterator at each iteration
                    this.nextResult = this.insertIterator.next();
                } else {
                    this.insertingNow = false;
                    this.insertingCompleted = true;
                }
            }
        }

        // if not inserting, take the next element from input sequence
        if (!this.insertingNow) {
            if (this.sequenceIterator.hasNext()) {
                this.nextResult = this.sequenceIterator.next();
                this.currentPosition++;
            } else if (this.insertIterator.hasNext()) {
                this.nextResult = this.insertIterator.next();
            }
        }

        if (this.nextResult == null) {
            this.hasNext = false;
            this.sequenceIterator.close();
            this.insertIterator.close();
        } else {
            this.hasNext = true;
        }
    }
}
