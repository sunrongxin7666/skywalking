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
 */

package org.apache.skywalking.oap.server.core.query.type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.analysis.metrics.DataTable;

/**
 * HeatMap represents the value distribution in the defined buckets.
 *
 * @since 8.0.0
 */
@Getter
public class HeatMap {
    private List<HeatMapColumn> values = new ArrayList<>(10);
    private List<Bucket> buckets = null;

    public void addBucket(Bucket bucket) {
        this.buckets.add(bucket);
    }

    /**
     * Build one heatmap value column based on rawdata in the storage and row id.
     *
     * @param id      of the row
     * @param rawdata literal string, represent a {@link DataTable}
     */
    public void buildColumn(String id, String rawdata, int defaultValue) {
        DataTable dataset = new DataTable(rawdata);

        final List<String> sortedKeys = dataset.sortedKeys(
            Comparator.comparingInt(Integer::parseInt));
        if (buckets == null) {
            buckets = new ArrayList<>(dataset.size());
            for (int i = 0; i < sortedKeys.size(); i++) {
                if (i == sortedKeys.size() - 1) {
                    // last element
                    this.addBucket(
                        new Bucket().setMin(Integer.parseInt(sortedKeys.get(i))).infiniteMax()
                    );
                } else {
                    this.addBucket(new Bucket(
                        Integer.parseInt(sortedKeys.get(i)),
                        Integer.parseInt(sortedKeys.get(i + 1))
                    ));
                }
            }
        }

        HeatMap.HeatMapColumn column = new HeatMap.HeatMapColumn();
        column.setId(id);
        sortedKeys.forEach(key -> {
            if (dataset.hasKey(key)) {
                column.addValue(dataset.get(key));
            } else {
                column.addValue((long) defaultValue);
            }
        });
        values.add(column);
    }

    public void fixMissingColumns(List<String> ids, int defaultValue) {
        for (int i = 0; i < ids.size(); i++) {
            final String expectedId = ids.get(i);
            boolean found = false;
            for (final HeatMapColumn value : values) {
                if (expectedId.equals(value.id)) {
                    found = true;
                }
            }
            if (!found) {
                final HeatMapColumn emptyColumn = buildMissingColumn(expectedId, defaultValue);
                values.add(i, emptyColumn);
            }
        }
    }

    private HeatMapColumn buildMissingColumn(String id, int defaultValue) {
        HeatMapColumn column = new HeatMapColumn();
        column.setId(id);
        buckets.forEach(bucket -> {
            column.addValue((long) defaultValue);
        });
        return column;
    }

    @Getter
    public static class HeatMapColumn {
        @Setter
        private String id;
        private List<Long> values = new ArrayList<>();

        public void addValue(Long value) {
            values.add(value);
        }
    }
}
