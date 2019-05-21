package mt4.license.com.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.time.Duration;
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

@Configuration
public class SslServer implements InitializingBean, DisposableBean, Runnable {

    @Autowired
    private RedisService redisService;

    class Accepter implements Runnable {
        private SSLSocket mSocket = null;

        public Accepter(SSLSocket socket) {
            this.mSocket = socket;
        }

        public String byteArrayToHexString(byte[] bytes) {
            final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
            char[] hexChars = new char[bytes.length * 2];
            int v;
            for (int j = 0; j < bytes.length; j++) {
                v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        }

        private byte[] Pbdf2(char[] key, byte[] salt, int times) throws Exception {
            PBEKeySpec spec = new PBEKeySpec(key, salt, times, 128);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return hash;
        }

        private boolean checkAccess(List<AccessInfo> list, AccessInfo accessInfo) {
            if (list.size() < 2) {
                return true;
            }

            if (accessInfo.getIp().equals(list.get(0).getIp()) && accessInfo.getIp().equals(list.get(1).getIp())) {
                return true;
            }

            if (list.size() > 10) {
                AccessInfo ai = list.get(list.size() - 1);
                if (accessInfo.getTimestamp().getTime() - ai.getTimestamp().getTime() < Duration.ofDays(list.size() - 1)
                        .toMillis()) {
                    return false;
                }
            }

            return true;
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
                System.out.println(byteArrayToHexString(keyInBytes));

                // Get license object
                String key = new String(keyInBytes);
                System.out.println("key:" + key);
                License license = new License(key);
                if (!redisService.getLicense(license)) {
                    dataOutputStream.writeInt(1);
                    dataOutputStream.writeByte(0);
                    return;
                }

                if (license.getExpirationDate().before(new Date())) {
                    dataOutputStream.writeInt(1);
                    dataOutputStream.writeByte(0);
                    return;
                }

                AccessInfo accessInfo = new AccessInfo(mSocket.getInetAddress().getHostAddress());
                List<AccessInfo> list = redisService.range(license, 0, 30);
                if (!checkAccess(list, accessInfo)) {
                    dataOutputStream.writeInt(1);
                    dataOutputStream.writeByte(0);
                    return;
                }

                // store access history
                if (!redisService.push(license, accessInfo)) {
                    // do nothing
                }

                // read encrypted key
                len = dataInputStream.readInt();
                byte[] encryptedKey = new byte[len];
                dataInputStream.read(encryptedKey);
                System.out.println(byteArrayToHexString(encryptedKey));

                // calculate encrypted text based on key and port.
                int port = mSocket.getPort();
                byte[] hash = Pbdf2(new String(keyInBytes).toCharArray(), keyInBytes, port);
                System.out.println(byteArrayToHexString(hash));

                if (!Arrays.equals(hash, encryptedKey)) {
                    dataOutputStream.writeInt(1);
                    dataOutputStream.writeByte(0);
                    return;
                }

                // write new encrypted text to client
                byte[] output = Pbdf2(new String(keyInBytes).toCharArray(), keyInBytes, (port + 9) / 3);
                System.out.println(byteArrayToHexString(output));
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
