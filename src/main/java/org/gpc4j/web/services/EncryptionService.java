package org.gpc4j.web.services;

import org.jasypt.util.text.AES256TextEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

  private final AES256TextEncryptor encryptor;

  public EncryptionService(@Value("${JASYPT_ENCRYPTOR_PASSWORD}") String secret) {
    this.encryptor = new AES256TextEncryptor();
    this.encryptor.setPassword(secret);
  }

  public String encrypt(String plainText) {
    return encryptor.encrypt(plainText);
  }

  public String decrypt(String encryptedText) {
    return encryptor.decrypt(encryptedText);
  }

}
