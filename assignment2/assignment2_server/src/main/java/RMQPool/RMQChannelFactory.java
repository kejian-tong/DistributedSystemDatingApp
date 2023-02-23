package RMQPool;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.io.IOException;


public class RMQChannelFactory extends BasePooledObjectFactory<Channel> {

  // Valid RMQ connection
  private final Connection connection;
  // used to count created channels for debugging
  private int count;

  public RMQChannelFactory(Connection connection) {
    this.connection = connection;
    count = 0;
  }

  @Override
  synchronized public Channel create() throws IOException {
    count ++;
    Channel channel = connection.createChannel();
    // Uncomment the line below to validate the expected number of channels are being created
    // System.out.println("Channel created: " + count);
    return channel;

  }

  @Override
  public PooledObject<Channel> wrap(Channel channel) {
    //System.out.println("Wrapping channel");
    return new DefaultPooledObject<>(channel);
  }

  public int getChannelCount() {
    return count;
  }

  // for all other methods, the no-op implementation
  // in BasePooledObjectFactory will suffice
}
