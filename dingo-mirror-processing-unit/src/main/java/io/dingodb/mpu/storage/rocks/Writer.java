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

package io.dingodb.mpu.storage.rocks;

import io.dingodb.mpu.instruction.Instruction;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Accessors(chain = true, fluent = true)
public class Writer implements io.dingodb.mpu.storage.Writer {

    @Getter
    private final Instruction instruction;

    private final RocksDB db;
    private final ColumnFamilyHandle handler;
    private final WriteBatch writeBatch;

    public Writer(RocksDB db, Instruction instruction, ColumnFamilyHandle dcfHandler) {
        this.db = db;
        this.instruction = instruction;
        this.handler = dcfHandler;
        this.writeBatch = new WriteBatch();
    }

    public WriteBatch writeBatch() {
        return writeBatch;
    }

    //@Override
    //public Instruction instruction() {
    //    //return new Instruction(instruction.clock, (byte) 0, (short) 3, ProtostuffCodec.write(instructionMap), null);
    //    return null;
    //}

    public void close() {
        writeBatch.close();
    }

    @Override
    public int count() {
        return writeBatch.count();
    }

    @Override
    public void set(byte[] key, byte[] value) {
        try {
            writeBatch.put(handler, key, value);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void erase(byte[] key) {
        try {
            writeBatch.delete(handler, key);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void erase(byte[] begin, byte[] end) {
        try {
            if (end == null) {
                try (RocksIterator iter = db.newIterator()) {
                    iter.seekToLast();
                    if (iter.isValid()) {
                        end = iter.key();
                        writeBatch.delete(handler, end);
                    } else {
                        return;
                    }
                }
            }
            writeBatch.deleteRange(handler, begin, end);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

}
