// Developer - Arman Sahakyan
// stocksray.com
// MS3 protocol buffer structure:
// * binary only
// * starts with 2-byte integer (short) that represents the length of the payload (excluding the size bytes and the checksum)
// * followed by the payload (flag + message)
// * flag is 1-byte
// * ends with 4-byte crc32 checksum of the payload

package com.stocksray.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.ByteArrayOutputStream;

public class MS3ProtocolBuffer {
  private final List<byte[]> binaryCommands = new ArrayList<>();
  private final ByteArrayOutputStream binaryBuffer = new ByteArrayOutputStream();

  public void appendData(byte[] data) {
    if (data == null || data.length == 0) {
      return;
    }
    processBinaryData(data);
  }

  public boolean hasMoreCommands() {
    return binaryCommands.size() > 0;
  }

  public byte[] nextBinaryCommand() {
    if (binaryCommands.size() == 0) {
      return null;
    }
    byte[] command = binaryCommands.get(0);
    binaryCommands.remove(0);
    return command;
  }

  // Processes binary data and extracts messages from it.
  // * starts with 2-byte integer that represents the length of the payload
  // (excluding the size bytes and the checksum)
  // * followed by the payload (flag + message)
  // * flag is 1-byte
  // * ends with 4-byte crc32 checksum of the payload
  private void processBinaryData(byte[] data) {
    binaryBuffer.write(data, 0, data.length);

    while (binaryBuffer.size() > 8) { // Minimum size: 2 bytes (length) + 1 byte (flag) + 4 bytes (checksum) + 1 byte
                                      // (message)
      byte[] buffer = binaryBuffer.toByteArray();
      int payloadLength = ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);

      // Ensure the buffer has enough data for the payload and checksum
      final int totalPacketSize = 2 + payloadLength + 4;
      if (buffer.length < totalPacketSize) {
        break; // Wait for more data
      }

      // Validate payload length
      if (payloadLength < 2) { // Minimum payload: 1-byte flag + 1-byte message
        // Discard invalid data and move forward
        binaryBuffer.reset();
        binaryBuffer.write(buffer, 2, buffer.length - 2);
        continue;
      }

      // Extract payload and checksum
      byte[] payload = Arrays.copyOfRange(buffer, 2, 2 + payloadLength);
      byte[] checksum = Arrays.copyOfRange(buffer, 2 + payloadLength, totalPacketSize);

      // Validate checksum
      boolean crcFailure = false;
      byte[] expectedChecksum = Utils.checksum(payload);
      if (Arrays.equals(expectedChecksum, checksum)) {
        // Extract message (excluding the flag)
        byte[] message = Arrays.copyOfRange(payload, 1, payload.length);
        binaryCommands.add(message);
      } else {
        crcFailure = true;
      }

      // Remove processed data from the buffer
      binaryBuffer.reset();
      if (crcFailure) {
        // binaryBuffer.write(buffer, 2, buffer.length - 2);
      } else {
        binaryBuffer.write(buffer, totalPacketSize, buffer.length - totalPacketSize);
      }
    }
  }

  public static void main(String[] args) {
    System.out.println("Starting MS3ProtocolBuffer tests...");
    MS3ProtocolBuffer binaryBuffer = new MS3ProtocolBuffer();

    final byte[] payload = { 0x01, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60 };
    final byte[] checksum = Utils.checksum(payload);
    Utils.assertTrue(checksum.length == 4);
    final byte[] command = new byte[2 + payload.length + checksum.length];
    command[0] = (byte) ((payload.length >> 8) & 0xFF);
    command[1] = (byte) (payload.length & 0xFF);
    System.arraycopy(payload, 0, command, 2, payload.length);
    System.arraycopy(checksum, 0, command, 2 + payload.length, checksum.length);

    // Test single message
    System.out.println("Testing single message");
    binaryBuffer.appendData(command);
    Utils.assertArrayEquals(Arrays.copyOfRange(payload, 1, payload.length), binaryBuffer.nextBinaryCommand());

    // Test partial messages
    System.out.println("Testing partial messages");
    binaryBuffer.appendData(Arrays.copyOfRange(command, 0, 4));
    Utils.assertFalse(binaryBuffer.hasMoreCommands());
    binaryBuffer.appendData(Arrays.copyOfRange(command, 4, 6));
    Utils.assertFalse(binaryBuffer.hasMoreCommands());
    binaryBuffer.appendData(Arrays.copyOfRange(command, 6, command.length));
    Utils.assertTrue(binaryBuffer.hasMoreCommands());
    Utils.assertArrayEquals(Arrays.copyOfRange(payload, 1, payload.length), binaryBuffer.nextBinaryCommand());

    // Test multiple messages
    System.out.println("Testing multiple messages");
    byte[] multipleMessages = new byte[command.length * 2];
    System.arraycopy(command, 0, multipleMessages, 0, command.length);
    System.arraycopy(command, 0, multipleMessages, command.length, command.length);
    binaryBuffer.appendData(multipleMessages);
    Utils.assertTrue(binaryBuffer.hasMoreCommands());
    Utils.assertArrayEquals(Arrays.copyOfRange(payload, 1, payload.length), binaryBuffer.nextBinaryCommand());
    Utils.assertTrue(binaryBuffer.hasMoreCommands());
    Utils.assertArrayEquals(Arrays.copyOfRange(payload, 1, payload.length), binaryBuffer.nextBinaryCommand());

    // Test message with incorrect checksum
    System.out.println("Testing message with incorrect checksum");
    binaryBuffer.appendData(new byte[] { 0x00, 0x03, 0x01, 0x02, 0x03, 0x00, 0x00, 0x00, 0x01 });
    Utils.assertFalse(binaryBuffer.hasMoreCommands());

    // Test MS3 real-world data 1)
    System.out.println("Testing MS3 real-world data 1");
    String hexPayload = "004D533320312E362E302072656C6561736520202020202032303234303330312031343A3032474D5420286329204A534D2F4B43202A2A2A2A2A2A2A00";
    byte[] bytesPayload = Utils.hexToByteArray(hexPayload);
    byte[] crc = Utils.checksum(bytesPayload);
    byte[] data = new byte[2 + bytesPayload.length + crc.length];
    data[0] = (byte) ((bytesPayload.length >> 8) & 0xFF);
    data[1] = (byte) (bytesPayload.length & 0xFF);
    System.arraycopy(bytesPayload, 0, data, 2, bytesPayload.length);
    System.arraycopy(crc, 0, data, 2 + bytesPayload.length, crc.length);

    binaryBuffer.appendData(data);
    Utils.assertArrayEquals(Arrays.copyOfRange(bytesPayload, 1, bytesPayload.length), binaryBuffer.nextBinaryCommand());

    // Test MS3 real-world data 2)
    System.out.println("Testing MS3 real-world data 2");
    String hexFull = "003D004D533320312E362E302072656C6561736520202020202032303234303330312031343A3032474D5420286329204A534D2F4B43202A2A2A2A2A2A2A004250E029";
    byte[] bytesFull = Utils.hexToByteArray(hexFull);
    binaryBuffer.appendData(bytesFull);
    Utils.assertArrayEquals(Arrays.copyOfRange(bytesFull, 3, bytesFull.length - 4), binaryBuffer.nextBinaryCommand());

    // Test MS3 real-world data 3)
    System.out.println("Testing MS3 real-world data 3");
    hexFull = "0201010012000000000000000000009393010103E8000002BB0707FFAA007600670000000003E803E803E500640000006403E8006403E803E800A7000000000000003C000000000064670000640023000000000000000000000000000000000000000000000000000000000A2400000000000000000000000000000000000000000000000000000000030200000000000000000000000000000000000000000000000000000000000000000000000000000000000004830000000000000000000000000000000000000000000000000000FFAA000000000000000000000000000000000064001E00000000000000000000000000000000000000000000000067000000000000000000000000000000002300000000000000000000000000000000000000000000000000000000000003E803E803E803E803E803E803E803E803E803E803E803E8BF0300000000000000000000000000000000000000000023002C0000000000000000000000000000000000000000000000000000000000000A250000000000000000000000000000000000000000000080000000000000FF47FE001055000000000002BB01BB0000000003E70E0D000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004780000000000000000000000000000000000000000000000000000000000000000A6A6A60C";
    bytesFull = Utils.hexToByteArray(hexFull);
    binaryBuffer.appendData(bytesFull);
    Utils.assertArrayEquals(Arrays.copyOfRange(bytesFull, 3, bytesFull.length - 4), binaryBuffer.nextBinaryCommand());

    System.out.println("All tests passed!");
  }
}