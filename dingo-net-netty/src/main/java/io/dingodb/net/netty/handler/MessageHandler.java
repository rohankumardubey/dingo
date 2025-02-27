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

package io.dingodb.net.netty.handler;

import io.dingodb.net.netty.connection.Connection;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.List;

@Slf4j
public class MessageHandler extends ByteToMessageDecoder {

    private final Connection connection;

    public MessageHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!ctx.channel().isOpen()) {
            if (in.readableBytes() > 0) {
                log.info("Channel is closed, discarding remaining {} byte(s) in buffer.", in.readableBytes());
            }
            in.skipBytes(in.readableBytes());
            return;
        }
        // if read throw exception, must close connection
        ByteBuffer message = read(in);
        try {
            connection.receive(message);
        } catch (Exception e) {
            log.error("Handler message from [{}] error.", connection.remoteLocation());
        }
    }

    private static ByteBuffer read(ByteBuf buf) {
        if (buf.readableBytes() < 5) {
            return null;
        }
        buf.markReaderIndex();
        int length = buf.readInt();
        if (length < 0) {
            throw new CorruptedFrameException("Negative length: " + length);
        }
        if (length == 0) {
            throw new CorruptedFrameException("Received a message of length 0.");
        }
        if (!buf.isReadable(length)) {
            buf.resetReaderIndex();
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buf.readBytes(buffer);
        return (ByteBuffer) buffer.flip();
    }

}
