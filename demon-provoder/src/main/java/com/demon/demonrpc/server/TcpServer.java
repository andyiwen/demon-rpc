package com.demon.demonrpc.server;

import com.demon.demonrpc.constant.constants;
import com.demon.demonrpc.factory.ZookeepterFactory;
import com.demon.demonrpc.handler.TcpServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;


/**
 * Created by wangpengpeng on 2018/12/10.
 */
public class TcpServer {
    public static void main(String[] args) throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                           // ch.pipeline().addLast(new DelimiterBasedFrameDecoder(65535, Delimiters.lineDelimiter()[0]));
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new IdleStateHandler(60,45,20, TimeUnit.SECONDS));
                            ch.pipeline().addLast(new TcpServerHandler());
                            ch.pipeline().addLast(new StringEncoder());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, false); // (6)

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(constants.SERVER_PORT).sync(); // (7)

            // Wait until the com.demon.demonrpc.server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your com.demon.demonrpc.server.
            System.out.println("正在启动Zookeepter");
            CuratorFramework client = ZookeepterFactory.create();
            String address = InetAddress.getLocalHost().getHostAddress();
            client.create().withMode(CreateMode.EPHEMERAL).forPath("/"+address);
            System.out.println("服务器启动成功");
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("服务器关闭成功");
        }

    }
}
