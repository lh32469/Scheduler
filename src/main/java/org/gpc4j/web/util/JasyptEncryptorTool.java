package org.gpc4j.web.util;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

/**
 * Small CLI helper to encrypt property values for application.yml using Jasypt.
 *
 * Usage:
 *   1) Set environment variable JASYPT_ENCRYPTOR_PASSWORD to your master secret
 *   2) Run: mvn -q -Dexec.mainClass=org.gpc4j.web.util.JasyptEncryptorTool \
 *               -Dexec.args="your-plain-secret" exec:java
 *      or run this class from your IDE with the arg being the plaintext
 *   3) Copy the printed ENC(...) into application.yml
 */
public class JasyptEncryptorTool {

  public static void main(String[] args) {
//    if (args == null || args.length == 0 || args[0] == null || args[0].isBlank()) {
//      System.err.println("Usage: JasyptEncryptorTool <PLAINTEXT_TO_ENCRYPT>");
//      System.exit(2);
//    }
//
    String master = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
    if (master == null || master.isBlank()) {
      // Allow passing as system property for convenience
      master = System.getProperty("JASYPT_ENCRYPTOR_PASSWORD");
    }
    if (master == null || master.isBlank()) {
      System.err.println("Missing JASYPT_ENCRYPTOR_PASSWORD (env or -D). Aborting.");
      System.exit(3);
    }

    System.out.println("JASYPT_ENCRYPTOR_PASSWORD:" + master);
    String plaintext = "plain-secret";

    PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
    encryptor.setPoolSize(1);
    encryptor.setPassword(master);
    // Match application.yml configuration
    encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
    encryptor.setIvGenerator(new RandomIvGenerator());

    String ciphertext = encryptor.encrypt(plaintext);
    System.out.println("ENC(" + ciphertext + ")");
  }
}
