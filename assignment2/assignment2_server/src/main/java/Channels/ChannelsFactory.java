package Channels;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;


public class ChannelsFactory implements PooledObjectFactory<Channel> {
  private Connection connection;
  private final static int PORT = 5672;
  // remember to update username/password to admin/admin when deploy to ec2
  private final static String USER_NAME = "guest";
  private final static String USER_PASSWORD = "guest";

  public ChannelsFactory(String ip) {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      if (ip != null) {
        factory.setHost(ip);
        factory.setPort(PORT);
        factory.setUsername(USER_NAME);
        factory.setPassword(USER_PASSWORD);
      }
      connection = factory.newConnection();
    } catch (IOException | TimeoutException e) {
      e.printStackTrace();
    }
  }

  @Override
  public PooledObject<Channel> makeObject() throws Exception {
    Channel channel = connection.createChannel();
    return new DefaultPooledObject<Channel>(channel);
  }

  @Override
  public void destroyObject(PooledObject<Channel> pooledObject) throws Exception {
    Channel channel = pooledObject.getObject();
    if (channel.isOpen()) {
      try {
        channel.close();
      } catch (Exception e) {
      }
    }
  }


  @Override
  public boolean validateObject(PooledObject<Channel> pooledObject) {
    Channel channel = pooledObject.getObject();
    return channel.isOpen();
  }

  @Override
  public void activateObject(PooledObject<Channel> pooledObject) throws Exception {

  }

  @Override
  public void passivateObject(PooledObject<Channel> pooledObject) throws Exception {

  }
}