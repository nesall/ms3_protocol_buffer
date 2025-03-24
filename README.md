``MS3ProtocolBuffer`` helps read data from MegaSquirt3 ECUs.
Tested on MS3Pro Evo (which is basically MS3). Should work on other versions too (but not tested.)

Example usage:

```java
  private MyAsyncReader serialCallback = new MyAsyncReader() {
    @Override
    public void onReceiveData(byte[] data) {
      if (data == null)
        return;
      try {
        if (data != null) {
          protocolBuffer.appendData(data);
          while (protocolBuffer.hasMoreCommands()) {
            byte[] message = protocolBuffer.nextBinaryCommand(); // Extract full message
            // TODO: work with message
            // message is a complete message. It is not truncated nor does it have any extra bytes.
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  };
```

``MyAsyncReader`` can be any serial reader, e.g. felHR85/UsbSerial for Android.
