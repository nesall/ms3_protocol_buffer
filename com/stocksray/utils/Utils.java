package com.stocksray.utils;

import java.util.Arrays;
import java.util.zip.CRC32;
import java.nio.ByteBuffer;

public class Utils {
  public static void assertArrayEquals(byte[] a, byte[] b) {
    if (!Arrays.equals(a, b)) {
      throw new AssertionError("Expected " + Arrays.toString(a) + " but got " + Arrays.toString(b));
    }
  }

  public static void assertTrue(boolean condition) {
    if (!condition) {
      throw new AssertionError("Expected true but got false");
    }
  }

  public static void assertFalse(boolean condition) {
    if (condition) {
      throw new AssertionError("Expected false but got true");
    }
  }

  public static byte[] hexToByteArray(String hexString) {
    int len = hexString.length();
    byte[] byteArray = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
          + Character.digit(hexString.charAt(i + 1), 16));
    }
    return byteArray;
  }

  // Returns a 4-byte checksum of the data
  public static byte[] checksum(byte[] data) {
    CRC32 crc32 = new CRC32();
    crc32.update(data);
    long checksum = crc32.getValue();
    return ByteBuffer.allocate(4).putInt((int) checksum).array();
  }

  // Creates an MS3 commmand.
  // Returns bytes for a full command: 2-bytes for size, 1-byte command, and
  // 4-byte checksum
  public static byte[] createCommand(char cmd) {
    byte[] data = new byte[] { (byte) cmd };
    byte[] checksum = checksum(data);
    byte[] commandBytes = new byte[2 + data.length + checksum.length];
    commandBytes[0] = (byte) (data.length >> 8);
    commandBytes[1] = (byte) data.length;
    System.arraycopy(data, 0, commandBytes, 2, data.length);
    System.arraycopy(checksum, 0, commandBytes, 2 + data.length, checksum.length);
    return commandBytes;
  }
}
