package mt4.license.com.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import mt4.license.com.entity.AccessInfo;
import mt4.license.com.entity.License;
import mt4.license.com.util.Utils;

@Configuration
public class SslServer implements InitializingBean, DisposableBean, Runnable {

    @Autowired
    private RedisService redisService;

    class Accepter implements Runnable {
        private SSLSocket mSocket = null;

        public Accepter(SSLSocket socket) {
            this.mSocket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream dataInputStream = new DataInputStream(mSocket.getInputStream());
                DataOutputStream dataOutputStream = new DataOutputStream(mSocket.getOutputStream());

                // read key
                int len = dataInputStream.readInt();
                byte[] keyInBytes = new byte[len];
                dataInputStream.read(keyInBytes);
                System.out.println(Utils.byteArrayToHexString(keyInBytes));

                // Get license object
                String key = new String(keyInBytes);
                System.out.println("key:" + key);
                License license = new License(key);
                if (!redisService.getLicense(license)) {
                    dataOutputStream.writeInt(1);
                    dataOutputStream.writeByte(0);
                    return;
                }

                if (!license.isEnable()) {
                    dataOutputStream.writeInt(1);
                    dataOutputStream.writeByte(0);
                    return;
                }

                if (license.getExpirationDate().before(new Date())) {
                    dataOutputStream.writeInt(1);
                    dataOutputStream.writeByte(0);
                    return;
                }

                int serialNumber = dataInputStream.readInt();
                List<AccessInfo> list = redisService.range(license, 0, 5);
                if (!isSerialNumberValid(serialNumber, list)) {
                    dataOutputStream.writeInt(1);
                    dataOutputStream.writeByte(0);
                    return;
                }

                AccessInfo accessInfo = new AccessInfo(mSocket.getInetAddress().getHostAddress(), serialNumber);

                // store access history
                if (redisService.push(license, accessInfo)) {
                    redisService.trimList(license, 20);
                }

                // read encrypted key
                len = dataInputStream.readInt();
                byte[] encryptedKey = new byte[len];
                dataInputStream.read(encryptedKey);
                System.out.println(Utils.byteArrayToHexString(encryptedKey));

                // calculate encrypted text based on key and port.
                int port = mSocket.getPort();
                byte[] hash = Pbdf2(new String(keyInBytes).toCharArray(), keyInBytes, port);
                System.out.println(Utils.byteArrayToHexString(hash));

                if (!Arrays.equals(hash, encryptedKey)) {
                    dataOutputStream.writeInt(1);
                    dataOutputStream.writeByte(0);
                    return;
                }

                // write new encrypted text to client
                byte[] output = Pbdf2(new String(keyInBytes).toCharArray(), keyInBytes, (port + 9) / 3);
                System.out.println(Utils.byteArrayToHexString(output));
                dataOutputStream.writeInt(output.length);
                dataOutputStream.write(output);
            } catch (Exception e) {
            } finally {
                try {
                    mSocket.close();
                } catch (Exception e) {
                }
            }
        }

        private byte[] Pbdf2(char[] key, byte[] salt, int times) throws Exception {
            PBEKeySpec spec = new PBEKeySpec(key, salt, times, 128);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return hash;
        }

        private boolean isSerialNumberValid(int serialNumber, List<AccessInfo> list) {
            if (list.size() == 0) {
                return true;
            }
            if (serialNumber != 0) {
                return serialNumber == list.get(0).getSerialNumber() + 1;
            }
            for (AccessInfo ai : list) {
                if (ai.getSerialNumber() != 0) {
                    return true;
                }
            }
            return false;
        }
    }

    @Value("${server.ssl.key-alias}")
    String keyAlias;

    @Value("${server.ssl.key-store-password}")
    String keyStorePassword;

    @Value("${server.ssl.key-store}")
    String keyStorePath;

    @Value("${server.ssl.key-store-type}")
    String keyStoreType;

    private boolean m_stop;

    @Value("${server.ssl.socket.port}")
    String sslSocketPort;

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this).start();
    }

    @Override
    public void destroy() {
        m_stop = false;
    }

    @Override
    public void run() {
        m_stop = false;
        // System.setProperty("javax.net.debug", "ssl,handshake");

        System.setProperty("javax.net.ssl.keyStore", keyStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", keyStoreType);
        System.setProperty("javax.net.ssl.keyStoreAlias", keyAlias);

        SSLServerSocketFactory serverSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket serverSocket;
        try {
            serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(Integer.parseInt(sslSocketPort));
            serverSocket.setNeedClientAuth(false);

            while (!m_stop) {
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                new Thread(new Accepter(socket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
