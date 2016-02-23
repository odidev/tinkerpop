/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.process.traversal.step.map;

import org.apache.tinkerpop.gremlin.process.traversal.NumberHelper;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.FinalGet;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.ReducingBarrierStep;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.TraverserRequirement;
import org.apache.tinkerpop.gremlin.util.function.MeanNumberSupplier;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

import static org.apache.tinkerpop.gremlin.process.traversal.NumberHelper.div;
import static org.apache.tinkerpop.gremlin.process.traversal.NumberHelper.mul;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Daniel Kuppitz (http://gremlin.guru)
 */
public final class MeanGlobalStep<S extends Number, E extends Number> extends ReducingBarrierStep<S, E> {

    private static final Set<TraverserRequirement> REQUIREMENTS = EnumSet.of(TraverserRequirement.OBJECT, TraverserRequirement.BULK);

    public MeanGlobalStep(final Traversal.Admin traversal) {
        super(traversal);
        this.setSeedSupplier((Supplier) MeanNumberSupplier.instance());
        this.setReducingBiOperator(new MeanGlobalBiOperator<>());
    }

    @Override
    public E projectTraverser(final Traverser.Admin<S> traverser) {
        return (E) new MeanNumber(traverser.get(), traverser.bulk());
    }

    @Override
    public Set<TraverserRequirement> getRequirements() {
        return REQUIREMENTS;
    }

    /////

    public static final class MeanGlobalBiOperator<S extends Number> implements BinaryOperator<S>, Serializable {

        @Override
        public S apply(final S mutatingSeed, final S number) {
            if (mutatingSeed instanceof MeanNumber) {
                return (number instanceof MeanNumber) ?
                        (S) ((MeanNumber) mutatingSeed).add((MeanNumber) number) :
                        (S) ((MeanNumber) mutatingSeed).add(number, 1l);
            } else {
                return (number instanceof MeanNumber) ?
                        (S) ((MeanNumber) number).add(mutatingSeed, 1l) :
                        (S) new MeanNumber(number, 1l).add(mutatingSeed, 1l);
            }
        }
    }

    public static final class MeanNumber extends Number implements Comparable<Number>, FinalGet<Number> {

        private long count;
        private Number sum;

        public MeanNumber() {
            this(0, 0);
        }

        public MeanNumber(final Number number, final long count) {
            this.count = count;
            this.sum = mul(number, count);
        }

        public MeanNumber add(final Number amount, final long count) {
            this.count += count;
            this.sum = NumberHelper.add(sum, mul(amount, count));
            return this;
        }

        public MeanNumber add(final MeanNumber other) {
            this.count += other.count;
            this.sum = NumberHelper.add(sum, other.sum);
            return this;
        }

        @Override
        public int intValue() {
            return div(this.sum, this.count).intValue();
        }

        @Override
        public long longValue() {
            return div(this.sum, this.count).longValue();
        }

        @Override
        public float floatValue() {
            return div(this.sum, this.count, true).floatValue();
        }

        @Override
        public double doubleValue() {
            return div(this.sum, this.count, true).doubleValue();
        }

        @Override
        public String toString() {
            return getFinal().toString();
        }

        @Override
        public int compareTo(final Number number) {
            // TODO: NumberHelper should provide a compareTo() implementation
            return Double.valueOf(this.doubleValue()).compareTo(number.doubleValue());
        }

        @Override
        public boolean equals(final Object object) {
            return object instanceof Number && Double.valueOf(this.doubleValue()).equals(((Number) object).doubleValue());
        }

        @Override
        public int hashCode() {
            return Double.valueOf(this.doubleValue()).hashCode();
        }

        @Override
        public Number getFinal() {
            return div(this.sum, this.count, true);
        }
    }
}