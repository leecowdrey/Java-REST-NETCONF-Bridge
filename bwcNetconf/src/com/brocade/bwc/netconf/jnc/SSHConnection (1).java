package com.brocade.bwc.netconf.jnc;

import java.io.File;
import java.io.IOException;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.KnownHosts;
import ch.ethz.ssh2.ServerHostKeyVerifier;

/**
 * A SSH NETCONF connection class. Can be used whenever {@link NetconfSession}
 * intends to use SSH for its transport.
 * <p>
 * Example:
 *
 * <pre>
 * SSHConnection ssh = new SSHConnection(&quot;127.0.0.1&quot;, 2023);
 * ssh.authenticateWithPassword(&quot;ola&quot;, &quot;secret&quot;);
 * SSHSession tr = new SSHSession(ssh);
 * NetconfSession dev1 = new NetconfSession(tr);
 * </pre>
 */
public class SSHConnection {

    Connection connection = null;
    boolean strictHostKeyCheck = false;
    final private KnownHosts knownHostsDatabase = new KnownHosts();
    private String knownHosts;
    private File knownHostsFile;
    private File knownHostsDir;
    private String knownDir;
    private String serverHostKey;
    private String serverHostKeyAlgorithm;
    private int serverHostKeyPort;
    private int serverHostKeyValidation;
    private String serverHostKeyHostName;

    /**
     * LC added
     *
     * This method establishes an SSH strictHostKeyCheck policy
     */
    private class HostVerifier implements ServerHostKeyVerifier {

        @Override
        public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey)
                throws Exception {

            int result = knownHostsDatabase.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);

            switch (result) {
                case KnownHosts.HOSTKEY_IS_OK:
                    return true;

                case KnownHosts.HOSTKEY_IS_NEW:
                    knownHostsDatabase.addHostkey(new String[]{hostname}, serverHostKeyAlgorithm, serverHostKey);
                    KnownHosts.addHostkeyToFile(knownHostsFile, new String[]{hostname}, serverHostKeyAlgorithm, serverHostKey);
                    return true;

                case KnownHosts.HOSTKEY_HAS_CHANGED:
                    System.out.println("SSH hostkey verification failed for a known host " + hostname);
                    if (strictHostKeyCheck) {
                        throw new IllegalStateException();
                    } else {
                        knownHostsDatabase.addHostkey(new String[]{hostname}, serverHostKeyAlgorithm, serverHostKey);
                        KnownHosts.addHostkeyToFile(knownHostsFile, new String[]{hostname}, serverHostKeyAlgorithm, serverHostKey);
                        return true;
                    }

                default:
                    // "SSH Host Key Verification Failed (IllegalStateException)");
                    System.err.println("SSH hostkey verification failed for " + hostname);
                    throw new IllegalStateException();
            }
        }
    }

    /**
     * By default we connect to the IANA registered port for NETCONF which is
     * 830
     *
     * @param host Host or IP address to connect to
     */
    public SSHConnection(String host) throws IOException, JNCException {
        this(host, 830, 0, false);
    }

    /**
     * This method establishes an SSH connection to a host, once the connection
     * is established it must be authenticated.
     *
     * @param host Host name.
     * @param port Port number to connect to.
     */
    public SSHConnection(String host, int port) throws IOException,
            JNCException {
        this(host, port, 0, false);
    }

    /**
     * LC added
     *
     * This method establishes an SSH connection to a host, once the connection
     * is established it must be authenticated.
     *
     * @param host Host name.
     * @param port Port number to connect to.
     * @param strictHostKeyCheck set
     */
    public SSHConnection(String host, int port, boolean strictHostKeyCheck) throws IOException,
            JNCException {
        this(host, port, 0, strictHostKeyCheck);
    }

    /**
     * LC modified added strictHostKeyCheck, HostVerifier and known_hosts
     * management
     *
     * This method establishes an SSH connection to a host, once the connection
     * is established it must be authenticated.
     *
     * @param host Host name.
     * @param port Port number to connect to.
     * @param connectTimeout
     * @param strictHostKeyCheck Validate or ignore host fingerprint
     */
    public SSHConnection(String host, int port, int connectTimeout, boolean strictHostKeyCheck)
            throws IOException, JNCException {
        this.strictHostKeyCheck = strictHostKeyCheck;
        // figure out running OS platform and make known_hosts if does not exist
        knownHosts = "known_hosts";
        knownDir = System.getProperty("user.home").toString() + System.getProperty("file.separator").toString() + ".ssh" + System.getProperty("file.separator").toString();
        knownHostsDir = new File(knownDir);
        knownHostsFile = new File(knownDir + knownHosts);
        if (!knownHostsDir.exists()) {
            if (knownHostsDir.mkdirs()) {
//                System.out.println("SSH known_hosts directory created:" + knownHostsDir.getAbsolutePath());
            }
        }
        if (!knownHostsFile.exists()) {
            if (knownHostsFile.createNewFile()) {

            }
        }
        // continue with connection 
        knownHostsDatabase.addHostkeys(knownHostsFile);
        connection = new Connection(host, port);

        try {
            connection.connect(new HostVerifier(), connectTimeout, 0);
        } catch (IOException ioee) {
            throw new JNCException(JNCException.SSH_TARGET_NOT_AVAILABLE,
                    "host: \"" + host + "\", port: \"" + Integer.toString(port) + "\"");
        } catch (IllegalStateException isee) {
            throw new JNCException(JNCException.SSH_HOSTKEY_INVALID,
                    "host: \"" + host + "\", port: \"" + Integer.toString(port) + "\"");
        }
    }

    /**
     * This method establishes an SSH connection to a host, once the connection
     * is established it must be authenticated.
     *
     * @param host Host name.
     * @param port Port number to connect to.
     * @param connectTimeout Connection timeout timer. Connect the underlying
     * TCP socket to the server with the given timeout value (non-negative, in
     * milliseconds). Zero means no timeout.
     * @param kexTimeout Key exchange timeout timer. Timeout for complete
     * connection establishment (non-negative, in milliseconds). Zero means no
     * timeout. The timeout counts until the first key-exchange round has
     * finished.
     * @throws IOException In case of a timeout (either connectTimeout or
     * kexTimeout) a SocketTimeoutException is thrown.
     * <p>
     * An exception may also be thrown if the connection was already
     * successfully connected (no matter if the connection broke in the mean
     * time) and you invoke <code>connect()</code> again without having called
     * {@link #close()} first.
     */
    public SSHConnection(String host, int port, int connectTimeout,
            int kexTimeout) throws IOException, JNCException {
        connection = new Connection(host, port);
        connection.connect(null, connectTimeout, kexTimeout);
    }

    /**
     * @return the underlying Ganymed connection object This is required if wish
     * to use the addConnectionMonitor() method in the ganymed Connection class.
     *
     */
    Connection getConnection() {
        return connection;
    }

    /**
     * This is required if wish to have access to the ganymed connection object
     * outside of this package.
     *
     * @return the underlying Ganymed connection object
     */
    public Connection getGanymedConnection() {
        return connection;
    }

    /**
     * Authenticate with regular username pass.
     *
     * @param user User name.
     * @param password Password.
     *
     *
     */
    public void authenticateWithPassword(String user, String password)
            throws IOException, JNCException {
        if (!connection.authenticateWithPassword(user, password)) {
            throw new JNCException(JNCException.AUTH_FAILED, this);
        }
    }

    /**
     * Authenticate with the name of a file containing the private key See
     * ganymed docs for full explanation, use null for password if the key
     * doesn't have a passphrase.
     *
     * @param user User name.
     * @param pemFile Fila name.
     * @param password Password.
     *
     */
    public void authenticateWithPublicKeyFile(String user, File pemFile,
            String password) throws IOException, JNCException {
        if (!connection.authenticateWithPublicKey(user, pemFile, password)) {
            throw new JNCException(JNCException.AUTH_FAILED, this);
        }
    }

    /**
     * Authenticate with a private key. See ganymed docs for full explanation,
     * use null for password if the key doesn't have a passphrase.
     *
     * @param user User name.
     * @param pemPrivateKey Private key.
     * @param pass Passphrase.
     *
     */
    public void authenticateWithPublicKey(String user, char[] pemPrivateKey,
            String pass) throws IOException, JNCException {
        if (!connection.authenticateWithPublicKey(user, pemPrivateKey, pass)) {
            throw new JNCException(JNCException.AUTH_FAILED, this);
        }
    }

    /**
     * Closes the SSH session/connection.
     */
    public void close() {
        connection.close();
    }

}
