---
layout: post
title: RSA Encryption with Clojure and Kotlin
introduction: An overview of RSA encryption in Java using Clojure and Kotlin
---

One of my clients had the requirements that all their data should not be readable except on special clients. So, I had to encrypt all the data on mobile clients, sent and store them on a server, and decrypt them on Java clients.

For the mobile encryption, Kotlin was used and the Java client was written in Clojure. 

( write something about encryption in general and RSA, PBE)

## Encryption on Android

First we need to define some constants:

```kotlin
val PROVIDER = "BC"
val PBE_ALGORITHM = "PBEWithSHA256And256BitAES-CBC-BC"
val PASSWORD_CIPHER_ALGORITHM = "RSA/NONE/OAEPWithSHA1AndMGF1Padding"
val RANDOM_ALGORITHM = "SHA1PRNG"
val DATA_CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"
val IV_LENGTH = 16
val SECRET_LENGTH = 128
```

Now we write the function that creates a secret key of length 128 that we want to encrypt with RSA:

```kotlin
fun generateSecretKey() : SecretKey {
    val keyGenerator = KeyGenerator.getInstance("AES", PROVIDER)
    keyGenerator.init(SECRET_LENGTH)
    return keyGenerator.generateKey();
}
```

So, what's happening here? 


Next we encrypt this key using RSA:

```kotlin
fun encryptSecret(secretKey: SecretKey, context: Context) : String {
    val stream = context.resources.openRawResource(R.raw.pub)
    val bytes = stream.readBytes()
    val keySpec = X509EncodedKeySpec(bytes)
    val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
    val cipher = Cipher.getInstance(PASSWORD_CIPHER_ALGORITHM)
    cipher.init(Cipher.ENCRYPT_MODE, publicKey)
    val cipherText = cipher.doFinal(secretKey.encoded)
    return Base64.encodeToString(cipherText, Base64.NO_WRAP)
}
```
