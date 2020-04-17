/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.metrics;

/**
 * A MetricsContext is like a namespace for metrics. Each component/submodule
 * will have its own MetricsContext.
 * <p>
 * In some cases it is possible to have a separate MetricsContext for each
 * instance of a component, for instance on the server side a possible usecase
 * it to gather metrics for every other peer.
 * </p>
 * <p>
 * Contexts are organized in a hierarchy. 上下文按层次结构组织
 * </p>
 * MetricsContext就像度量标准的名称空间。 每个组件/子模块将具有其自己的MetricsContext。
 *  在某些情况下，有可能为组件的每个实例提供单独的MetricsContext
 *  例如，在服务器端可能有一个用例，它可以为每个其他对等方收集指标。
 */
public interface MetricsContext {

    /**
     * Returns a sub context.
     *
     * @param name the name of the subcontext
     *
     * @return a new metrics context.
     */
    MetricsContext getContext(String name);

    /**
     * Returns a counter.
     *
     * @param name
     * @return the counter identified by name in this context.
     */
    Counter getCounter(String name);

    /**
     * Registers an user provided {@link Gauge} which will be called by the
     * MetricsProvider in order to sample an integer value.
     * If another Gauge was already registered the new one will
     * take its place.
     * Registering a null callback is not allowed.
     *
     * @param name unique name of the Gauge in this context
     * @param gauge the implementation of the Gauge
     *
     */
    void registerGauge(String name, Gauge gauge);

    /**
     * Unregisters the user provided {@link Gauge} bound to the given name.
     *
     * @param name unique name of the Gauge in this context
     *
     */
    void unregisterGauge(String name);

    enum DetailLevel {
        /**
         * The returned Summary is expected to track only simple aggregated
         * values, like min/max/avg
         */
        BASIC,
        /**
         * It is expected that the returned Summary performs expensive
         * aggregations, like percentiles.
         */
        ADVANCED
    }

    /**
     * Returns a summary.
     *
     * @param name
     * @param detailLevel
     * @return the summary identified by name in this context.
     */
    Summary getSummary(String name, DetailLevel detailLevel);

    /**
     * Returns a set of summaries.
     *
     * @param name
     * @param detailLevel
     * @return the summary identified by name in this context.
     */
    SummarySet getSummarySet(String name, DetailLevel detailLevel);

}
