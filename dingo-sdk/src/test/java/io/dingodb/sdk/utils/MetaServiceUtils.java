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

package io.dingodb.sdk.utils;

import io.dingodb.common.table.ColumnDefinition;
import io.dingodb.common.table.TableDefinition;
import io.dingodb.net.api.ApiRegistry;
import io.dingodb.sdk.client.DingoClient;
import io.dingodb.sdk.client.DingoConnection;
import io.dingodb.sdk.client.MetaClient;
import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetaServiceUtils {

    public static TableDefinition getSimpleTableDefinition(String tableName) {

        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(ColumnDefinition.getInstance(
            "id",
            "integer",
            null,
            null,
            null,
            true,
            true,
            null)
        );
        columns.add(ColumnDefinition.getInstance(
            "name",
            "varchar",
            null,
            200,
            0,
            true,
            true,
            null)
        );
        columns.add(ColumnDefinition.getInstance(
            "salary",
            "double",
            null,
            null,
            3,
            true,
            true,
            null)
        );
        TableDefinition tableDefinition = new TableDefinition(tableName);
        tableDefinition.setColumns(columns);
        return tableDefinition;
    }

    public static TableDefinition getComplexTableDefinition(String tableName) {
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(ColumnDefinition.getInstance(
            "ssn",
            "varchar",
            null,
            200,
            null,
            true,
            true,
            null)
        );

        columns.add(ColumnDefinition.getInstance(
            "values",
            "varchar",
            null,
            200,
            null,
            true,
            true,
            null)
        );

        columns.add(ColumnDefinition.getInstance(
            "addresses",
            "varchar",
            null,
            200,
            null,
            true,
            true,
            null)
        );

        columns.add(ColumnDefinition.getInstance(
            "home",
            "varchar",
            null,
            200,
            null,
            true,
            true,
            null)
        );

        columns.add(ColumnDefinition.getInstance(
            "work",
            "varchar",
            null,
            200,
            null,
            true,
            true,
            null)
        );

        columns.add(ColumnDefinition.getInstance(
            "integerValues",
            "varchar",
            null,
            200,
            null,
            true,
            true,
            null)
        );

        TableDefinition tableDefinition = new TableDefinition(tableName);
        tableDefinition.setColumns(columns);
        return tableDefinition;
    }


    public static void initConnectionInMockMode(DingoClient dingoClient,
                                                MetaClient metaClient,
                                                ApiRegistry apiRegistry) {
        DingoConnection connection = mock(DingoConnection.class);
        when(connection.getMetaClient()).thenReturn(metaClient);
        when(connection.getApiRegistry()).thenReturn(apiRegistry);

        try {
            Field metaClientField = DingoConnection.class.getDeclaredField("metaClient");
            metaClientField.setAccessible(true);
            metaClientField.set(connection, metaClient);

            Field apiRegistryField = DingoConnection.class.getDeclaredField("apiRegistry");
            apiRegistryField.setAccessible(true);
            apiRegistryField.set(connection, apiRegistry);

            Field connectionField = DingoClient.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            connectionField.set(dingoClient, connection);
        } catch (NoSuchFieldException e) {
            Assertions.fail("DingoConnection.metaClient field not found");
        } catch (SecurityException e) {
            Assertions.fail("DingoConnection.metaClient field not accessible");
        } catch (IllegalAccessException e) {
            Assertions.fail("Invalid Runtime Exception");
        }
    }
}
