/*
 * Copyright 2021 DataCanvas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dingodb.server.coordinator.meta.service;

import io.dingodb.common.CommonId;
import io.dingodb.common.Location;
import io.dingodb.common.codec.PrimitiveCodec;
import io.dingodb.common.config.DingoConfiguration;
import io.dingodb.common.table.TableDefinition;
import io.dingodb.common.util.ByteArrayUtils.ComparableByteArray;
import io.dingodb.common.util.Optional;
import io.dingodb.meta.MetaService;
import io.dingodb.meta.Part;
import io.dingodb.net.NetService;
import io.dingodb.net.NetServiceProvider;
import io.dingodb.server.api.MetaServiceApi;
import io.dingodb.server.coordinator.meta.adaptor.impl.ExecutorAdaptor;
import io.dingodb.server.coordinator.meta.adaptor.impl.ReplicaAdaptor;
import io.dingodb.server.coordinator.meta.adaptor.impl.TableAdaptor;
import io.dingodb.server.coordinator.meta.adaptor.impl.TablePartStatsAdaptor;
import io.dingodb.server.protocol.CommonIdConstant;
import io.dingodb.server.protocol.meta.Executor;
import io.dingodb.server.protocol.meta.Replica;
import io.dingodb.server.protocol.meta.Table;
import io.dingodb.server.protocol.meta.TablePart;
import io.dingodb.server.protocol.meta.TablePartStats;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static io.dingodb.server.coordinator.meta.adaptor.MetaAdaptorRegistry.getMetaAdaptor;
import static io.dingodb.server.coordinator.meta.adaptor.MetaAdaptorRegistry.getStatsMetaAdaptor;
import static io.dingodb.server.protocol.CommonIdConstant.ID_TYPE;
import static io.dingodb.server.protocol.CommonIdConstant.STATS_IDENTIFIER;

@Slf4j
public class DingoMetaService implements MetaService, MetaServiceApi {

    private static final NetService NET_SERVICE = ServiceLoader.load(NetServiceProvider.class).iterator().next().get();
    private static final DingoMetaService INSTANCE = new DingoMetaService();

    public static final String NAME = "DINGO";
    public static final CommonId DINGO_ID = new CommonId(
        CommonIdConstant.ID_TYPE.table,
        CommonIdConstant.TABLE_IDENTIFIER.schema,
        PrimitiveCodec.encodeInt(0),
        PrimitiveCodec.encodeInt(0)
    );
    public static final Location CURRENT_LOCATION = new Location(DingoConfiguration.host(), DingoConfiguration.port());

    private Map<String, Object> props;

    private DingoMetaService() {
        NET_SERVICE.apiRegistry().register(MetaServiceApi.class, this);
    }

    public static DingoMetaService instance() {
        return INSTANCE;
    }

    public static void init() {
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(@Nullable Map<String, Object> props) {
        this.props = props;
    }

    @Override
    public void clear() {
    }

    @Override
    public synchronized void createTable(@NonNull String tableName, @NonNull TableDefinition tableDefinition) {
        if (getTableDefinitions().containsKey(tableName)) {
            throw new RuntimeException("Table " + tableName + " already exists");
        }
        ((TableAdaptor) getMetaAdaptor(Table.class)).create(DINGO_ID, tableDefinition);
    }

    @Override
    public boolean dropTable(@NonNull String tableName) {
        CommonId tableId = ((TableAdaptor) getMetaAdaptor(Table.class)).getTableId(tableName);
        if (tableId == null || !((TableAdaptor) getMetaAdaptor(Table.class)).delete(tableName)) {
            return false;
        }
        return true;
    }

    @Override
    public byte[] getTableKey(@NonNull String tableName) {
        return ((TableAdaptor) getMetaAdaptor(Table.class)).getTableId(tableName).encode();
    }

    @Override
    public CommonId getTableId(@NonNull String tableName) {
        return ((TableAdaptor) getMetaAdaptor(Table.class)).getTableId(tableName);
    }

    @Override
    public byte[] getIndexId(@NonNull String tableName) {
        return new byte[0];
    }

    @Override
    public Map<String, TableDefinition> getTableDefinitions() {
        return ((TableAdaptor) getMetaAdaptor(Table.class)).getAllDefinition();
    }

    @Override
    public NavigableMap<ComparableByteArray, Part> getParts(String name) {
        CommonId tableId = ((TableAdaptor) getMetaAdaptor(Table.class)).getTableId(name);

        TablePartStatsAdaptor statsMetaAdaptor = getStatsMetaAdaptor(TablePartStats.class);
        ExecutorAdaptor executorAdaptor = getMetaAdaptor(Executor.class);
        ReplicaAdaptor replicaAdaptor = getMetaAdaptor(Replica.class);

        NavigableMap<ComparableByteArray, Part> result = new TreeMap<>();
        getMetaAdaptor(TablePart.class).getByDomain(tableId.seqContent()).forEach(part -> {
            Optional.ofNullable(statsMetaAdaptor.getStats(
                new CommonId(ID_TYPE.stats, STATS_IDENTIFIER.part, part.getId().domainContent(),
                    part.getId().seqContent())
            )).ifPresent(stats -> {
                result.put(
                    new ComparableByteArray(part.getStart()),
                    new Part(
                        part.getId().encode(),
                        executorAdaptor.getLocation(stats.getLeader()),
                        replicaAdaptor.getLocationsByDomain(part.getId().seqContent()),
                        part.getStart(),
                        part.getEnd())
                );
            });
        });
        return result;
    }

    @Override
    public List<Location> getDistributes(String name) {
        return getParts(name).values().stream()
            .map(Part::getLeader)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public Location currentLocation() {
        return CURRENT_LOCATION;
    }

    public CommonId getTableIdByIdString(CommonId id) {
        return ((TableAdaptor) getMetaAdaptor(Table.class)).getTableIdByIdString(id);
    }

    @Override
    public TableDefinition getTableDefinition(@NonNull CommonId commonId) {
        return ((TableAdaptor) getMetaAdaptor(Table.class)).getDefinition(commonId);
    }

    @Override
    public int registerUDF(CommonId id, String udfName, String function) {
        id = getTableIdByIdString(id);
        TableAdaptor adaptor = (TableAdaptor) getMetaAdaptor(Table.class);
        synchronized (id) {
            Integer version = adaptor.updateUdfVersion(id, udfName);
            if (adaptor.updateUdfFunction(id, udfName, version, function)) {
                return version;
            }
        }
        return 0;
    }

    @Override
    public boolean unregisterUDF(CommonId id, String udfName, int version) {
        id = getTableIdByIdString(id);
        TableAdaptor adaptor = (TableAdaptor) getMetaAdaptor(Table.class);
        synchronized (id) {
            return adaptor.deleteUdfFunction(id, udfName, version);
        }
    }

    @Override
    public String getUDF(CommonId id, String udfName) {
        return getUDF(id, udfName, 0);
    }

    @Override
    public String getUDF(CommonId id, String udfName, int version) {
        id = getTableIdByIdString(id);
        TableAdaptor adaptor = (TableAdaptor) getMetaAdaptor(Table.class);
        if (version == 0) {
            version = adaptor.getUdfVersion(id, udfName);
        }
        String udf = null;
        while (udf == null && version > 0) {
            udf = adaptor.getUdfFunction(id, udfName, version);
            version--;
        }
        return udf;
    }

    private boolean isCoordinatorLeader() {
        // todo
        return true;
    }

}
